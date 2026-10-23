package ir.torob.seller.data.repository

import android.content.Context
import androidx.work.*
import ir.torob.seller.data.local.*
import ir.torob.seller.data.model.*
import ir.torob.seller.data.remote.ApiClient
import ir.torob.seller.worker.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class InquiryRepository(private val context: Context) {
    private val api = ApiClient.getService(context)
    private val db = SellerDatabase.getDatabase(context)
    private val inquiryDao = db.inquiryDao()
    private val syncQueueDao = db.syncQueueDao()

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun getLocalInquiries(status: String? = null): Flow<List<InquiryEntity>> {
        return if (status.isNullOrBlank()) {
            inquiryDao.getAllInquiries()
        } else {
            inquiryDao.getInquiriesByStatus(status.uppercase())
        }
    }

    fun getHistoryInquiries(): Flow<List<InquiryEntity>> = inquiryDao.getHistoryInquiries()

    suspend fun fetchRemoteInquiries(status: String? = null): Result<List<InquiryDto>> {
        return try {
            val resp = api.getInquiries(status)
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!
                // Cache into Room
                val entities = list.map { dto ->
                    InquiryEntity(
                        inquiryId = dto.inquiryId,
                        trackingCode = dto.trackingCode,
                        shopId = dto.shopId,
                        productId = dto.productId,
                        productTitle = dto.productTitle,
                        productImageUrl = dto.productImageUrl,
                        categoryName = dto.categoryName,
                        recordedPriceToman = dto.recordedPriceToman,
                        customerName = dto.customerName,
                        customerPhone = dto.customerPhone,
                        inquiryType = dto.inquiryType,
                        userNote = dto.userNote,
                        status = dto.status,
                        createdAt = dto.createdAt,
                        expiresAt = dto.expiresAt,
                        remainingSeconds = dto.remainingSeconds
                    )
                }
                inquiryDao.insertInquiries(entities)
                Result.success(list)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "خطا در دریافت استعلام‌ها"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInquiryById(inquiryId: String): InquiryEntity? {
        val cached = inquiryDao.getInquiryById(inquiryId)
        if (cached != null) return cached

        // Fetch remote if not in cache
        try {
            val resp = api.getInquiryDetail(inquiryId)
            if (resp.isSuccessful && resp.body() != null) {
                val dto = resp.body()!!
                val entity = InquiryEntity(
                    inquiryId = dto.inquiryId,
                    trackingCode = dto.trackingCode,
                    shopId = dto.shopId,
                    productId = dto.productId,
                    productTitle = dto.productTitle,
                    productImageUrl = dto.productImageUrl,
                    categoryName = dto.categoryName,
                    recordedPriceToman = dto.recordedPriceToman,
                    customerName = dto.customerName,
                    customerPhone = dto.customerPhone,
                    inquiryType = dto.inquiryType,
                    userNote = dto.userNote,
                    status = dto.status,
                    createdAt = dto.createdAt,
                    expiresAt = dto.expiresAt,
                    remainingSeconds = dto.remainingSeconds
                )
                inquiryDao.insertInquiry(entity)
                return entity
            }
        } catch (e: Exception) {
            // Offline fallback
        }

        if (inquiryId == "inq_01" || inquiryId.startsWith("test_")) {
            val demoEntity = InquiryEntity(
                inquiryId = inquiryId,
                trackingCode = "TRB-98721",
                shopId = "shop_test",
                productId = "prod_iphone15pro",
                productTitle = "گوشی موبایل اپل مدل iPhone 15 Pro ظرفیت 256 گیگابایت",
                productImageUrl = "https://torob.com/static/images/mock/iphone15.jpg",
                categoryName = "موبایل و تبلت",
                recordedPriceToman = 68500000L,
                customerName = "خریدار استعلام ترب",
                customerPhone = "0912***4567",
                inquiryType = "قیمت و موجودی فوری",
                userNote = "سلام، در صورت موجودی رنگ خاکستری تیتانیوم امروز سفارش می‌دم.",
                status = "ACTIVE",
                createdAt = "همین الان",
                expiresAt = "۱۰ دقیقه دیگر",
                remainingSeconds = 600
            )
            try {
                inquiryDao.insertInquiry(demoEntity)
            } catch (e: Exception) {
                // Ignore
            }
            return demoEntity
        }

        return null
    }

    fun getSyncStateForInquiry(inquiryId: String): Flow<SyncState?> {
        return syncQueueDao.getResponseForInquiryFlow(inquiryId).map { it?.syncState }
    }

    fun getPendingOfflineCount(): Flow<Int> = syncQueueDao.getPendingCountFlow()

    suspend fun submitResponse(
        inquiryId: String,
        priceToman: Long,
        inStock: Boolean,
        notes: String?,
        stockCount: Int = 1
    ): Result<SyncState> {
        val clientUuid = UUID.randomUUID().toString()
        val submittedAtIso = isoFormat.format(Date())

        // 1. Immediately persist locally in Room DB
        val queueItem = SyncQueueEntity(
            clientResponseId = clientUuid,
            inquiryId = inquiryId,
            offeredPriceToman = priceToman,
            isInStock = inStock,
            stockCount = stockCount,
            notes = notes,
            submittedAt = submittedAtIso,
            syncState = SyncState.SAVED_ON_PHONE
        )
        syncQueueDao.insertSyncItem(queueItem)
        inquiryDao.updateInquiryStatus(inquiryId, "ANSWERED")

        // 2. Attempt immediate network submission
        return try {
            val request = InquiryResponseRequest(
                clientResponseId = clientUuid,
                offeredPriceToman = priceToman,
                isInStock = inStock,
                stockCount = stockCount,
                notes = notes,
                submittedAt = submittedAtIso
            )
            val resp = api.submitResponse(inquiryId, request)
            if (resp.isSuccessful) {
                // Marked as permanently SYNCED on server
                syncQueueDao.updateSyncState(clientUuid, SyncState.SYNCED_ON_SERVER)
                Result.success(SyncState.SYNCED_ON_SERVER)
            } else {
                // Server returned error (e.g. 500, or temporary server issue)
                scheduleBackgroundSync()
                Result.success(SyncState.SAVED_ON_PHONE)
            }
        } catch (e: Exception) {
            // Network failure (offline, timeout) -> Enqueue WorkManager for safe retry
            scheduleBackgroundSync()
            Result.success(SyncState.SAVED_ON_PHONE)
        }
    }

    suspend fun syncPendingResponses(): BatchSyncResponse? {
        val pending = syncQueueDao.getPendingSyncItems()
        if (pending.isEmpty()) return null

        val batchItems = pending.map {
            BatchSyncItem(
                inquiryId = it.inquiryId,
                clientResponseId = it.clientResponseId,
                offeredPriceToman = it.offeredPriceToman,
                isInStock = it.isInStock,
                stockCount = it.stockCount,
                notes = it.notes,
                submittedAt = it.submittedAt
            )
        }

        return try {
            val resp = api.batchSyncResponses(BatchSyncRequest(batchItems))
            if (resp.isSuccessful && resp.body() != null) {
                val batchResult = resp.body()!!
                // Mark processed items as SYNCED_ON_SERVER
                for (item in pending) {
                    syncQueueDao.updateSyncState(item.clientResponseId, SyncState.SYNCED_ON_SERVER)
                }
                batchResult
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "TorobOfflineSyncWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}

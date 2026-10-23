package ir.torob.seller.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inquiries")
data class InquiryEntity(
    @PrimaryKey val inquiryId: String,
    val trackingCode: String,
    val shopId: String,
    val productId: String?,
    val productTitle: String,
    val productImageUrl: String?,
    val categoryName: String?,
    val recordedPriceToman: Long,
    val customerName: String,
    val customerPhone: String?,
    val inquiryType: String,
    val userNote: String?,
    val status: String, // ACTIVE, ANSWERED, EXPIRED
    val createdAt: String,
    val expiresAt: String,
    val remainingSeconds: Long,
    val lastCachedAt: Long = System.currentTimeMillis()
)

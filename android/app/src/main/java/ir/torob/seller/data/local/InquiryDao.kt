package ir.torob.seller.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InquiryDao {
    @Query("SELECT * FROM inquiries ORDER BY createdAt DESC")
    fun getAllInquiries(): Flow<List<InquiryEntity>>

    @Query("SELECT * FROM inquiries WHERE status = :status ORDER BY createdAt DESC")
    fun getInquiriesByStatus(status: String): Flow<List<InquiryEntity>>

    @Query("SELECT * FROM inquiries WHERE status IN ('ANSWERED', 'EXPIRED') ORDER BY createdAt DESC")
    fun getHistoryInquiries(): Flow<List<InquiryEntity>>

    @Query("SELECT * FROM inquiries WHERE inquiryId = :id")
    suspend fun getInquiryById(id: String): InquiryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInquiries(inquiries: List<InquiryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInquiry(inquiry: InquiryEntity)

    @Query("UPDATE inquiries SET status = :status WHERE inquiryId = :id")
    suspend fun updateInquiryStatus(id: String, status: String)

    @Query("DELETE FROM inquiries WHERE status = 'EXPIRED' AND lastCachedAt < :thresholdTime")
    suspend fun clearOldExpired(thresholdTime: Long)
}

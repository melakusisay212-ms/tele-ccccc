package et.teleexpense.app

import android.content.Context
import androidx.room.*

/** Extracted data only. Raw SMS bodies are never stored (only a one-way hash for duplicate-SMS detection). */
@Entity(tableName = "candidates", indices = [Index("smsHash", unique = true)])
data class CandidateRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val smsHash: Int,
    val classification: String, val provider: String, val category: String, val amount: Double?,
    val direction: String, val packageName: String?, val recipient: String?, val transactionId: String?,
    val transferId: String?, val dateTimeIso: String, val receivedIso: String, val explicit: Boolean,
    val reason: String, val confidence: Double,
)

/** Final expense. key = stable group key so user edits survive regrouping. */
@Entity(tableName = "transactions")
data class TxRow(
    @PrimaryKey val key: String, val provider: String, val category: String, val amount: Double,
    val packageName: String?, val recipient: String?, val transactionId: String?,
    val dateTimeIso: String, val ethYear: Int, val ethMonth: Int, val ethDay: Int,
    val smsCount: Int, val confidence: Double,
    val userEdited: Boolean = false, val deleted: Boolean = false,
)

@Dao interface TxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCandidate(c: CandidateRow): Long
    @Query("SELECT * FROM candidates") suspend fun allCandidates(): List<CandidateRow>
    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY dateTimeIso DESC")
    fun observeTx(): kotlinx.coroutines.flow.Flow<List<TxRow>>
    @Query("SELECT * FROM transactions") suspend fun allTx(): List<TxRow>
    @Upsert suspend fun upsert(t: TxRow)
    @Query("DELETE FROM transactions WHERE key = :k AND userEdited = 0") suspend fun dropAuto(k: String)
    @Query("UPDATE transactions SET deleted = 1, userEdited = 1 WHERE key = :k") suspend fun softDelete(k: String)
    @Query("UPDATE

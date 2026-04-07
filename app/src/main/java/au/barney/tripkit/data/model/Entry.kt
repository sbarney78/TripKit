package au.barney.tripkit.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = ListItem::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("list_id")]
)
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val entry_id: Int = 0,
    val entry_name: String,
    val entry_type: String,   // "single" or "container"
    val quantity: Int,
    val is_checked: Int,
    val notes: String?,
    val list_id: Int,
    @ColumnInfo(defaultValue = "")
    val sync_id: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val last_updated: Long = System.currentTimeMillis(),
    val image_path: String? = null,
    @ColumnInfo(defaultValue = "#800000") // Default Maroon
    val color: String = "#800000",
    @ColumnInfo(defaultValue = "0")
    val weightGrams: Int = 0
)

data class EntryWithCount(
    @Embedded val entry: Entry,
    val subItemCount: Int
)

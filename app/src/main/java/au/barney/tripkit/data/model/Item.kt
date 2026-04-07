package au.barney.tripkit.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Entry::class,
            parentColumns = ["entry_id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entry_id"])]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val item_id: Int = 0,
    val entry_id: Int,
    val item_name: String,
    val quantity: Int,
    val notes: String? = null,
    val is_checked: Int = 0,
    @ColumnInfo(defaultValue = "")
    val sync_id: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val last_updated: Long = System.currentTimeMillis(),
    val image_path: String? = null,
    @ColumnInfo(defaultValue = "0")
    val is_container: Boolean = false,
    @ColumnInfo(defaultValue = "#800000") // Default Maroon
    val color: String = "#800000"
)

data class ItemWithCount(
    @Embedded val item: Item,
    val subSubItemCount: Int
)

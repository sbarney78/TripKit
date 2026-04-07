package au.barney.tripkit.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sub_items",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["item_id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["item_id"])]
)
data class SubItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val item_id: Int,
    val name: String,
    val quantity: Int,
    val notes: String? = null,
    val is_checked: Int = 0,
    @ColumnInfo(defaultValue = "")
    val sync_id: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val last_updated: Long = System.currentTimeMillis(),
    val image_path: String? = null,
    @ColumnInfo(defaultValue = "#800000")
    val color: String = "#800000"
)

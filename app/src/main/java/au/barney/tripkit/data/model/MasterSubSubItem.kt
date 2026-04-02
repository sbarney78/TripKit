package au.barney.tripkit.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_sub_sub_items",
    foreignKeys = [
        ForeignKey(
            entity = MasterSubItem::class,
            parentColumns = ["id"],
            childColumns = ["master_sub_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("master_sub_item_id")]
)
data class MasterSubSubItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val master_sub_item_id: Int,
    val name: String,
    val default_quantity: Int = 1,
    val image_path: String? = null,
    @ColumnInfo(defaultValue = "0")
    val is_container: Boolean = false
)

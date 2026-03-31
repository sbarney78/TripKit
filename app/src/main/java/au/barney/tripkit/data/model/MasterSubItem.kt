package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_sub_items",
    foreignKeys = [
        ForeignKey(
            entity = MasterItem::class,
            parentColumns = ["id"],
            childColumns = ["master_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("master_item_id")]
)
data class MasterSubItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val master_item_id: Int,
    val name: String,
    val default_quantity: Int = 1
)

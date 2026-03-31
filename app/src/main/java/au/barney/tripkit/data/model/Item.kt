package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val is_checked: Int = 0
)

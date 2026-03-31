package au.barney.tripkit.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val list_id: Int
)

data class EntryWithCount(
    @Embedded val entry: Entry,
    val subItemCount: Int
)

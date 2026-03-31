package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "menu_items",
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
data class MenuItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val list_id: Int,
    val day: String,
    val meal_type: String,
    val description: String,
    val created_at: String
)

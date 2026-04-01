package au.barney.tripkit.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "lists")
data class ListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val created_at: String,
    val show_inventory: Boolean = true,
    val show_menu: Boolean = true,
    val show_ingredients: Boolean = true,
    val show_itinerary: Boolean = true,
    @ColumnInfo(defaultValue = "")
    val sync_id: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val last_updated: Long = System.currentTimeMillis()
)

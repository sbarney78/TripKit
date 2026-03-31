package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itinerary_items",
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
data class ItineraryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val list_id: Int,
    val day: String,
    val time: String,
    val activity: String,
    val location: String? = null,
    val price: Double? = null,
    val notes: String? = null,
    val departure_day: String? = null,
    val departure_time: String? = null
)

package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payload_locations")
data class PayloadLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val weightLimitGrams: Int? = null,
    val category: String // vehicle, person, pack, storage, etc.
)

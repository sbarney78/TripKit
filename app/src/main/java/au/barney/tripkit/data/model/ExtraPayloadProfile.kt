package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extra_payload_profiles")
data class ExtraPayloadProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val weightGrams: Int,
    val category: String // people, water, fuel, food, recovery, etc.
)

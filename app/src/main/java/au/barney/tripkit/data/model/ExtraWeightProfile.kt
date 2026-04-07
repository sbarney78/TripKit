package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extra_weight_profiles")
data class ExtraWeightProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val weightGrams: Int,
    val category: String // vehicle, person, gear, etc.
)

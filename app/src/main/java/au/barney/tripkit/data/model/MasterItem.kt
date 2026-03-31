package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_items")
data class MasterItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val default_quantity: Int = 1,
    val category: String? = null,
    val is_container: Boolean = false
)

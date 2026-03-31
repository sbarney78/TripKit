package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = IngredientGroup::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("group_id")]
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val group_id: Int,
    val ingredient_name: String,
    val quantity: Int = 1,
    val notes: String? = null,
    val is_checked: Int = 0,
    val created_at: String
)

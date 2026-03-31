package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredient_groups",
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
data class IngredientGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val list_id: Int,
    val group_name: String,
    val created_at: String
)

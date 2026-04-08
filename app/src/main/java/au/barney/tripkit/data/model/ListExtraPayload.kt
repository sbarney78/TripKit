package au.barney.tripkit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "list_extra_payloads",
    primaryKeys = ["listId", "payloadProfileId"],
    foreignKeys = [
        ForeignKey(
            entity = ListItem::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExtraPayloadProfile::class,
            parentColumns = ["id"],
            childColumns = ["payloadProfileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ListExtraPayload(
    val listId: Int,
    val payloadProfileId: Int
)

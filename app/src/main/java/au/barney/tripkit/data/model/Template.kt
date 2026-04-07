package au.barney.tripkit.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    @ColumnInfo(defaultValue = "")
    val sync_id: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val last_updated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "template_entries",
    foreignKeys = [
        ForeignKey(
            entity = Template::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("template_id")]
)
data class TemplateEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val template_id: Int,
    val name: String,
    val default_quantity: Int = 1,
    val is_container: Boolean = false,
    val image_path: String? = null,
    val color: String = "#800000",
    @ColumnInfo(defaultValue = "0")
    val is_checked: Int = 0,
    @ColumnInfo(defaultValue = "")
    val sync_id: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val last_updated: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val weightGrams: Int = 0
)

@Entity(
    tableName = "template_items",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntry::class,
            parentColumns = ["id"],
            childColumns = ["template_entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("template_entry_id")]
)
data class TemplateItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val template_entry_id: Int,
    val name: String,
    val default_quantity: Int = 1,
    val is_container: Boolean = false,
    val image_path: String? = null,
    val color: String = "#800000",
    @ColumnInfo(defaultValue = "0")
    val is_checked: Int = 0,
    @ColumnInfo(defaultValue = "")
    val sync_id: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val last_updated: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val weightGrams: Int = 0
)

@Entity(
    tableName = "template_sub_items",
    foreignKeys = [
        ForeignKey(
            entity = TemplateItem::class,
            parentColumns = ["id"],
            childColumns = ["template_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("template_item_id")]
)
data class TemplateSubItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val template_item_id: Int,
    val name: String,
    val default_quantity: Int = 1,
    val image_path: String? = null,
    @ColumnInfo(defaultValue = "0")
    val is_container: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val is_checked: Int = 0,
    @ColumnInfo(defaultValue = "")
    val sync_id: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val last_updated: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "#800000")
    val color: String = "#800000",
    @ColumnInfo(defaultValue = "0")
    val weightGrams: Int = 0
)

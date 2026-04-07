package au.barney.tripkit.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import au.barney.tripkit.data.model.*

@Database(
    entities = [
        ListItem::class,
        Entry::class,
        Item::class,
        SubItem::class,
        MenuItem::class,
        IngredientGroup::class,
        Ingredient::class,
        MasterItem::class,
        MasterSubItem::class,
        MasterSubSubItem::class,
        ItineraryItem::class,
        Template::class,
        TemplateEntry::class,
        TemplateItem::class,
        TemplateSubItem::class,
        ExtraWeightProfile::class
    ],
    version = 18,
    autoMigrations = [
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 18)
    ],
    exportSchema = true
)
abstract class TripKitDatabase : RoomDatabase() {

    abstract fun tripKitDao(): TripKitDao

    companion object {
        @Volatile
        private var INSTANCE: TripKitDatabase? = null

        fun getDatabase(context: Context): TripKitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripKitDatabase::class.java,
                    "tripkit_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

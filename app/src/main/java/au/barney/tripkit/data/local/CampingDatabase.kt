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
        MenuItem::class,
        IngredientGroup::class,
        Ingredient::class,
        MasterItem::class,
        MasterSubItem::class,
        ItineraryItem::class
    ],
    version = 8,
    autoMigrations = [
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8)
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

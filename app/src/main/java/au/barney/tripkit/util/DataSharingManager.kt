package au.barney.tripkit.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import au.barney.tripkit.data.model.*
import au.barney.tripkit.data.repository.TripKitRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object DataSharingManager {

    private val gson = Gson()

    /**
     * Exports a single trip list (and all associated data) to a JSON file and shares it.
     */
    suspend fun exportTripList(context: Context, repository: TripKitRepository, listId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val list = repository.getList(listId) ?: return@withContext
                val data = FullTripData(
                    list = list,
                    itinerary = repository.getItinerarySync(listId),
                    entries = repository.getEntriesSync(listId),
                    allItems = repository.getAllItemsForListSync(listId),
                    allSubItems = repository.getAllSubItemsForListSync(listId),
                    menu = repository.getMenuSync(listId),
                    ingredientGroups = repository.getIngredientGroupsSync(listId),
                    allIngredients = repository.getAllIngredientsForListSync(listId)
                )

                val json = gson.toJson(data)
                
                val exportDir = File(context.filesDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()
                
                // Clean filename
                val safeName = list.name.replace(Regex("[^a-zA-Z0-9]"), "_")
                val exportFile = File(exportDir, "${safeName}_trip.json")
                
                FileOutputStream(exportFile).use { it.write(json.toByteArray()) }

                val uri = FileProvider.getUriForFile(context, "au.barney.tripkit.fileprovider", exportFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(intent, "Share Trip List")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Reads a trip list from a JSON file URI.
     */
    suspend fun readTripFile(context: Context, uri: Uri): FullTripData? {
        return withContext(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { 
                    it.bufferedReader().readText() 
                } ?: return@withContext null

                gson.fromJson(json, FullTripData::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Imports a trip list as a brand new list.
     */
    suspend fun importAsNew(context: Context, repository: TripKitRepository, data: FullTripData) {
        withContext(Dispatchers.IO) {
            try {
                repository.importFullTripData(data)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Trip '${data.list?.name ?: "Unknown"}' imported as a new list!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Merges imported data into an existing list.
     */
    suspend fun mergeIntoExisting(context: Context, repository: TripKitRepository, data: FullTripData) {
        withContext(Dispatchers.IO) {
            try {
                repository.mergeTripData(data)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Trip '${data.list?.name ?: "Unknown"}' synced successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

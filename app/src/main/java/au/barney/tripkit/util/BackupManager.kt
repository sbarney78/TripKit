package au.barney.tripkit.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.sqlite.db.SimpleSQLiteQuery
import au.barney.tripkit.data.local.TripKitDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object BackupManager {

    /**
     * Copies the current database files to a shareable file and opens the system chooser.
     * This allows the user to save the backup to Google Drive, Email, or local storage.
     */
    fun backupDatabase(context: Context) {
        try {
            val db = TripKitDatabase.getDatabase(context)
            
            // 1. Force Checkpoint to merge WAL (Write-Ahead Log) into the main DB file.
            // This ensures the main file contains all the latest changes (Master Inventory, etc.)
            db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA checkpoint(FULL)"))

            val dbFile = context.getDatabasePath("tripkit_database")
            
            // 2. Prepare the export file
            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val backupFile = File(exportDir, "tripkit_backup.db")
            
            // 3. Perform the copy
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 4. Trigger the system Share Intent
            val uri = FileProvider.getUriForFile(context, "au.barney.tripkit.fileprovider", backupFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(intent, "Save Backup To...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Replaces the current app database with the file provided via the Uri.
     */
    fun restoreDatabase(context: Context, backupUri: Uri) {
        try {
            val dbFile = context.getDatabasePath("tripkit_database")
            
            // 1. Close the active database connection to prevent corruption
            TripKitDatabase.getDatabase(context).close()

            // 2. Overwrite the existing database file with the backup data
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // 3. Delete the -wal and -shm files if they exist. 
            // This forces the next database open to use the freshly restored main file.
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            Toast.makeText(context, "Restore successful! Please restart the app.", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

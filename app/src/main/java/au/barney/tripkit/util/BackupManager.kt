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

    fun backupDatabase(context: Context) {
        try {
            // 1. Force Checkpoint to ensure WAL journal is merged into main DB file
            val db = TripKitDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA checkpoint(FULL)"))

            val dbFile = context.getDatabasePath("tripkit_database")
            
            // 2. Use a dedicated folder in filesDir
            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val backupFile = File(exportDir, "tripkit_backup.db")
            
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 3. Share
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

    fun restoreDatabase(context: Context, backupUri: Uri) {
        try {
            val dbFile = context.getDatabasePath("tripkit_database")
            
            // Close the database first
            TripKitDatabase.getDatabase(context).close()

            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(context, "Restore successful! Please restart the app.", Toast.LENGTH_LONG).show()
            
            // Optionally force a restart (though simple toast is safer for some UX)
            // Process.killProcess(Process.myPid())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

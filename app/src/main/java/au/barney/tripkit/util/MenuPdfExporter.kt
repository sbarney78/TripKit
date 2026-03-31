package au.barney.tripkit.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import au.barney.tripkit.data.model.MenuItem

object MenuPdfExporter {

    fun export(context: Context, title: String, menu: List<MenuItem>): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = 50
        val paint = android.graphics.Paint().apply {
            textSize = 18f
        }

        canvas.drawText(title, 50f, y.toFloat(), paint)
        y += 40

        val grouped = menu.groupBy { it.day }

        grouped.forEach { (day, items) ->
            canvas.drawText(day, 50f, y.toFloat(), paint)
            y += 30

            items.forEach {
                canvas.drawText("• ${it.meal_type}: ${it.description}", 70f, y.toFloat(), paint)
                y += 25
            }

            y += 20
        }

        document.finishPage(page)

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, "$title.pdf")

        return try {
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            file
        } catch (e: Exception) {
            document.close()
            null
        }
    }
}

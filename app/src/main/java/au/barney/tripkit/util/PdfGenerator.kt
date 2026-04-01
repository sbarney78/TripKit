package au.barney.tripkit.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import au.barney.tripkit.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object PdfGenerator {

    private val MAROON = Color.rgb(128, 0, 0)
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f

    private fun drawHeader(canvas: android.graphics.Canvas, title: String, listName: String) {
        val headerPaint = Paint().apply { color = MAROON; style = Paint.Style.FILL }
        val titlePaint = Paint().apply { 
            color = Color.WHITE
            isFakeBoldText = true
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawRect(0f, 0f, PAGE_WIDTH, 90f, headerPaint)
        canvas.drawText(title, PAGE_WIDTH / 2, 40f, titlePaint)
        canvas.drawText("Trip: $listName", PAGE_WIDTH / 2, 75f, titlePaint)
    }

    private fun drawMultilineText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, paint: Paint, spacing: Float = 20f): Float {
        var currentY = y
        text.split("\n").forEach { line ->
            canvas.drawText(line, x, currentY, paint)
            currentY += spacing
        }
        return currentY
    }

    private fun checkNewPage(pdfDocument: PdfDocument, currentPage: PdfDocument.Page, y: Float, title: String, listName: String): Triple<PdfDocument.Page, android.graphics.Canvas, Float> {
        if (y > 800) {
            pdfDocument.finishPage(currentPage)
            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
            val newPage = pdfDocument.startPage(newPageInfo)
            drawHeader(newPage.canvas, title, listName)
            return Triple(newPage, newPage.canvas, 120f)
        }
        return Triple(currentPage, currentPage.canvas, y)
    }

    private fun formatShortTime(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return ""
        val sdfFullTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return try {
            val date = sdfFullTime.parse(timeStr)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val hour = if (cal.get(Calendar.HOUR) == 0) 12 else cal.get(Calendar.HOUR)
                val minute = cal.get(Calendar.MINUTE)
                val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                if (minute == 0) "$hour $amPm" else "$hour:${String.format(Locale.getDefault(), "%02d", minute)} $amPm"
            } else timeStr
        } catch (e: Exception) { timeStr }
    }

    private fun getDurationText(item: ItineraryItem): String? {
        val sdfFullDate = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val sdfFullTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return try {
            val startD = sdfFullDate.parse(item.day)
            val startT = if (item.time.isNotBlank()) sdfFullTime.parse(item.time) else null
            if (startD != null) {
                val startCal = Calendar.getInstance().apply {
                    time = startD
                    if (startT != null) {
                        val tCal = Calendar.getInstance().apply { time = startT }
                        set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, tCal.get(Calendar.MINUTE))
                    }
                }
                val hasDepDay = !item.departure_day.isNullOrBlank()
                val endDayStr = if (hasDepDay) item.departure_day else item.day
                val endD = sdfFullDate.parse(endDayStr!!)
                val hasDepTime = !item.departure_time.isNullOrBlank()
                val endT = if (hasDepTime) sdfFullTime.parse(item.departure_time!!) else null
                if (endD != null) {
                    val endCal = Calendar.getInstance().apply {
                        time = endD
                        if (endT != null) {
                            val tCal = Calendar.getInstance().apply { time = endT }
                            set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, tCal.get(Calendar.MINUTE))
                        }
                    }
                    val diff = endCal.timeInMillis - startCal.timeInMillis
                    if (diff > 0) {
                        if (hasDepDay) {
                            val diffDays = TimeUnit.DAYS.convert(endD.time - startD.time, TimeUnit.MILLISECONDS)
                            if (diffDays > 0) "Duration: $diffDays night${if (diffDays > 1) "s" else ""}"
                            else {
                                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                                val minutes = (TimeUnit.MILLISECONDS.toMinutes(diff) % 60)
                                if (hours > 0) "Duration: ${hours}h ${minutes}m" else "Duration: ${minutes}m"
                            }
                        } else if (hasDepTime) {
                            val hours = TimeUnit.MILLISECONDS.toHours(diff)
                            val minutes = (TimeUnit.MILLISECONDS.toMinutes(diff) % 60)
                            if (hours > 0) "Duration: ${hours}h ${minutes}m" else "Duration: ${minutes}m"
                        } else null
                    } else null
                } else null
            } else null
        } catch (e: Exception) { null }
    }

    fun generateItineraryPdf(context: Context, listName: String, items: List<ItineraryItem>) {
        val pdfDocument = PdfDocument()
        val dayPaint = Paint().apply { isFakeBoldText = true; textSize = 18f; color = MAROON }
        val activityPaint = Paint().apply { isFakeBoldText = true; textSize = 14f }
        val timePaint = Paint().apply { textSize = 12f; color = Color.rgb(100, 100, 100) }
        val durationPaint = Paint().apply { isFakeBoldText = true; textSize = 11f; color = Color.rgb(0, 100, 0) }
        val detailPaint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val notesPaint = Paint().apply { 
            textSize = 11f
            color = Color.DKGRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var myPage = pdfDocument.startPage(pageInfo)
        var canvas = myPage.canvas
        drawHeader(canvas, "Itinerary", listName)
        var y = 120f

        val sdfFullDate = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val sdfShortDate = SimpleDateFormat("EEE dd MMM", Locale.getDefault())

        val sorted = items.sortedWith { a, b ->
            val d1 = try { sdfFullDate.parse(a.day) } catch (e: Exception) { null }
            val d2 = try { sdfFullDate.parse(b.day) } catch (e: Exception) { null }
            if (d1 != null && d2 != null) {
                val comp = d1.compareTo(d2)
                if (comp != 0) comp else a.time.compareTo(b.time)
            } else a.day.compareTo(b.day)
        }

        sorted.groupBy { it.day }.forEach { (day, activities) ->
            val res = checkNewPage(pdfDocument, myPage, y, "Itinerary", listName)
            myPage = res.first; canvas = res.second; y = res.third

            canvas.drawText(day, 40f, y, dayPaint)
            y += 30f

            activities.forEach { act ->
                val r2 = checkNewPage(pdfDocument, myPage, y, "Itinerary", listName)
                myPage = r2.first; canvas = r2.second; y = r2.third

                // 1. Activity Name
                canvas.drawText(act.activity, 45f, y, activityPaint)
                y += 18f

                // 2. Arrival -> Departure
                val startTime = formatShortTime(act.time)
                val endTime = formatShortTime(act.departure_time)
                val hasDepDay = !act.departure_day.isNullOrBlank()
                val timeString = buildString {
                    append(startTime)
                    if (hasDepDay || endTime.isNotEmpty()) {
                        append(" -> ")
                        if (hasDepDay) {
                            try {
                                val d = sdfFullDate.parse(act.departure_day!!)
                                if (d != null) append(sdfShortDate.format(d)).append(", ")
                            } catch (e: Exception) { append(act.departure_day).append(", ") }
                        }
                        append(endTime)
                    }
                }
                canvas.drawText(timeString, 45f, y, timePaint)
                y += 16f

                // 3. Duration
                getDurationText(act)?.let {
                    canvas.drawText(it, 45f, y, durationPaint)
                    y += 16f
                }

                // 4. Location
                if (!act.location.isNullOrBlank()) {
                    canvas.drawText("Location: ${act.location}", 45f, y, detailPaint)
                    y += 16f
                }

                // 5. Price
                if (act.price != null && act.price!! > 0) {
                    canvas.drawText("Price: AU$${String.format(Locale.getDefault(), "%.2f", act.price)}", 45f, y, detailPaint)
                    y += 16f
                }

                // 6. Category
                if (!act.category.isNullOrBlank()) {
                    canvas.drawText("Category: ${act.category}", 45f, y, detailPaint)
                    y += 16f
                }

                // 7. Booking Ref
                if (!act.booking_ref.isNullOrBlank()) {
                    canvas.drawText("Booking Ref: ${act.booking_ref}", 45f, y, detailPaint)
                    y += 16f
                }

                // 8. Notes
                if (!act.notes.isNullOrBlank()) {
                    y = drawMultilineText(canvas, "Notes: ${act.notes!!}", 45f, y, notesPaint, 16f)
                }
                y += 15f
            }
            y += 10f
        }

        pdfDocument.finishPage(myPage)
        saveAndSharePdf(context, pdfDocument, "Itinerary_${listName.replace(" ", "_")}.pdf")
    }

    fun generateFullTripPdf(context: Context, data: FullTripData) {
        val listName = data.list.name
        val pdfDocument = PdfDocument()
        val sectionPaint = Paint().apply { isFakeBoldText = true; textSize = 20f; color = MAROON; textAlign = Paint.Align.CENTER }
        val title = "Full Trip Plan"
        
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var myPage = pdfDocument.startPage(pageInfo)
        var canvas = myPage.canvas
        var y = 120f
        drawHeader(canvas, title, listName)

        // ITINERARY
        canvas.drawText("--- ITINERARY ---", PAGE_WIDTH/2, y, sectionPaint); y += 45f
        val sdfFullDate = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val sdfShortDate = SimpleDateFormat("EEE dd MMM", Locale.getDefault())
        val sortedItinerary = data.itinerary.sortedWith { a, b ->
            val d1 = try { sdfFullDate.parse(a.day) } catch (e: Exception) { null }
            val d2 = try { sdfFullDate.parse(b.day) } catch (e: Exception) { null }
            if (d1 != null && d2 != null) {
                val comp = d1.compareTo(d2)
                if (comp != 0) comp else a.time.compareTo(b.time)
            } else a.day.compareTo(b.day)
        }
        
        val activityPaint = Paint().apply { isFakeBoldText = true; textSize = 14f }
        val detailPaint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val durationPaint = Paint().apply { isFakeBoldText = true; textSize = 11f; color = Color.rgb(0, 100, 0) }
        val notesPaint = Paint().apply { 
            textSize = 11f
            color = Color.DKGRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        
        sortedItinerary.groupBy { it.day }.forEach { (day, activities) ->
            val r = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r.first; canvas = r.second; y = r.third
            canvas.drawText(day, 40f, y, Paint().apply { isFakeBoldText = true; textSize = 16f; color = MAROON }); y += 25f
            activities.forEach { act ->
                val r2 = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r2.first; canvas = r2.second; y = r2.third
                
                // 1. Activity Name
                canvas.drawText(act.activity, 45f, y, activityPaint); y += 18f
                
                // 2. Arrival -> Departure
                val startTime = formatShortTime(act.time)
                val endTime = formatShortTime(act.departure_time)
                val hasDepDay = !act.departure_day.isNullOrBlank()
                val timeString = buildString {
                    append(startTime)
                    if (hasDepDay || endTime.isNotEmpty()) {
                        append(" -> ")
                        if (hasDepDay) {
                            try {
                                val d = sdfFullDate.parse(act.departure_day!!)
                                if (d != null) append(sdfShortDate.format(d)).append(", ")
                            } catch (e: Exception) { append(act.departure_day).append(", ") }
                        }
                        append(endTime)
                    }
                }
                canvas.drawText(timeString, 45f, y, detailPaint); y += 16f

                // 3. Duration
                getDurationText(act)?.let {
                    canvas.drawText(it, 45f, y, durationPaint)
                    y += 16f
                }

                // 4. Location
                if (!act.location.isNullOrBlank()) {
                    canvas.drawText("Location: ${act.location}", 45f, y, detailPaint); y += 16f
                }

                // 5. Price
                if (act.price != null && act.price!! > 0) {
                    canvas.drawText("Price: AU$${String.format(Locale.getDefault(), "%.2f", act.price)}", 45f, y, detailPaint); y += 16f
                }

                // 6. Category
                if (!act.category.isNullOrBlank()) {
                    canvas.drawText("Category: ${act.category}", 45f, y, detailPaint); y += 16f
                }

                // 7. Booking Ref
                if (!act.booking_ref.isNullOrBlank()) {
                    canvas.drawText("Booking Ref: ${act.booking_ref}", 45f, y, detailPaint); y += 16f
                }

                // 8. Notes
                if (!act.notes.isNullOrBlank()) {
                    y = drawMultilineText(canvas, "Notes: ${act.notes}", 45f, y, notesPaint, 16f)
                }
                y += 10f
            }
            y += 15f
        }

        // INVENTORY
        y += 30f
        val r3 = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r3.first; canvas = r3.second; y = r3.third
        canvas.drawText("--- INVENTORY ---", PAGE_WIDTH/2, y, sectionPaint); y += 45f
        val itemsByEntry = data.allItems.groupBy { it.entry_id }
        data.entries.forEach { entry ->
            val r = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r.first; canvas = r.second; y = r.third
            val status = if (entry.is_checked == 1) "[X]" else "[ ]"
            canvas.drawText("$status ${entry.entry_name} (Qty: ${entry.quantity})", 40f, y, Paint().apply { textSize = 13f }); y += 20f
            if (entry.entry_type == "container") {
                itemsByEntry[entry.entry_id]?.forEach { sub ->
                    val rSub = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = rSub.first; canvas = rSub.second; y = rSub.third
                    val itemStatus = if (sub.is_checked == 1) "[X]" else "[ ]"
                    canvas.drawText("      $itemStatus ${sub.item_name} (Qty: ${sub.quantity})", 40f, y, detailPaint); y += 18f
                }
            }
        }

        pdfDocument.finishPage(myPage)
        saveAndSharePdf(context, pdfDocument, "FullTrip_${listName.replace(" ", "_")}.pdf")
    }

    fun generateInventoryPdf(context: Context, listName: String, entries: List<Entry>, allItems: Map<Int, List<Item>>) {
        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply { textSize = 14f }
        val boldPaint = Paint().apply { isFakeBoldText = true; textSize = 14f }
        val itemPaint = Paint().apply { textSize = 12f; color = Color.DKGRAY }

        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var myPage = pdfDocument.startPage(pageInfo)
        var canvas = myPage.canvas
        drawHeader(canvas, "TripKit Inventory", listName)
        var y = 120f

        entries.forEach { entry ->
            val res = checkNewPage(pdfDocument, myPage, y, "TripKit Inventory", listName); myPage = res.first; canvas = res.second; y = res.third
            val status = if (entry.is_checked == 1) "[X]" else "[ ]"
            canvas.drawText("$status ${entry.entry_name} (Qty: ${entry.quantity})", 40f, y, boldPaint); y += 20f

            if (entry.entry_type == "container") {
                allItems[entry.entry_id]?.forEach { item ->
                    val r = checkNewPage(pdfDocument, myPage, y, "TripKit Inventory", listName); myPage = r.first; canvas = r.second; y = r.third
                    val itemStatus = if (item.is_checked == 1) "[X]" else "[ ]"
                    canvas.drawText("      $itemStatus ${item.item_name} (Qty: ${item.quantity})", 40f, y, itemPaint); y += 18f
                }
            }
            if (!entry.notes.isNullOrEmpty()) {
                y = drawMultilineText(canvas, "   Notes: ${entry.notes}", 40f, y, textPaint)
            }
            y += 10f
        }
        pdfDocument.finishPage(myPage)
        saveAndSharePdf(context, pdfDocument, "Inventory_${listName.replace(" ", "_")}.pdf")
    }

    fun generateMenuPdf(context: Context, listName: String, menuItems: List<MenuItem>) {
        val pdfDocument = PdfDocument()
        val dayPaint = Paint().apply { isFakeBoldText = true; textSize = 18f; color = MAROON }
        val mealTypePaint = Paint().apply { isFakeBoldText = true; textSize = 14f }
        val descriptionPaint = Paint().apply { textSize = 14f }

        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var myPage = pdfDocument.startPage(pageInfo)
        var canvas = myPage.canvas
        drawHeader(canvas, "Meal Plan", listName)
        var y = 120f

        menuItems.groupBy { it.day }.forEach { (day, items) ->
            val res = checkNewPage(pdfDocument, myPage, y, "Meal Plan", listName); myPage = res.first; canvas = res.second; y = res.third
            canvas.drawText(day, 40f, y, dayPaint); y += 30f
            items.forEach { item ->
                val r = checkNewPage(pdfDocument, myPage, y, "Meal Plan", listName); myPage = r.first; canvas = r.second; y = r.third
                canvas.drawText(item.meal_type, 40f, y, mealTypePaint); y += 20f
                y = drawMultilineText(canvas, item.description, 40f, y, descriptionPaint); y += 10f
            }
            y += 10f
        }
        pdfDocument.finishPage(myPage)
        saveAndSharePdf(context, pdfDocument, "Menu_${listName.replace(" ", "_")}.pdf")
    }

    fun generateIngredientsPdf(context: Context, listName: String, groups: List<IngredientGroup>, allIngredients: Map<Int, List<Ingredient>>) {
        val pdfDocument = PdfDocument()
        val groupPaint = Paint().apply { isFakeBoldText = true; textSize = 16f; color = MAROON }
        val textPaint = Paint().apply { textSize = 14f }

        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var myPage = pdfDocument.startPage(pageInfo)
        var canvas = myPage.canvas
        drawHeader(canvas, "Shopping List", listName)
        var y = 120f

        groups.forEach { group ->
            val res = checkNewPage(pdfDocument, myPage, y, "Shopping List", listName); myPage = res.first; canvas = res.second; y = res.third
            canvas.drawText(group.group_name, 40f, y, groupPaint); y += 25f
            allIngredients[group.id]?.forEach { ing ->
                val r = checkNewPage(pdfDocument, myPage, y, "Shopping List", listName); myPage = r.first; canvas = r.second; y = r.third
                canvas.drawText("• ${ing.ingredient_name}", 40f, y, textPaint); y += 20f
            }
            y += 15f
        }
        pdfDocument.finishPage(myPage)
        saveAndSharePdf(context, pdfDocument, "Shopping_List_${listName.replace(" ", "_")}.pdf")
    }

    private fun saveAndSharePdf(context: Context, pdfDocument: PdfDocument, fileName: String) {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)
        try {
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            val uri = FileProvider.getUriForFile(context, "au.barney.tripkit.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

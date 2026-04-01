package au.barney.tripkit.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.*
import android.print.*
import android.widget.Toast
import androidx.core.content.FileProvider
import au.barney.tripkit.data.model.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object PdfGenerator {

    private val MAROON = Color.rgb(128, 0, 0)
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val COL1_X = 40f
    private const val COL2_X = 315f
    private const val COL_WIDTH = 240f
    private const val START_Y = 120f
    private const val MAX_Y = 800f

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

    private fun drawMultilineText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, paint: Paint, maxWidth: Float, spacing: Float = 16f): Float {
        var currentY = y
        text.split("\n").forEach { block ->
            if (block.isBlank()) {
                currentY += spacing
                return@forEach
            }
            val words = block.split(" ")
            var line = ""
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) <= maxWidth) {
                    line = test
                } else {
                    canvas.drawText(line, x, currentY, paint)
                    currentY += spacing
                    line = word
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, x, currentY, paint)
                currentY += spacing
            }
        }
        return currentY
    }

    private fun checkLayout(pdfDocument: PdfDocument, currentPage: PdfDocument.Page, y: Float, column: Int, title: String, listName: String, sectionStartY: Float): Triple<PdfDocument.Page, Int, Float> {
        if (y > MAX_Y) {
            if (column == 1) {
                return Triple(currentPage, 2, sectionStartY)
            } else {
                pdfDocument.finishPage(currentPage)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                val newPage = pdfDocument.startPage(newPageInfo)
                drawHeader(newPage.canvas, title, listName)
                return Triple(newPage, 1, START_Y)
            }
        }
        return Triple(currentPage, column, y)
    }

    private fun startNewPage(pdfDocument: PdfDocument, title: String, listName: String): Pair<PdfDocument.Page, android.graphics.Canvas> {
        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
        val newPage = pdfDocument.startPage(newPageInfo)
        drawHeader(newPage.canvas, title, listName)
        return Pair(newPage, newPage.canvas)
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
            val endDayStr = if (!item.departure_day.isNullOrBlank()) item.departure_day else item.day
            val endD = sdfFullDate.parse(endDayStr!!)
            
            if (startD != null && endD != null) {
                if (startD.before(endD)) {
                    val diffInMillis = endD.time - startD.time
                    val nights = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                    if (nights > 0) "Duration: $nights night${if (nights > 1) "s" else ""}" else null
                } else {
                    val startT = if (item.time.isNotBlank()) sdfFullTime.parse(item.time) else null
                    val endT = if (!item.departure_time.isNullOrBlank()) sdfFullTime.parse(item.departure_time!!) else null
                    if (startT != null && endT != null) {
                        val diff = endT.time - startT.time
                        if (diff > 0) {
                            val hours = TimeUnit.MILLISECONDS.toHours(diff)
                            val minutes = (TimeUnit.MILLISECONDS.toMinutes(diff) % 60)
                            if (hours > 0) "Duration: ${hours}h ${minutes}m" else "Duration: ${minutes}m"
                        } else null
                    } else null
                }
            } else null
        } catch (e: Exception) { null }
    }

    fun generateItineraryPdf(context: Context, listName: String, items: List<ItineraryItem>): File? {
        val pdfDocument = PdfDocument()
        val dayPaint = Paint().apply { isFakeBoldText = true; textSize = 16f; color = MAROON }
        val activityPaint = Paint().apply { isFakeBoldText = true; textSize = 13f }
        val detailPaint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val durationPaint = Paint().apply { isFakeBoldText = true; textSize = 10f; color = Color.rgb(0, 100, 0) }
        val notesPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) }

        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var myPage = pdfDocument.startPage(pageInfo)
        var canvas = myPage.canvas
        drawHeader(canvas, "Itinerary", listName)
        
        var y = START_Y
        var column = 1
        var x = COL1_X
        var sectionStartY = START_Y

        val sdfFullDate = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val sdfShortDate = SimpleDateFormat("EEE dd MMM", Locale.getDefault())
        val sdfFullTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val sorted = items.sortedWith { a, b ->
            val d1 = try { sdfFullDate.parse(a.day) } catch (e: Exception) { null }
            val d2 = try { sdfFullDate.parse(b.day) } catch (e: Exception) { null }
            if (d1 != null && d2 != null) {
                val comp = d1.compareTo(d2)
                if (comp != 0) comp else {
                    val t1 = try { sdfFullTime.parse(a.time) } catch (e: Exception) { null }
                    val t2 = try { sdfFullTime.parse(b.time) } catch (e: Exception) { null }
                    if (t1 != null && t2 != null) t1.compareTo(t2) else a.time.compareTo(b.time)
                }
            } else a.day.compareTo(b.day)
        }

        sorted.groupBy { it.day }.forEach { (day, activities) ->
            val rDay = checkLayout(pdfDocument, myPage, y, column, "Itinerary", listName, sectionStartY)
            myPage = rDay.first; column = rDay.second; y = rDay.third; canvas = myPage.canvas
            if (column == 1 && y == START_Y) sectionStartY = START_Y
            x = if (column == 1) COL1_X else COL2_X

            canvas.drawText(day, x, y, dayPaint)
            y += 25f

            activities.forEach { act ->
                val r = checkLayout(pdfDocument, myPage, y, column, "Itinerary", listName, sectionStartY)
                myPage = r.first; column = r.second; y = r.third; canvas = myPage.canvas
                if (column == 1 && y == START_Y) sectionStartY = START_Y
                x = if (column == 1) COL1_X else COL2_X

                canvas.drawText(act.activity, x, y, activityPaint); y += 16f
                
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
                canvas.drawText(timeString, x, y, detailPaint); y += 14f

                getDurationText(act)?.let {
                    canvas.drawText(it, x, y, durationPaint); y += 14f
                }

                if (!act.location.isNullOrBlank()) {
                    y = drawMultilineText(canvas, "Location: ${act.location}", x, y, detailPaint, COL_WIDTH, 14f)
                }
                if (act.price != null && act.price!! > 0) {
                    canvas.drawText("Price: AU$${String.format(Locale.getDefault(), "%.2f", act.price)}", x, y, detailPaint); y += 14f
                }
                if (!act.booking_ref.isNullOrBlank()) {
                    canvas.drawText("Ref: ${act.booking_ref}", x, y, detailPaint); y += 14f
                }
                if (!act.notes.isNullOrBlank()) {
                    y = drawMultilineText(canvas, "Notes: ${act.notes}", x, y, notesPaint, COL_WIDTH, 14f)
                }
                y += 10f
            }
            y += 5f
        }

        pdfDocument.finishPage(myPage)
        return savePdf(context, pdfDocument, "Itinerary_${listName.replace(" ", "_")}.pdf")
    }

    fun generateFullTripPdf(context: Context, data: FullTripData): File? {
        val listName = data.list.name
        val pdfDocument = PdfDocument()
        val sectionPaint = Paint().apply { isFakeBoldText = true; textSize = 18f; color = MAROON; textAlign = Paint.Align.CENTER }
        val title = "Full Trip Plan"
        
        var (myPage, canvas) = startNewPage(pdfDocument, title, listName)
        var y = START_Y
        var column = 1
        var x = COL1_X
        var sectionStartY = START_Y

        val sdfFullDate = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val sdfShortDate = SimpleDateFormat("EEE dd MMM", Locale.getDefault())
        val sdfFullTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val detailPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        val activityPaint = Paint().apply { isFakeBoldText = true; textSize = 12f }
        val durationPaint = Paint().apply { isFakeBoldText = true; textSize = 10f; color = Color.rgb(0, 100, 0) }

        // --- ITINERARY SECTION ---
        canvas.drawText("--- ITINERARY ---", PAGE_WIDTH/2, y, sectionPaint); y += 35f
        sectionStartY = y
        
        val sortedItinerary = data.itinerary.sortedWith { a, b ->
            val d1 = try { sdfFullDate.parse(a.day) } catch (e: Exception) { null }
            val d2 = try { sdfFullDate.parse(b.day) } catch (e: Exception) { null }
            if (d1 != null && d2 != null) {
                val comp = d1.compareTo(d2)
                if (comp != 0) comp else {
                    val t1 = try { sdfFullTime.parse(a.time) } catch (e: Exception) { null }
                    val t2 = try { sdfFullTime.parse(b.time) } catch (e: Exception) { null }
                    if (t1 != null && t2 != null) t1.compareTo(t2) else a.time.compareTo(b.time)
                }
            } else a.day.compareTo(b.day)
        }

        sortedItinerary.groupBy { it.day }.forEach { (day, activities) ->
            val rDay = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
            myPage = rDay.first; column = rDay.second; y = rDay.third; canvas = myPage.canvas
            if (column == 1 && y == START_Y) sectionStartY = START_Y
            x = if (column == 1) COL1_X else COL2_X
            
            canvas.drawText(day, x, y, Paint().apply { isFakeBoldText = true; textSize = 13f; color = MAROON }); y += 18f
            activities.forEach { act ->
                val r2 = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
                myPage = r2.first; column = r2.second; y = r2.third; canvas = myPage.canvas
                if (column == 1 && y == START_Y) sectionStartY = START_Y
                x = if (column == 1) COL1_X else COL2_X

                canvas.drawText(act.activity, x, y, activityPaint); y += 14f
                val timeStr = "${formatShortTime(act.time)}${if (!act.departure_time.isNullOrBlank()) " -> " + formatShortTime(act.departure_time) else ""}"
                canvas.drawText(timeStr, x, y, detailPaint); y += 12f

                getDurationText(act)?.let {
                    canvas.drawText(it, x, y, durationPaint); y += 12f
                }

                if (!act.location.isNullOrBlank()) {
                    y = drawMultilineText(canvas, act.location!!, x, y, detailPaint, COL_WIDTH, 12f)
                }
                if (act.price != null && act.price!! > 0) {
                    canvas.drawText("Price: AU$${String.format(Locale.getDefault(), "%.2f", act.price)}", x, y, detailPaint); y += 12f
                }
                y += 4f
            }
            y += 6f
        }

        // --- INVENTORY SECTION ---
        pdfDocument.finishPage(myPage)
        val resInv = startNewPage(pdfDocument, title, listName)
        myPage = resInv.first; canvas = resInv.second; y = START_Y; column = 1; x = COL1_X
        canvas.drawText("--- INVENTORY ---", PAGE_WIDTH/2, y, sectionPaint); y += 35f
        sectionStartY = y
        val itemsByEntry = data.allItems.groupBy { it.entry_id }
        data.entries.forEach { entry ->
            val r = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
            myPage = r.first; column = r.second; y = r.third; canvas = myPage.canvas
            if (column == 1 && y == START_Y) sectionStartY = START_Y
            x = if (column == 1) COL1_X else COL2_X

            val status = if (entry.is_checked == 1) "[X]" else "[ ]"
            canvas.drawText("$status ${entry.entry_name} (x${entry.quantity})", x, y, Paint().apply { textSize = 11f }); y += 15f
            if (entry.entry_type == "container") {
                itemsByEntry[entry.entry_id]?.forEach { sub ->
                    val rS = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
                    myPage = rS.first; column = rS.second; y = rS.third; canvas = myPage.canvas
                    if (column == 1 && y == START_Y) sectionStartY = START_Y
                    x = if (column == 1) COL1_X else COL2_X
                    val itemStatus = if (sub.is_checked == 1) "[X]" else "[ ]"
                    canvas.drawText("  $itemStatus ${sub.item_name} (x${sub.quantity})", x, y, detailPaint); y += 14f
                }
            }
            y += 4f
        }

        // --- MEAL PLAN SECTION ---
        pdfDocument.finishPage(myPage)
        val resMenu = startNewPage(pdfDocument, title, listName)
        myPage = resMenu.first; canvas = resMenu.second; y = START_Y; column = 1; x = COL1_X
        canvas.drawText("--- MEAL PLAN ---", PAGE_WIDTH/2, y, sectionPaint); y += 35f
        sectionStartY = y
        data.menu.groupBy { it.day }.forEach { (day, meals) ->
            val rDay = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
            myPage = rDay.first; column = rDay.second; y = rDay.third; canvas = myPage.canvas
            if (column == 1 && y == START_Y) sectionStartY = START_Y
            x = if (column == 1) COL1_X else COL2_X

            canvas.drawText(day, x, y, Paint().apply { isFakeBoldText = true; textSize = 13f; color = MAROON }); y += 18f
            meals.forEach { meal ->
                val rM = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
                myPage = rM.first; column = rM.second; y = rM.third; canvas = myPage.canvas
                if (column == 1 && y == START_Y) sectionStartY = START_Y
                x = if (column == 1) COL1_X else COL2_X
                canvas.drawText(meal.meal_type, x, y, activityPaint); y += 14f
                y = drawMultilineText(canvas, meal.description, x, y, detailPaint, COL_WIDTH, 12f)
                y += 6f
            }
            y += 10f
        }

        // --- SHOPPING LIST SECTION ---
        pdfDocument.finishPage(myPage)
        val resIng = startNewPage(pdfDocument, title, listName)
        myPage = resIng.first; canvas = resIng.second; y = START_Y; column = 1; x = COL1_X
        canvas.drawText("--- SHOPPING LIST ---", PAGE_WIDTH/2, y, sectionPaint); y += 35f
        sectionStartY = y
        data.ingredientGroups.forEach { group ->
            val rG = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
            myPage = rG.first; column = rG.second; y = rG.third; canvas = myPage.canvas
            if (column == 1 && y == START_Y) sectionStartY = START_Y
            x = if (column == 1) COL1_X else COL2_X

            canvas.drawText(group.group_name, x, y, Paint().apply { isFakeBoldText = true; textSize = 12f; color = MAROON }); y += 16f
            data.allIngredients.filter { it.group_id == group.id }.forEach { ing ->
                val rI = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
                myPage = rI.first; column = rI.second; y = rI.third; canvas = myPage.canvas
                if (column == 1 && y == START_Y) sectionStartY = START_Y
                x = if (column == 1) COL1_X else COL2_X
                canvas.drawText("• ${ing.ingredient_name}", x, y, detailPaint); y += 14f
            }
            y += 10f
        }

        pdfDocument.finishPage(myPage)
        return savePdf(context, pdfDocument, "FullTrip_${listName.replace(" ", "_")}.pdf")
    }

    fun generateInventoryPdf(context: Context, listName: String, entries: List<Entry>, allItems: Map<Int, List<Item>>): File? {
        val pdfDocument = PdfDocument()
        val boldPaint = Paint().apply { isFakeBoldText = true; textSize = 14f }
        val itemPaint = Paint().apply { textSize = 12f; color = Color.DKGRAY }
        val title = "Inventory"

        var (myPage, canvas) = startNewPage(pdfDocument, title, listName)
        var y = START_Y; var column = 1; var x = COL1_X
        var sectionStartY = START_Y

        entries.forEach { entry ->
            val r = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
            myPage = r.first; column = r.second; y = r.third; canvas = myPage.canvas
            if (column == 1 && y == START_Y) sectionStartY = START_Y
            x = if (column == 1) COL1_X else COL2_X

            val status = if (entry.is_checked == 1) "[X]" else "[ ]"
            canvas.drawText("$status ${entry.entry_name} (x${entry.quantity})", x, y, boldPaint); y += 20f

            if (entry.entry_type == "container") {
                allItems[entry.entry_id]?.forEach { item ->
                    val rI = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
                    myPage = rI.first; column = rI.second; y = rI.third; canvas = myPage.canvas
                    if (column == 1 && y == START_Y) sectionStartY = START_Y
                    x = if (column == 1) COL1_X else COL2_X
                    val itemStatus = if (item.is_checked == 1) "[X]" else "[ ]"
                    canvas.drawText("      $itemStatus ${item.item_name} (x${item.quantity})", x, y, itemPaint); y += 18f
                }
            }
            if (!entry.notes.isNullOrEmpty()) {
                y = drawMultilineText(canvas, "Notes: ${entry.notes}", x, y, itemPaint, COL_WIDTH, 14f)
            }
            y += 8f
        }
        pdfDocument.finishPage(myPage)
        return savePdf(context, pdfDocument, "Inventory_${listName.replace(" ", "_")}.pdf")
    }

    fun generateMenuPdf(context: Context, listName: String, menuItems: List<MenuItem>): File? {
        val pdfDocument = PdfDocument()
        val dayPaint = Paint().apply { isFakeBoldText = true; textSize = 16f; color = MAROON }
        val mealTypePaint = Paint().apply { isFakeBoldText = true; textSize = 13f }
        val descriptionPaint = Paint().apply { textSize = 12f }
        val title = "Meal Plan"

        var (myPage, canvas) = startNewPage(pdfDocument, title, listName)
        var y = START_Y; var column = 1; var x = COL1_X
        var sectionStartY = START_Y

        menuItems.groupBy { it.day }.forEach { (day, items) ->
            val rDay = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
            myPage = rDay.first; column = rDay.second; y = rDay.third; canvas = myPage.canvas
            if (column == 1 && y == START_Y) sectionStartY = START_Y
            x = if (column == 1) COL1_X else COL2_X

            canvas.drawText(day, x, y, dayPaint); y += 25f
            items.forEach { item ->
                val r = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
                myPage = r.first; column = r.second; y = r.third; canvas = myPage.canvas
                if (column == 1 && y == START_Y) sectionStartY = START_Y
                x = if (column == 1) COL1_X else COL2_X
                canvas.drawText(item.meal_type, x, y, mealTypePaint); y += 18f
                y = drawMultilineText(canvas, item.description, x, y, descriptionPaint, COL_WIDTH, 15f)
                y += 10f
            }
            y += 5f
        }
        pdfDocument.finishPage(myPage)
        return savePdf(context, pdfDocument, "Menu_${listName.replace(" ", "_")}.pdf")
    }

    fun generateIngredientsPdf(context: Context, listName: String, groups: List<IngredientGroup>, allIngredients: Map<Int, List<Ingredient>>): File? {
        val pdfDocument = PdfDocument()
        val groupPaint = Paint().apply { isFakeBoldText = true; textSize = 15f; color = MAROON }
        val textPaint = Paint().apply { textSize = 13f }
        val title = "Shopping List"

        var (myPage, canvas) = startNewPage(pdfDocument, title, listName)
        var y = START_Y; var column = 1; var x = COL1_X
        var sectionStartY = START_Y

        groups.forEach { group ->
            val rG = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
            myPage = rG.first; column = rG.second; y = rG.third; canvas = myPage.canvas
            if (column == 1 && y == START_Y) sectionStartY = START_Y
            x = if (column == 1) COL1_X else COL2_X

            canvas.drawText(group.group_name, x, y, groupPaint); y += 25f
            allIngredients[group.id]?.forEach { ing ->
                val rI = checkLayout(pdfDocument, myPage, y, column, title, listName, sectionStartY)
                myPage = rI.first; column = rI.second; y = rI.third; canvas = myPage.canvas
                if (column == 1 && y == START_Y) sectionStartY = START_Y
                x = if (column == 1) COL1_X else COL2_X
                canvas.drawText("• ${ing.ingredient_name}", x, y, textPaint); y += 18f
            }
            y += 10f
        }
        pdfDocument.finishPage(myPage)
        return savePdf(context, pdfDocument, "Shopping_List_${listName.replace(" ", "_")}.pdf")
    }

    private fun savePdf(context: Context, pdfDocument: PdfDocument, fileName: String): File? {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)
        return try {
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun printFile(context: Context, file: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${context.packageName} ${file.name}"
        
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onWrite(pages: Array<PageRange>, destination: ParcelFileDescriptor, 
                                 cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
                try {
                    val input = FileInputStream(file)
                    val output = FileOutputStream(destination.fileDescriptor)
                    input.copyTo(output)
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
            override fun onLayout(oldAttributes: PrintAttributes, newAttributes: PrintAttributes, 
                                  cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle) {
                if (cancellationSignal.isCanceled) {
                    callback.onLayoutCancelled()
                    return
                }
                val pbi = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(pbi, true)
            }
        }
        printManager.print(jobName, printAdapter, null)
    }
}

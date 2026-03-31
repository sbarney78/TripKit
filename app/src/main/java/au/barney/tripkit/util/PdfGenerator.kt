package au.barney.tripkit.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import au.barney.tripkit.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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

    private fun getSortedDisplayItems(itinerary: List<ItineraryItem>): List<DisplayItineraryItem> {
        val sdfDate = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return itinerary.flatMap { item ->
            val list = mutableListOf<DisplayItineraryItem>()
            list.add(DisplayItineraryItem(item.day, item.time, item.activity, item.location, item.price, item.notes, item, false))
            if (!item.departure_day.isNullOrBlank()) {
                list.add(DisplayItineraryItem(item.departure_day!!, item.departure_time ?: "", "Departure: ${item.activity}", item.location, null, null, item, true))
            }
            list
        }.sortedWith { a, b ->
            val dateA = try { sdfDate.parse(a.day) } catch (e: Exception) { null }
            val dateB = try { sdfDate.parse(b.day) } catch (e: Exception) { null }
            if (dateA != null && dateB != null) {
                val dateComparison = dateA.compareTo(dateB)
                if (dateComparison != 0) return@sortedWith dateComparison
            }
            val timeA = try { sdfTime.parse(a.time) } catch (e: Exception) { null }
            val timeB = try { sdfTime.parse(b.time) } catch (e: Exception) { null }
            if (timeA != null && timeB != null) timeA.compareTo(timeB) else a.time.compareTo(b.time)
        }
    }

    fun generateItineraryPdf(context: Context, listName: String, items: List<ItineraryItem>) {
        val pdfDocument = PdfDocument()
        val dayPaint = Paint().apply { isFakeBoldText = true; textSize = 18f; color = MAROON }
        val timePaint = Paint().apply { isFakeBoldText = true; textSize = 14f }
        val activityPaint = Paint().apply { textSize = 14f }
        val detailPaint = Paint().apply { textSize = 12f; color = Color.DKGRAY }

        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var myPage = pdfDocument.startPage(pageInfo)
        var canvas = myPage.canvas
        drawHeader(canvas, "Itinerary", listName)
        var y = 120f

        getSortedDisplayItems(items).groupBy { it.day }.forEach { (day, activities) ->
            val res = checkNewPage(pdfDocument, myPage, y, "Itinerary", listName)
            myPage = res.first; canvas = res.second; y = res.third

            canvas.drawText(day, 40f, y, dayPaint)
            y += 35f

            activities.forEach { act ->
                val r2 = checkNewPage(pdfDocument, myPage, y, "Itinerary", listName)
                myPage = r2.first; canvas = r2.second; y = r2.third

                canvas.drawText("${act.time}: ${act.activity}", 40f, y, timePaint)
                y += 20f
                
                if (!act.location.isNullOrBlank()) {
                    canvas.drawText("Address: ${act.location!!}", 60f, y, detailPaint)
                    y += 20f
                }
                
                if (act.price != null && act.price!! > 0) {
                    canvas.drawText("Cost: AU$${String.format(Locale.getDefault(), "%.2f", act.price)}", 60f, y, detailPaint)
                    y += 20f
                }

                if (!act.notes.isNullOrBlank()) {
                    y = drawMultilineText(canvas, act.notes!!, 60f, y, activityPaint)
                }
                y += 15f
            }
            y += 10f
        }

        pdfDocument.finishPage(myPage)
        saveAndSharePdf(context, pdfDocument, "Itinerary_${listName.replace(" ", "_")}.pdf")
    }

    fun generateFullTripPdf(
        context: Context,
        data: FullTripData
    ) {
        val listName = data.list.name
        val itinerary = data.itinerary
        val entries = data.entries
        val allItems = data.allItems.groupBy { it.entry_id }
        val menu = data.menu
        val ingredientGroups = data.ingredientGroups
        val ingredients = data.allIngredients.groupBy { it.group_id }

        val pdfDocument = PdfDocument()
        val sectionPaint = Paint().apply { isFakeBoldText = true; textSize = 20f; color = MAROON; textAlign = Paint.Align.CENTER }
        val textPaint = Paint().apply { textSize = 14f }
        val boldPaint = Paint().apply { isFakeBoldText = true; textSize = 14f }
        val detailPaint = Paint().apply { textSize = 12f; color = Color.DKGRAY }

        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var myPage = pdfDocument.startPage(pageInfo)
        var canvas = myPage.canvas
        var y = 120f
        val title = "Full Trip Plan"

        drawHeader(canvas, title, listName)

        // 1. ITINERARY
        canvas.drawText("--- ITINERARY ---", PAGE_WIDTH/2, y, sectionPaint)
        y += 45f
        getSortedDisplayItems(itinerary).groupBy { it.day }.forEach { (day, activities) ->
            val r = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r.first; canvas = r.second; y = r.third
            canvas.drawText(day, 40f, y, boldPaint); y += 25f
            activities.forEach { act ->
                val r2 = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r2.first; canvas = r2.second; y = r2.third
                canvas.drawText("${act.time}: ${act.activity}", 50f, y, textPaint); y += 20f
                if (!act.location.isNullOrBlank()) {
                    canvas.drawText("Address: ${act.location!!}", 65f, y, detailPaint); y += 18f
                }
                if (!act.notes.isNullOrBlank()) {
                    y = drawMultilineText(canvas, act.notes!!, 65f, y, detailPaint, 16f)
                }
            }
            y += 10f
        }

        // 2. INVENTORY
        y += 30f
        val r3 = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r3.first; canvas = r3.second; y = r3.third
        canvas.drawText("--- INVENTORY ---", PAGE_WIDTH/2, y, sectionPaint); y += 45f
        val itemsByEntry = allItems
        entries.forEach { entry ->
            val r = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r.first; canvas = r.second; y = r.third
            val status = if (entry.is_checked == 1) "[X]" else "[ ]"
            canvas.drawText("$status ${entry.entry_name} (Qty: ${entry.quantity})", 40f, y, textPaint); y += 20f
            if (entry.entry_type == "container") {
                itemsByEntry[entry.entry_id]?.forEach { sub ->
                    val rSub = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = rSub.first; canvas = rSub.second; y = rSub.third
                    canvas.drawText("  - ${sub.item_name}", 60f, y, detailPaint); y += 18f
                }
            }
            if (!entry.notes.isNullOrEmpty()) {
                y = drawMultilineText(canvas, "   Notes: ${entry.notes}", 45f, y, detailPaint, 16f)
            }
        }

        // 3. MENU
        y += 30f
        val r4 = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r4.first; canvas = r4.second; y = r4.third
        canvas.drawText("--- MEAL PLAN ---", PAGE_WIDTH/2, y, sectionPaint); y += 45f
        menu.groupBy { it.day }.forEach { (day, items) ->
            val r = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r.first; canvas = r.second; y = r.third
            canvas.drawText(day, 40f, y, boldPaint); y += 25f
            items.forEach { meal ->
                val r2 = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r2.first; canvas = r2.second; y = r2.third
                canvas.drawText(meal.meal_type, 50f, y, boldPaint); y += 20f
                y = drawMultilineText(canvas, meal.description, 50f, y, textPaint); y += 10f
            }
        }

        // 4. SHOPPING LIST
        y += 30f
        val r5 = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r5.first; canvas = r5.second; y = r5.third
        canvas.drawText("--- SHOPPING LIST ---", PAGE_WIDTH/2, y, sectionPaint); y += 45f
        val ingByGroup = ingredients
        ingredientGroups.forEach { group ->
            val r = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r.first; canvas = r.second; y = r.third
            canvas.drawText(group.group_name, 40f, y, boldPaint); y += 25f
            ingByGroup[group.id]?.forEach { ing ->
                val r2 = checkNewPage(pdfDocument, myPage, y, title, listName); myPage = r2.first; canvas = r2.second; y = r2.third
                canvas.drawText("• ${ing.ingredient_name}", 50f, y, textPaint); y += 20f
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

data class DisplayItineraryItem(
    val day: String,
    val time: String,
    val activity: String,
    val location: String?,
    val price: Double?,
    val notes: String?,
    val original: ItineraryItem,
    val isDeparture: Boolean
)

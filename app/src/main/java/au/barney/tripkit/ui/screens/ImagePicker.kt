package au.barney.tripkit.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.util.*

@Composable
fun ImagePicker(
    currentImagePath: String?,
    onImageSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    var showOptions by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageSelected(tempFile?.absolutePath)
        }
        showOptions = false
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = saveImageToInternalStorage(context, it)
            onImageSelected(path)
        }
        showOptions = false
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (currentImagePath != null) {
            Box {
                AsyncImage(
                    model = currentImagePath,
                    contentDescription = "Item Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullScreen = true },
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onImageSelected(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            OutlinedButton(
                onClick = { showOptions = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Photo")
            }
        }

        if (showOptions) {
            AlertDialog(
                onDismissRequest = { showOptions = false },
                title = { Text("Add Photo") },
                text = { Text("Choose a source for the photo") },
                confirmButton = {
                    TextButton(onClick = {
                        val file = createTempImageFile(context)
                        tempFile = file
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        cameraLauncher.launch(uri)
                    }) { Text("Camera") }
                },
                dismissButton = {
                    TextButton(onClick = { galleryLauncher.launch("image/*") }) { Text("Gallery") }
                }
            )
        }

        if (showFullScreen && currentImagePath != null) {
            FullScreenImageDialog(currentImagePath) { showFullScreen = false }
        }
    }
}

private var tempFile: File? = null

private fun createTempImageFile(context: Context): File {
    val storageDir = context.getExternalFilesDir(null)
    return File.createTempFile("TEMP_IMG_${System.currentTimeMillis()}", ".jpg", storageDir)
}

private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val file = File(context.filesDir, "IMG_${UUID.randomUUID()}.jpg")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Composable
fun FullScreenImageDialog(imagePath: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 3f
                            offset = Offset.Zero
                        },
                        onTap = { onDismiss() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = maxOf(1f, scale),
                        scaleY = maxOf(1f, scale),
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state),
                contentScale = ContentScale.Fit
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

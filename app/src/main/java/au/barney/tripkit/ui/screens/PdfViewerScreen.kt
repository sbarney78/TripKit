package au.barney.tripkit.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.pdf.PdfDocument
import androidx.pdf.viewer.fragment.PdfViewerFragment
import au.barney.tripkit.util.PdfGenerator
import java.io.File

/**
 * A custom PdfViewerFragment that hides the toolbox (which contains the annotation button).
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class NoToolboxPdfViewerFragment : PdfViewerFragment() {
    override fun onLoadDocumentSuccess(document: PdfDocument) {
        super.onLoadDocumentSuccess(document)
        isToolboxVisible = false
    }

    override fun onRequestImmersiveMode(enterImmersive: Boolean) {
        super.onRequestImmersiveMode(enterImmersive)
        isToolboxVisible = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val file = File(filePath)
    val fragmentActivity = context as? FragmentActivity
    val fragmentManager = fragmentActivity?.supportFragmentManager
    
    // We use a unique ID for the container to avoid conflicts
    val containerId = remember { View.generateViewId() }

    // Launcher for picking a save location (Storage Access Framework)
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { targetUri ->
            try {
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "File saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Clean up the fragment when this composable leaves the composition
    DisposableEffect(containerId) {
        onDispose {
            fragmentManager?.findFragmentById(containerId)?.let { fragment ->
                try {
                    if (!fragmentManager.isStateSaved) {
                        fragmentManager.beginTransaction()
                            .remove(fragment)
                            .commitAllowingStateLoss()
                    }
                } catch (_: Exception) {
                    // Best effort removal
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    // 1. Share/Export (Best for Cloud, Drive, and "Save to Files" apps)
                    IconButton(onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Save or Share PDF"))
                        } catch (_: Exception) {}
                    }) {
                        Icon(
                            Icons.Default.Share, 
                            contentDescription = "Share or Save to Cloud",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // 2. Print (Best for "Save as PDF" which often has a better folder picker)
                    IconButton(onClick = {
                        PdfGenerator.printFile(context, file)
                    }) {
                        Icon(
                            Icons.Default.Print, 
                            contentDescription = "Print or Save as PDF",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // 3. Direct Download/Save (Uses System File Picker)
                    IconButton(onClick = {
                        // Launching SAF with the file name
                        saveLauncher.launch(file.name)
                    }) {
                        Icon(
                            Icons.Default.FileDownload, 
                            contentDescription = "Download to device",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (fragmentActivity != null && fragmentManager != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                // PdfViewerFragment requires SDK Extension 13
                val isSupported = remember {
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13
                }

                if (isSupported) {
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                id = containerId
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { _ ->
                            val existingFragment = fragmentManager.findFragmentById(containerId) as? PdfViewerFragment
                            val uri = Uri.fromFile(file)
                            
                            if (existingFragment == null) {
                                // Use the custom fragment that hides the annotation toolbox
                                val pdfViewerFragment = NoToolboxPdfViewerFragment()
                                
                                fragmentManager.beginTransaction()
                                    .replace(containerId, pdfViewerFragment)
                                    .commitAllowingStateLoss()
                                
                                fragmentManager.executePendingTransactions()
                                pdfViewerFragment.documentUri = uri
                            } else {
                                if (existingFragment.isAdded && existingFragment.documentUri != uri) {
                                    existingFragment.documentUri = uri
                                }
                            }
                        }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "PDF viewing requires a system update (SDK Extension 13).",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Error: Activity must be a FragmentActivity")
            }
        }
    }
}

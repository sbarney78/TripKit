package au.barney.tripkit.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PdfViewerScreen(
    url: String,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box {

        // FULL SCREEN WEBVIEW
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {

                    // Enable zoom + gestures
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)

                    // Disable shrinking
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = true

                    // JS + storage
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    settings.cacheMode = WebSettings.LOAD_DEFAULT

                    webViewClient = WebViewClient()

                    loadUrl(url)

                    webViewRef = this
                }
            },
            update = { it.loadUrl(url) }
        )

        // FLOATING PRINT BUTTON
        FloatingActionButton(
            onClick = {
                webViewRef?.let { wv ->
                    printWebView(context, wv)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Text("Print")
        }
    }
}

// ANDROID PRINT SYSTEM
fun printWebView(context: Context, webView: WebView) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "TripKit Menu PDF"

    val printAdapter = webView.createPrintDocumentAdapter(jobName)

    printManager.print(
        jobName,
        printAdapter,
        PrintAttributes.Builder().build()
    )
}

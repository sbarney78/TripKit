package au.barney.tripkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import au.barney.tripkit.navigation.AppNavGraph
import au.barney.tripkit.ui.theme.TripKitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the system splash screen transition
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Optional: Keep the system splash screen on-screen until your app is ready
        // For now, we'll let it transition immediately to your custom animated splash

        setContent {
            TripKitTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }
}

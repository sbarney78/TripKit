package au.barney.tripkit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import au.barney.tripkit.navigation.AppNavGraph
import au.barney.tripkit.ui.theme.TripKitTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the system splash screen transition
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        setContent {
            TripKitTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }
}

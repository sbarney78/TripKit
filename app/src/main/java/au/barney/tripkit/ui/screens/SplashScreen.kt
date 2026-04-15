package au.barney.tripkit.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.barney.tripkit.R
import au.barney.tripkit.ui.theme.TripKitTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0.9f) }
    val alpha = remember { Animatable(0f) }

    val versionName = "V3.5"

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(900, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(1f, tween(1200))
        delay(2000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // Full‑width splash image
        Image(
            painter = painterResource(id = R.drawable.ic_tripkit_splash_gold),
            contentDescription = "TripKit Splash",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .scale(scale.value)
                .alpha(alpha.value),
            contentScale = ContentScale.FillWidth
        )

        // Version text positioned ~1/4 up from bottom, italic, 26sp
        Text(
            text = versionName,
            fontSize = 22.sp,
            fontStyle = FontStyle.Italic,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .alpha(alpha.value)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    TripKitTheme {
        Surface {
            SplashScreen(onTimeout = {})
        }
    }
}

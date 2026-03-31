package au.barney.tripkit.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.barney.tripkit.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(-15f) }

    // Continuous pulse for the background circle
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        // Initial "Pop" entrance with rotation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(1f, tween(1000))
        rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        
        delay(2000)
        onTimeout()
    }

    // Dynamic Gradient Background
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Container with pulsing shadow effect
            Box(contentAlignment = Alignment.Center) {
                // Outer Pulse
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                )
                
                // Main Icon Circle
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale.value)
                        .rotate(rotation.value)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_tripkit_logo),
                        contentDescription = "TripKit Logo",
                        modifier = Modifier.size(100.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Branding with fade-in and slide-up feel
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(alpha.value)
                    .offset(y = (20 * (1 - scale.value)).dp)
            ) {
                Text(
                    text = "TripKit",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "MASTER YOUR JOURNEY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 5.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = "Your Weekend Away Starts Here",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
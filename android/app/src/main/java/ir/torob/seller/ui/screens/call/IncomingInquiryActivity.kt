package ir.torob.seller.ui.screens.call

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.MainActivity
import ir.torob.seller.service.InquiryAlertService
import ir.torob.seller.ui.theme.*
import ir.torob.seller.util.InquiryRingtoneManager

import androidx.compose.ui.text.TextStyle
import ir.torob.seller.data.repository.InquiryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingInquiryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and turn on over lockscreen
        turnScreenOnAndDismissKeyguard()

        val inquiryId = intent.getStringExtra("inquiry_id") ?: "inq_01"

        setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalTextStyle provides TextStyle(fontFamily = VazirFontFamily)
            ) {
                IncomingCallScreen(
                    inquiryId = inquiryId,
                    onAnswer = {
                        stopRingingAndAlert()
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            data = Uri.parse("torob://inquiry/$inquiryId")
                            putExtra("inquiry_id", inquiryId)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onDecline = {
                        stopRingingAndAlert()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                InquiryRepository(applicationContext).submitResponse(
                                    inquiryId = inquiryId,
                                    priceToman = 0L,
                                    inStock = false,
                                    notes = "اعلام عدم موجودی از صفحه تماس",
                                    stockCount = 0
                                )
                            } catch (e: Exception) {
                                // Handled via offline sync queue
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }

    private fun turnScreenOnAndDismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun stopRingingAndAlert() {
        InquiryRingtoneManager.stopRinging(applicationContext)
        try {
            val stopIntent = Intent(this, InquiryAlertService::class.java).apply {
                action = InquiryAlertService.ACTION_STOP_ALERT
            }
            startService(stopIntent)
        } catch (e: Exception) {
            // Ignored
        }
        try {
            stopService(Intent(this, InquiryAlertService::class.java))
        } catch (e: Exception) {
            // Ignored
        }
    }

    override fun onBackPressed() {
        stopRingingAndAlert()
        super.onBackPressed()
    }

    override fun onDestroy() {
        stopRingingAndAlert()
        super.onDestroy()
    }
}

@Composable
fun IncomingCallScreen(
    inquiryId: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    // Pulsing animation for call ring effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val outerPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B), // Slate 800
                        Color(0xFF0B1120)  // Deep Midnight
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Minimal Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TorobRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TorobRed.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TorobRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "استعلام خریدار در ترب",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "تماس ورودی استعلام",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }

            // Center: Pulsing Ring Animation (Clean & Minimal, No Card)
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outermost subtle halo ring
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .scale(outerPulseScale)
                        .clip(CircleShape)
                        .background(TorobRed.copy(alpha = 0.08f))
                )

                // Middle pulsing ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(TorobRed.copy(alpha = 0.22f))
                )

                // Inner core button with call icon
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(TorobRed, TorobDarkRed)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Bottom Section: Professional Answer & Decline Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Red "عدم موجودی" Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onDecline,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = TorobRed),
                        modifier = Modifier.size(72.dp),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "عدم موجودی",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "عدم موجودی",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Green "بررسی استعلام" Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onAnswer,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = TorobGreen),
                        modifier = Modifier.size(72.dp),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "بررسی استعلام",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "بررسی استعلام",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

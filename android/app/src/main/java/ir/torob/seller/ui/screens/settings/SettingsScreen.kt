package ir.torob.seller.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.VolumeOff
import ir.torob.seller.ui.components.NotificationPermissionBanner
import ir.torob.seller.ui.theme.*
import ir.torob.seller.ui.viewmodel.AuthViewModel
import ir.torob.seller.ui.viewmodel.SettingsViewModel
import ir.torob.seller.util.InquiryRingtoneManager
import ir.torob.seller.util.NotificationHelper

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onLogout: () -> Unit
) {
    val state by settingsViewModel.uiState.collectAsState()
    var serverUrlInput by remember(state.serverBaseUrl) { mutableStateOf(state.serverBaseUrl) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhite,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تنظیمات برنامه",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Text(
                    text = "مدیریت اعلان‌ها، اتصال به سرور و حساب کاربری",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasNotificationPermission) {
                NotificationPermissionBanner(onEnableClicked = onRequestNotificationPermission)
            }

            // Notification preferences card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "اعلان‌ها و پوش‌نوتیفیکیشن",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "دریافت نوتیفیکیشن استعلام جدید",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = "ارسال پوش‌نوتیفیکیشن در ثانیه صفر و یادآوری دقیقه ۵",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }

                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = settingsViewModel::toggleNotifications,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = TorobGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test normal notification
                    Button(
                        onClick = {
                            NotificationHelper.showInquiryNotification(
                                context = context,
                                inquiryId = "inq_01",
                                title = "استعلام جدید در ترب",
                                body = "خریدار محمد رضایی منتظر استعلام قیمت گوشی آیفون ۱۵ است.",
                                notificationType = "INITIAL_INQUIRY"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TorobGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ارسال اعلان تستی به گوشی",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Full-screen incoming call test button
                    Button(
                        onClick = {
                            val serviceIntent = Intent(context, ir.torob.seller.service.InquiryAlertService::class.java).apply {
                                action = ir.torob.seller.service.InquiryAlertService.ACTION_START_ALERT
                                putExtra("inquiry_id", "inq_01")
                                putExtra("title", "استعلام فوری در ترب")
                                putExtra("body", "خریدار منتظر استعلام قیمت گوشی آیفون ۱۵ پرو است.")
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TorobRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "شبیه‌سازی تماس ورودی تمام‌صفحه (Full-Screen)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Test Ringtone / Alarm Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                InquiryRingtoneManager.startRinging(context, maxDurationSec = 30)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RingVolume,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تست زنگ استعلام",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                InquiryRingtoneManager.stopRinging(context)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    tint = Slate700,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "قطع زنگ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // MIUI / Background Settings button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تنظیمات پس‌زمینه و شروع خودکار (شیائومی)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        }
                    }

                    if (!state.fcmToken.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "توکن پوش متصل به دستگاه: ${state.fcmToken?.take(16)}...",
                            fontSize = 10.sp,
                            color = Slate500,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Server connection configuration card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "آدرس اتصال به سرور بک‌اند",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "برای شبیه‌ساز اندروید مقدار 10.0.2.2 و برای گوشی واقعی آی‌پی شبکه محلی سیستم خود را وارد نمایید.",
                        fontSize = 11.sp,
                        color = Slate500,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { settingsViewModel.updateServerUrl(serverUrlInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "بروزرسانی آدرس سرور", fontSize = 12.sp, color = PureWhite)
                    }

                    if (state.message != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message!!,
                            color = TorobGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Logout
            Button(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TorobLightRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "خروج از حساب کاربری فروشنده",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TorobRed
                )
            }
        }
    }
}

package ir.torob.seller.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhite,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "تنظیمات برنامه",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "مدیریت اعلان‌ها، اتصال به سرور و حساب کاربری",
                    fontSize = 11.sp,
                    color = TextSecondary
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

            // Section 1: Notifications & Instant Alerts
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        icon = Icons.Default.Notifications,
                        title = "اعلان‌ها و یادآوری فوری",
                        iconColor = PrimaryBlue,
                        iconBg = PrimaryBlueLight
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "اعلان صوتی در ثانیه صفر و یادآوری مجدد در دقیقه ۵",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = settingsViewModel::toggleNotifications,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = SuccessGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ابزارهای تست و اعتبارسنجی زنگ گوشی:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Test Normal Push Notification
                    FilledTonalButton(
                        onClick = {
                            NotificationHelper.showInquiryNotification(
                                context = context,
                                inquiryId = "inq_test",
                                title = "استعلام جدید در ترب",
                                body = "خریدار منتظر استعلام قیمت گوشی آیفون ۱۵ پرو است.",
                                notificationType = "INITIAL_INQUIRY"
                            )
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Slate100,
                            contentColor = Slate800
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ارسال اعلان نوتیفیکیشن تستی",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Full-screen incoming call test
                    Button(
                        onClick = {
                            val serviceIntent = Intent(context, ir.torob.seller.service.InquiryAlertService::class.java).apply {
                                action = ir.torob.seller.service.InquiryAlertService.ACTION_START_ALERT
                                putExtra("inquiry_id", "inq_test")
                                putExtra("title", "استعلام فوری در ترب")
                                putExtra("body", "خریدار منتظر استعلام قیمت کالا است.")
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
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

                    // Ringtone Test Section
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
                            border = BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
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

                    // Battery / Xiaomi autostart
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        border = BorderStroke(1.dp, BorderSubtle),
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
                                text = "تنظیمات پس‌زمینه و مجوز شروع خودکار (Autostart)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        }
                    }

                    if (!state.fcmToken.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "توکن پوش: ${state.fcmToken?.take(20)}...",
                            fontSize = 10.sp,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Section 2: Server Connection Configuration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        icon = Icons.Default.Dns,
                        title = "آدرس اتصال به سرور بک‌اند",
                        iconColor = Slate700,
                        iconBg = Slate100
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "برای شبیه‌ساز 10.0.2.2 و برای گوشی فیزیکی آی‌پی سیستم خود را وارد نمایید.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Base URL", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { settingsViewModel.updateServerUrl(serverUrlInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "بروزرسانی آدرس سرور", fontSize = 12.sp, color = PureWhite)
                    }

                    if (state.message != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message!!,
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Section 3: App Info & Version
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        icon = Icons.Default.Info,
                        title = "درباره اپلیکیشن فروشندگان",
                        iconColor = Slate700,
                        iconBg = Slate100
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "نسخه نرم‌افزار", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "1.0.0 (Build 2026)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "فناوری رابط کاربری", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "Jetpack Compose • Material 3", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }
            }

            // Section 4: Safe Logout Button
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ErrorRed
                ),
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "خروج از حساب کاربری فروشنده",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
            }
        }
    }

    // Safe Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "خروج از حساب کاربری",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "آیا اطمینان دارید که می‌خواهید از حساب کاربری خود خارج شوید؟ برای دریافت مجدد استعلام‌ها باید دوباره وارد شوید.",
                    fontSize = 13.sp,
                    color = Slate700,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("بله، خروج", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("انصراف", color = Slate700)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    iconBg: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(iconBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

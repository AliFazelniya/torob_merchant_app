package ir.torob.seller.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.data.model.ShopUpdateDto
import ir.torob.seller.ui.theme.*
import ir.torob.seller.ui.viewmodel.StoreViewModel

@Composable
fun StoreProfileScreen(
    viewModel: StoreViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val shop = state.shop

    var shopName by remember(shop) { mutableStateOf(shop?.shopName ?: "") }
    var address by remember(shop) { mutableStateOf(shop?.address ?: "") }
    var phone by remember(shop) { mutableStateOf(shop?.phone ?: "") }
    var openTime by remember(shop) { mutableStateOf(shop?.openTime ?: "09:00") }
    var closeTime by remember(shop) { mutableStateOf(shop?.closeTime ?: "21:00") }

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
                    text = "اطلاعات و وضعیت فروشگاه",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Text(
                    text = "تنظیم باز/بسته بودن فروشگاه و ساعات کاری",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }

        if (state.isLoading && shop == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TorobRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Store Operating Scenario Selector Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "وضعیت فعالیت فروشگاه در ترب",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "یکی از سناریوهای زیر را برای وضعیت فروشگاه و دریافت استعلام انتخاب کنید:",
                            fontSize = 11.sp,
                            color = Slate500
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val currentScenario = when {
                            shop?.isOpen == true && shop.acceptInquiries -> 1
                            shop?.isOpen == true && !shop.acceptInquiries -> 2
                            else -> 3
                        }

                        // Scenario 1: فروشگاه فعال و پشتیبانی استعلام دردسترس
                        ScenarioOptionCard(
                            title = "فروشگاه فعال و پشتیبانی استعلام دردسترس",
                            description = "فروشگاه باز است و استعلام‌های فوری خریداران همراه با زنگ تماس ارسال می‌شود.",
                            isSelected = currentScenario == 1,
                            accentColor = TorobGreen,
                            onClick = {
                                viewModel.updateProfile(ShopUpdateDto(isOpen = true, acceptInquiries = true))
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scenario 2: فروشگاه فعال اما پشتیبانی استعلام غیرفعال
                        ScenarioOptionCard(
                            title = "فروشگاه فعال اما پشتیبانی استعلام غیرفعال",
                            description = "فروشگاه در ترب باز است اما استعلام فوری دریافت نمی‌شود.",
                            isSelected = currentScenario == 2,
                            accentColor = TorobAmber,
                            onClick = {
                                viewModel.updateProfile(ShopUpdateDto(isOpen = true, acceptInquiries = false))
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scenario 3: فروشگاه غیرفعال و بسته
                        ScenarioOptionCard(
                            title = "فروشگاه غیرفعال و بسته",
                            description = "فروشگاه کاملاً غیرفعال است و هیچ استعلامی ارسال نخواهد شد.",
                            isSelected = currentScenario == 3,
                            accentColor = TorobRed,
                            onClick = {
                                viewModel.updateProfile(ShopUpdateDto(isOpen = false, acceptInquiries = false))
                            }
                        )
                    }
                }

                // Outside Working Hours Banner
                if (shop != null && !shop.isWithinWorkingHours) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TorobLightAmber),
                        border = BorderStroke(1.dp, TorobAmber.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = TorobAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "هم‌اکنون در ساعات غیرکاری هستید",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ساعت کاری فروشگاه: ${shop.openTime} الی ${shop.closeTime}. در ساعات غیرکاری، استعلام جدید ارسال نمی‌شود و وضعیت فروشگاه بسته خواهد بود.",
                                    fontSize = 11.sp,
                                    color = Slate700,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Error Message Alert
                if (state.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TorobLightRed),
                        border = BorderStroke(1.dp, TorobRed.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = TorobRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.errorMessage!!,
                                color = TorobRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Success Message Alert
                if (state.successMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TorobLightGreen),
                        border = BorderStroke(1.dp, TorobGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = TorobGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.successMessage!!,
                                color = TorobGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Edit Store Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "مشخصات عمومی فروشگاه",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("نام فروشگاه") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("تلفن تماس فروشگاه") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("آدرس دقیق فروشگاه در تهران") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = openTime,
                                onValueChange = { openTime = it },
                                label = { Text("ساعت بازگشایی (HH:MM)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = closeTime,
                                onValueChange = { closeTime = it },
                                label = { Text("ساعت پایان کار (HH:MM)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    ShopUpdateDto(
                                        shopName = shopName,
                                        address = address,
                                        phone = phone,
                                        openTime = openTime,
                                        closeTime = closeTime
                                    )
                                )
                            },
                            enabled = !state.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TorobRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "ذخیره تغییرات فروشگاه",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.08f) else Slate100.copy(alpha = 0.6f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else Slate200
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = Slate500
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Slate900 else Slate700
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Slate500,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

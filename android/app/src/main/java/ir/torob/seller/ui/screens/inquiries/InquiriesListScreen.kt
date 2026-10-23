package ir.torob.seller.ui.screens.inquiries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.data.local.InquiryEntity
import ir.torob.seller.ui.components.ConnectionStatusBar
import ir.torob.seller.ui.components.NotificationPermissionBanner
import ir.torob.seller.ui.theme.*
import ir.torob.seller.ui.viewmodel.InquiryViewModel
import ir.torob.seller.util.PersianFormatters

@Composable
fun InquiriesListScreen(
    viewModel: InquiryViewModel,
    isOnline: Boolean,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onInquiryClick: (String) -> Unit
) {
    val activeInquiries by viewModel.activeInquiries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // Connection & Offline State Banner
        ConnectionStatusBar(isOnline = isOnline, pendingSyncCount = pendingSyncCount)

        // Top App Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhite,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "استعلام‌های فعال و جدید",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                        if (activeInquiries.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TorobLightRed
                            ) {
                                Text(
                                    text = "${PersianFormatters.toPersianDigits(activeInquiries.size)} فعال",
                                    color = TorobRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "پاسخ سریع به قیمت و موجودی (مهلت پاسخ‌گویی ۱۰ دقیقه)",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                Button(
                    onClick = viewModel::refresh,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate100),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "بروزرسانی",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                }
            }
        }

        // Notification permission guidance if disabled
        if (!hasNotificationPermission) {
            Box(modifier = Modifier.padding(16.dp)) {
                NotificationPermissionBanner(onEnableClicked = onRequestNotificationPermission)
            }
        }

        if (errorMessage != null && activeInquiries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = Slate500,
                    fontSize = 13.sp
                )
            }
        } else if (activeInquiries.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "هیچ استعلام فعالی در انتظار پاسخ وجود ندارد.",
                        color = Slate700,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "استعلام‌های جدید مشتریان به محض ارسال در اینجا ظاهر می‌شوند.\nاستعلام‌های پاسخ‌داده‌شده و منقضی‌شده به بخش «تاریخچه» منتقل شده‌اند.",
                        color = Slate500,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeInquiries, key = { it.inquiryId }) { inq ->
                    InquiryCard(inquiry = inq, onClick = { onInquiryClick(inq.inquiryId) })
                }
            }
        }
    }
}

@Composable
private fun FilterTabItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) TorobRed else Slate100
    val textColor = if (isSelected) PureWhite else Slate700

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InquiryCard(
    inquiry: InquiryEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Tracking Code & Status / Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inquiry.trackingCode,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )

                if (inquiry.status == "ACTIVE") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TorobLightAmber)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "فرصت پاسخ: ${PersianFormatters.formatRemainingTime(inquiry.remainingSeconds)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TorobAmber
                        )
                    }
                } else if (inquiry.status == "ANSWERED") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TorobLightGreen)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "پاسخ داده شد",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TorobGreen
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TorobLightRed)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "منقضی شده",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TorobRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Title
            Text(
                text = inquiry.productTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Price & Customer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قیمت مرجع: ${PersianFormatters.formatPriceToman(inquiry.recordedPriceToman)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TorobRed
                )

                Text(
                    text = "مشتری: ${inquiry.customerName}",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }

            if (!inquiry.userNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "«${inquiry.userNote}»",
                    fontSize = 11.sp,
                    color = Slate700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

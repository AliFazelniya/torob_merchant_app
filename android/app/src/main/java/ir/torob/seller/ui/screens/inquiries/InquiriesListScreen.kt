package ir.torob.seller.ui.screens.inquiries

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import ir.torob.seller.ui.components.*
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

    var filterType by remember { mutableStateOf("ALL") }

    val urgentCount = remember(activeInquiries) {
        activeInquiries.count { it.remainingSeconds in 1..180 }
    }

    val filteredInquiries = remember(activeInquiries, filterType) {
        when (filterType) {
            "URGENT" -> activeInquiries.filter { it.remainingSeconds in 1..180 }
            "NORMAL" -> activeInquiries.filter { it.remainingSeconds !in 1..180 }
            else -> activeInquiries
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Connection & Offline Banner
        ConnectionStatusBar(isOnline = isOnline, pendingSyncCount = pendingSyncCount)

        // Modern Top App Bar
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
                            text = "استعلام‌های فعال",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        if (activeInquiries.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryBlueLight
                            ) {
                                Text(
                                    text = "${PersianFormatters.toPersianDigits(activeInquiries.size)} مورد",
                                    color = PrimaryBlue,
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
                        color = TextSecondary
                    )
                }

                FilledTonalButton(
                    onClick = viewModel::refresh,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Slate100,
                        contentColor = Slate700
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "بروزرسانی",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بروزرسانی",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Notification permission guidance if disabled
        if (!hasNotificationPermission) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                NotificationPermissionBanner(onEnableClicked = onRequestNotificationPermission)
            }
        }

        // Interactive Summary Cards (All, Urgent, Normal)
        if (activeInquiries.isNotEmpty()) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                InquirySummaryCards(
                    activeCount = activeInquiries.size,
                    urgentCount = urgentCount,
                    selectedFilter = filterType,
                    onFilterSelected = { filterType = it }
                )
            }
        }

        // Content Area
        when {
            isLoading && activeInquiries.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }

            errorMessage != null && activeInquiries.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Default.CloudOff,
                    iconTint = ErrorRed,
                    iconBgColor = ErrorRedLight,
                    title = "خطا در دریافت اطلاعات",
                    description = errorMessage ?: "امکان اتصال به سرور وجود ندارد. لطفاً اتصال اینترنت خود را بررسی نمایید.",
                    actionButtonText = "تلاش مجدد",
                    onActionClick = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                )
            }

            activeInquiries.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Default.Inbox,
                    iconTint = PrimaryBlue,
                    iconBgColor = PrimaryBlueLight,
                    title = "هیچ استعلام فعالی در انتظار پاسخ نیست",
                    description = "استعلام‌های جدید مشتریان به محض ثبت به صورت اعلان و زنگ به شما اطلاع‌رسانی خواهند شد.\nاستعلام‌های پاسخ‌داده‌شده را می‌توانید در تب «تاریخچه» مشاهده کنید.",
                    actionButtonText = "بروزرسانی صندوق",
                    onActionClick = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                )
            }

            filteredInquiries.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Default.Inbox,
                    iconTint = Slate500,
                    iconBgColor = Slate100,
                    title = "موردی با این فیلتر یافت نشد",
                    description = "استعلامی مطابق فیلتر انتخابی شما در صف انتظار قرار ندارد.",
                    actionButtonText = "مشاهده همه استعلام‌ها",
                    onActionClick = { filterType = "ALL" },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredInquiries, key = { it.inquiryId }) { inq ->
                        ModernInquiryCard(
                            inquiry = inq,
                            onClick = { onInquiryClick(inq.inquiryId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernInquiryCard(
    inquiry: InquiryEntity,
    onClick: () -> Unit
) {
    val isUrgent = inquiry.remainingSeconds in 1..180

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isUrgent) 1.5.dp else 1.dp,
                color = if (isUrgent) ErrorRed.copy(alpha = 0.5f) else BorderSubtle,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Bar: Tracking code & Status badge with countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "کد رهگیری: ${inquiry.trackingCode}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )

                InquiryStatusBadge(
                    status = inquiry.status,
                    remainingSeconds = inquiry.remainingSeconds
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle: Product Thumbnail + Title + Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProductThumbnail(
                    imageUrl = inquiry.productImageUrl,
                    contentDescription = inquiry.productTitle,
                    size = 64.dp,
                    cornerRadius = 10.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = inquiry.productTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "خریدار: ${inquiry.customerName}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // User note if provided
            if (!inquiry.userNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "سوال خریدار: «${inquiry.userNote}»",
                        fontSize = 11.sp,
                        color = Slate700,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Row: Reference Price + Big Response Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "قیمت مرجع در سایت",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = PersianFormatters.formatPriceToman(inquiry.recordedPriceToman),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUrgent) ErrorRed else PrimaryBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ثبت پاسخ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

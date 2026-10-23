package ir.torob.seller.ui.screens.inquiry_detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.ui.components.*
import ir.torob.seller.ui.theme.*
import ir.torob.seller.ui.viewmodel.InquiryViewModel
import ir.torob.seller.util.PersianFormatters

@Composable
fun InquiryDetailScreen(
    inquiryId: String,
    viewModel: InquiryViewModel,
    isOnline: Boolean,
    onBack: () -> Unit
) {
    LaunchedEffect(inquiryId) {
        viewModel.loadInquiryDetail(inquiryId)
    }

    val state by viewModel.detailUiState.collectAsState()
    val inquiry = state.inquiry

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PureWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "بازگشت",
                            tint = TextPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ثبت پاسخ به استعلام",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (inquiry != null) {
                            Text(
                                text = "کد رهگیری: ${inquiry.trackingCode}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    if (inquiry != null) {
                        InquiryStatusBadge(
                            status = inquiry.status,
                            remainingSeconds = inquiry.remainingSeconds
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (inquiry != null && inquiry.status == "ACTIVE") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PureWhite,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.submitResponse(inquiry.inquiryId) },
                            enabled = !state.isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isInStock) PrimaryBlue else ErrorRed
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    color = PureWhite,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.isInStock) Icons.AutoMirrored.Filled.Send else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isInStock) {
                                        if (isOnline) "ثبت و ارسال آنی پاسخ به خریدار" else "ذخیره در گوشی (ارسال خودکار پس از اتصال)"
                                    } else {
                                        "اعلام عدم موجودی کالا"
                                    },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
        ) {
            ConnectionStatusBar(isOnline = isOnline)

            if (inquiry == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sync badge if offline sync pending or completed
                    if (state.syncState != null) {
                        SyncBadge(syncState = state.syncState)
                    }

                    // Product & Inquiry Reference Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductThumbnail(
                                    imageUrl = inquiry.productImageUrl,
                                    contentDescription = inquiry.productTitle,
                                    size = 72.dp,
                                    cornerRadius = 12.dp
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = inquiry.productTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        lineHeight = 22.sp
                                    )

                                    if (!inquiry.categoryName.isNullOrBlank()) {
                                        Text(
                                            text = "دسته‌بندی: ${inquiry.categoryName}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Text(
                                        text = "مشتری: ${inquiry.customerName}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قیمت ثبت‌شده مرجع در سایت ترب:",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = PersianFormatters.formatPriceToman(inquiry.recordedPriceToman),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            if (!inquiry.userNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "سوال خریدار:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = inquiry.userNote,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Already Answered or Expired Card
                    if (inquiry.status == "EXPIRED") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ErrorRedLight),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "مهلت ۱۰ دقیقه‌ای پاسخ به پایان رسیده است",
                                        color = ErrorRedDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "به دلیل اتمام زمان، این استعلام به فروشگاه دیگری منتقل گردید.",
                                        color = ErrorRedDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else if (inquiry.status == "ANSWERED") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreenLight),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "پاسخ با موفقیت به خریدار ارسال شده است",
                                        color = SuccessGreenDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "جزئیات پاسخ شما در بخش «تاریخچه» قابل مشاهده است.",
                                        color = SuccessGreenDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Response Form
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "وضعیت موجودی و پیشنهاد قیمت",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Stock Selector: 2 Segmented Cards (Available vs Out of Stock)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Available Option
                                    StockOptionCard(
                                        title = "موجود در انبار",
                                        subtitle = "آماده ارسال و فروش",
                                        icon = Icons.Default.Check,
                                        isSelected = state.isInStock,
                                        activeColor = SuccessGreen,
                                        activeBg = SuccessGreenLight,
                                        onClick = { viewModel.onStockToggled(true) },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Out of Stock Option
                                    StockOptionCard(
                                        title = "عدم موجودی فعلی",
                                        subtitle = "کالا موجود نیست",
                                        icon = Icons.Default.Close,
                                        isSelected = !state.isInStock,
                                        activeColor = ErrorRed,
                                        activeBg = ErrorRedLight,
                                        onClick = { viewModel.onStockToggled(false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (state.isInStock) {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Inventory Quantity Stepper
                                    InventoryQuantitySelector(
                                        quantity = state.stockCount,
                                        onQuantityChange = viewModel::onStockCountChanged
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Quick Price Chips
                                    QuickPriceChips(
                                        currentPrice = state.offeredPriceToman,
                                        recordedPrice = inquiry.recordedPriceToman,
                                        onPriceSelected = viewModel::onPriceChanged
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Live Thousand-Separated Price Input Field
                                    PriceInputField(
                                        priceToman = state.offeredPriceToman,
                                        onPriceChange = viewModel::onPriceChanged,
                                        isError = state.offeredPriceToman <= 0,
                                        errorMessage = if (state.offeredPriceToman <= 0) "وارد کردن قیمت برای کالای موجود الزامی است" else null
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = ErrorRedLight.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = ErrorRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "پیام عدم موجودی به خریدار ارسال می‌شود و فیلد قیمت و تعداد کالا غیرفعال شد.",
                                                fontSize = 11.sp,
                                                color = ErrorRedDark,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Notes / Details for customer
                                OutlinedTextField(
                                    value = state.notes,
                                    onValueChange = viewModel::onNotesChanged,
                                    label = { Text("توضیحات تکمیلی برای خریدار (اختیاری)", fontSize = 12.sp) },
                                    placeholder = { Text("مثلاً: گارانتی ۱۸ ماهه شرکتی، ارسال فوری امروز", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedContainerColor = PureWhite,
                                        unfocusedContainerColor = PureWhite
                                    )
                                )

                                if (state.errorMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.errorMessage!!,
                                        color = ErrorRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Extra space at bottom to prevent floating content behind bottom bar
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun StockOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) activeColor else BorderSubtle,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeBg.copy(alpha = 0.4f) else PureWhite
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) activeColor else Slate100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) PureWhite else Slate500,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) activeColor else TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

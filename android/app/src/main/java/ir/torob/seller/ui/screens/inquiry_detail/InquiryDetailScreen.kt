package ir.torob.seller.ui.screens.inquiry_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.ui.components.ConnectionStatusBar
import ir.torob.seller.ui.components.QuickPriceChips
import ir.torob.seller.ui.components.SyncBadge
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhite,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "← بازگشت", color = Slate700, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت پاسخ به استعلام",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
        }

        ConnectionStatusBar(isOnline = isOnline)

        if (inquiry == null) {
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
                // Product & Inquiry Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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

                            // Status / Sync Badge
                            if (state.syncState != null) {
                                SyncBadge(syncState = state.syncState)
                            } else if (inquiry.status == "ANSWERED") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TorobLightGreen)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "پاسخ ثبت شد",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TorobGreen
                                    )
                                }
                            } else if (inquiry.status == "EXPIRED") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TorobLightRed)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "استعلام منقضی شده",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TorobRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = inquiry.productTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "قیمت ثبت‌شده مرجع در سایت:",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                            Text(
                                text = PersianFormatters.formatPriceToman(inquiry.recordedPriceToman),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }

                        if (!inquiry.userNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate100)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "سوال خریدار (${inquiry.customerName}):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = inquiry.userNote,
                                        fontSize = 12.sp,
                                        color = Slate800
                                    )
                                }
                            }
                        }
                    }
                }

                if (inquiry.status == "EXPIRED") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TorobLightRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "مهلت ۱۰ دقیقه‌ای پاسخ به این استعلام به پایان رسیده است و درخواست به فروشگاه دیگری منتقل گردید.",
                            color = TorobDarkRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else if (inquiry.status == "ANSWERED") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TorobLightGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "پاسخ این استعلام قبلاً با موفقیت ثبت و ارسال شده است.",
                                color = TorobGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "این استعلام در بخش تاریخچه استعلام‌ها نگهداری می‌شود.",
                                color = TorobGreen,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    // Response Input Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ثبت قیمت و موجودی فروشگاه شما",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // In Stock Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (state.isInStock) "کالا در انبار موجود است" else "عدم موجودی فعلی",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isInStock) TorobGreen else TorobRed
                                    )
                                    Text(
                                        text = "با خاموش کردن این گزینه، ناموجودی کالا به خریدار اعلام می‌شود.",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                                Switch(
                                    checked = state.isInStock,
                                    onCheckedChange = viewModel::onStockToggled,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PureWhite,
                                        checkedTrackColor = TorobGreen
                                    )
                                )
                            }

                            if (state.isInStock) {
                                Spacer(modifier = Modifier.height(14.dp))

                                // Stock Count Stepper
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Slate100)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "تعداد موجودی در انبار",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "تعداد کالای آماده ارسال را مشخص کنید",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = { viewModel.onStockCountChanged(state.stockCount - 1) },
                                            enabled = state.stockCount > 1,
                                            modifier = Modifier.size(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = PureWhite,
                                                contentColor = TorobRed
                                            )
                                        ) {
                                            Text(text = "−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Text(
                                            text = "${state.stockCount} عدد",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )

                                        FilledTonalIconButton(
                                            onClick = { viewModel.onStockCountChanged(state.stockCount + 1) },
                                            modifier = Modifier.size(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = PureWhite,
                                                contentColor = TorobGreen
                                            )
                                        ) {
                                            Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Quick Price Chips
                                QuickPriceChips(
                                    currentPrice = state.offeredPriceToman,
                                    recordedPrice = inquiry.recordedPriceToman,
                                    onPriceSelected = viewModel::onPriceChanged
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Price Input
                                OutlinedTextField(
                                    value = state.offeredPriceToman.toString(),
                                    onValueChange = { str ->
                                        val num = str.filter { it.isDigit() }.toLongOrNull() ?: 0L
                                        viewModel.onPriceChanged(num)
                                    },
                                    label = { Text("قیمت پیشنهادی (تومان)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    supportingText = {
                                        Text(
                                            text = PersianFormatters.formatPriceToman(state.offeredPriceToman),
                                            color = TorobRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Notes Input
                            OutlinedTextField(
                                value = state.notes,
                                onValueChange = viewModel::onNotesChanged,
                                label = { Text("توضیحات تکمیلی برای خریدار (اختیاری)") },
                                placeholder = { Text("مثلا: گارانتی اصلی ۱۸ ماهه، رنگ مشکی موجود است") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (state.errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.errorMessage!!,
                                    color = TorobRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Submit Button with Debounce Protection
                            Button(
                                onClick = { viewModel.submitResponse(inquiry.inquiryId) },
                                enabled = !state.isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TorobRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (state.isSubmitting) {
                                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(
                                        text = if (isOnline) "ثبت و ارسال آنی پاسخ به خریدار" else "ذخیره در گوشی (ارسال خودکار پس از اتصال)",
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
}

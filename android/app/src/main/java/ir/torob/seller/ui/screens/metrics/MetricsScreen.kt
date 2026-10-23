package ir.torob.seller.ui.screens.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.ui.theme.*
import ir.torob.seller.ui.viewmodel.MetricsViewModel
import ir.torob.seller.util.PersianFormatters

@Composable
fun MetricsScreen(
    viewModel: MetricsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val metrics = state.metrics

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
                    text = "عملکرد و شاخص‌های فروشگاه",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
                Text(
                    text = "پایش نرخ پاسخ‌گویی و سرعت ثبت قیمت در ترب",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }

        if (state.isLoading && metrics == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TorobRed)
            }
        } else if (metrics != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High level stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "نرخ پاسخ‌گویی",
                        value = "${PersianFormatters.toPersianDigits(metrics.responseRatePercent.toInt())}٪",
                        subtitle = "هدف سیستم: بالای ۸۵٪",
                        modifier = Modifier.weight(1f),
                        accentColor = TorobGreen
                    )
                    MetricCard(
                        title = "میانگین زمان پاسخ",
                        value = metrics.averageResponseTimeFormatted,
                        subtitle = "هدف سیستم: کمتر از ۵ دقیقه",
                        modifier = Modifier.weight(1f),
                        accentColor = TorobAmber
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "کل استعلام‌ها",
                        value = PersianFormatters.toPersianDigits(metrics.totalInquiries),
                        subtitle = "دریافتی از خریداران",
                        modifier = Modifier.weight(1f),
                        accentColor = Slate700
                    )
                    MetricCard(
                        title = "پاسخ‌داده‌شده",
                        value = PersianFormatters.toPersianDigits(metrics.answeredInquiries),
                        subtitle = "موفق",
                        modifier = Modifier.weight(1f),
                        accentColor = TorobGreen
                    )
                }

                // Guidelines card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "نحوه ارزیابی رتبه فروشگاه در ترب",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• فروشگاه‌هایی که در کمتر از ۵ دقیقه به استعلام‌ها پاسخ می‌دهند، در اولویت نمایش پیشنهادات در ترب قرار می‌گیرند.\n• عدم پاسخ به استعلام ظرف ۱۰ دقیقه منجر به انقضا و ارجاع درخواست به فروشگاه همسایه خواهد شد.",
                            fontSize = 12.sp,
                            color = Slate700,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 10.sp, color = Slate500)
        }
    }
}

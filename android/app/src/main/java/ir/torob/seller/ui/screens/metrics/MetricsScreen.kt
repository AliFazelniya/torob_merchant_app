package ir.torob.seller.ui.screens.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.data.model.MetricsDto
import ir.torob.seller.ui.components.EmptyStateView
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
            .background(TorobBackground)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = TorobSurface,
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
                    Text(
                        text = "عملکرد و شاخص‌های فروشگاه",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = TorobTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "پایش نرخ پاسخ‌گویی و سرعت ثبت قیمت در ترب",
                        fontSize = 11.sp,
                        color = TorobTextSecondary
                    )
                }

                FilledTonalButton(
                    onClick = viewModel::loadMetrics,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TorobBackground,
                        contentColor = TorobTextPrimary
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

        if (state.isLoading && metrics == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TorobRed)
            }
        } else if (metrics == null && state.errorMessage != null) {
            EmptyStateView(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = TorobError,
                iconBgColor = TorobErrorLight,
                title = "خطا در دریافت آمار عملکرد",
                description = state.errorMessage ?: "امکان بارگذاری شاخص‌های فروشگاه وجود ندارد.",
                actionButtonText = "تلاش مجدد",
                onActionClick = viewModel::loadMetrics,
                modifier = Modifier.fillMaxSize()
            )
        } else if (metrics != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Score Card
                HeroScoreCard(metrics = metrics)

                // 2x2 Metric Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernMetricCard(
                        title = "نرخ پاسخ‌گویی",
                        value = "${PersianFormatters.toPersianDigits(metrics.responseRatePercent.toInt())}٪",
                        subtitle = "هدف سیستم: بالای ۸۵٪",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = if (metrics.responseRatePercent >= 85f) TorobSuccess else TorobWarning,
                        bgColor = if (metrics.responseRatePercent >= 85f) TorobSuccessLight else TorobWarningLight,
                        modifier = Modifier.weight(1f)
                    )
                    ModernMetricCard(
                        title = "میانگین زمان پاسخ",
                        value = PersianFormatters.toPersianDigits(metrics.averageResponseTimeFormatted),
                        subtitle = "هدف سیستم: زیر ۵ دقیقه",
                        icon = Icons.Default.AccessTime,
                        accentColor = TorobInfo,
                        bgColor = TorobInfoLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernMetricCard(
                        title = "کل استعلام‌های دریافتی",
                        value = PersianFormatters.toPersianDigits(metrics.totalInquiries),
                        subtitle = "از خریداران ترب",
                        icon = Icons.Default.HourglassEmpty,
                        accentColor = TorobTextPrimary,
                        bgColor = TorobBackground,
                        modifier = Modifier.weight(1f)
                    )
                    ModernMetricCard(
                        title = "پاسخ‌های ثبت‌شده",
                        value = PersianFormatters.toPersianDigits(metrics.answeredInquiries),
                        subtitle = "موفق و در مهلت قانونی",
                        icon = Icons.Default.CheckCircle,
                        accentColor = TorobSuccess,
                        bgColor = TorobSuccessLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Visual Distribution Ratio Card
                RatioBreakdownCard(metrics = metrics)

                // Ranking & Tips Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TorobDivider, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TorobSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TorobWarningLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = TorobWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "راهنمای ارتقای رتبه فروشگاه در ترب",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TorobTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "• سرعت پاسخ‌گویی زیر ۵ دقیقه: فروشگاه شما در اولویت نمایش پیشنهادات ویژه ترب قرار می‌گیرد.\n• نرخ پاسخ بالای ۸۵٪: به عنوان فروشنده برتر و مطمئن به خریداران معرفی می‌شوید.\n• استعلام‌های منقضی‌شده: در صورت عدم ثبت قیمت ظرف ۱۰ دقیقه، استعلام به فروشگاه بعدی ارسال می‌شود.",
                            fontSize = 12.sp,
                            color = TorobTextSecondary,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroScoreCard(metrics: MetricsDto) {
    val rate = metrics.responseRatePercent
    val isGood = rate >= 85f
    val isModerate = rate in 70f..84.9f

    val badgeText = when {
        isGood -> "عملکرد عالی"
        isModerate -> "عملکرد متوسط"
        else -> "نیازمند بهبود"
    }

    val badgeColor = when {
        isGood -> TorobSuccess
        isModerate -> TorobWarning
        else -> TorobError
    }

    val badgeBg = when {
        isGood -> TorobSuccessLight
        isModerate -> TorobWarningLight
        else -> TorobErrorLight
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TorobDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TorobSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شاخص کلی عملکرد فروشگاه",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TorobTextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (rate / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = badgeColor,
                trackColor = TorobBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "نرخ پاسخ‌گویی: ${PersianFormatters.toPersianDigits(rate.toInt())}٪",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TorobTextPrimary
                )
                Text(
                    text = "زمان پاسخ: ${PersianFormatters.toPersianDigits(metrics.averageResponseTimeFormatted)}",
                    fontSize = 12.sp,
                    color = TorobTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ModernMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, TorobDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TorobSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TorobTextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TorobTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RatioBreakdownCard(metrics: MetricsDto) {
    val total = metrics.totalInquiries.coerceAtLeast(1)
    val answered = metrics.answeredInquiries
    val expired = metrics.expiredInquiries
    val active = metrics.activeInquiries

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TorobDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TorobSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "توزیع وضعیت استعلام‌ها",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TorobTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment horizontal bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                if (answered > 0) {
                    Box(
                        modifier = Modifier
                            .weight(answered.toFloat() / total)
                            .fillMaxHeight()
                            .background(TorobSuccess)
                    )
                }
                if (active > 0) {
                    Box(
                        modifier = Modifier
                            .weight(active.toFloat() / total)
                            .fillMaxHeight()
                            .background(TorobWarning)
                    )
                }
                if (expired > 0) {
                    Box(
                        modifier = Modifier
                            .weight(expired.toFloat() / total)
                            .fillMaxHeight()
                            .background(TorobError)
                    )
                }
                if (answered == 0 && active == 0 && expired == 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(TorobBackground)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(label = "پاسخ‌داده‌شده", count = answered, color = TorobSuccess)
                LegendItem(label = "در جریان", count = active, color = TorobWarning)
                LegendItem(label = "منقضی‌شده", count = expired, color = TorobError)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label (${PersianFormatters.toPersianDigits(count)})",
            fontSize = 11.sp,
            color = TorobTextSecondary
        )
    }
}

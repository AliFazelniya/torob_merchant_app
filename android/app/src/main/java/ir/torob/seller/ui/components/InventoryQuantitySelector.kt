package ir.torob.seller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.ui.theme.*
import ir.torob.seller.util.PersianFormatters

@Composable
fun InventoryQuantitySelector(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = 1,
    maxQuantity: Int = 9999,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Slate50 else Slate100,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "تعداد موجودی آماده ارسال",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) TextPrimary else TextSecondary
                )
                Text(
                    text = "تعداد کالای موجود در انبار برای تحویل فوری",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Decrement Button (−)
                IconButton(
                    onClick = {
                        if (quantity > minQuantity) {
                            onQuantityChange(quantity - 1)
                        }
                    },
                    enabled = enabled && quantity > minQuantity,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (enabled && quantity > minQuantity) PureWhite else Slate200)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "کاهش تعداد",
                        tint = if (enabled && quantity > minQuantity) PrimaryBlue else Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quantity Text Display
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PureWhite)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${PersianFormatters.toPersianDigits(quantity)} عدد",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) TextPrimary else TextSecondary
                    )
                }

                // Increment Button (+)
                IconButton(
                    onClick = {
                        if (quantity < maxQuantity) {
                            onQuantityChange(quantity + 1)
                        }
                    },
                    enabled = enabled && quantity < maxQuantity,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (enabled && quantity < maxQuantity) PureWhite else Slate200)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "افزایش تعداد",
                        tint = if (enabled && quantity < maxQuantity) PrimaryBlue else Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

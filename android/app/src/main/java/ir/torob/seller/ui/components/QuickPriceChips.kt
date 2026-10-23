package ir.torob.seller.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.torob.seller.ui.theme.*

@Composable
fun QuickPriceChips(
    currentPrice: Long,
    recordedPrice: Long,
    onPriceSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "تغییر سریع قیمت با یک لمس:",
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SuggestionChip(
                onClick = { onPriceSelected(recordedPrice) },
                label = { Text("قیمت مرجع", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Slate100,
                    labelColor = PrimaryBlue
                ),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
            SuggestionChip(
                onClick = { onPriceSelected(maxOf(0L, currentPrice - 50_000)) },
                label = { Text("۵۰- هزار", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Slate100,
                    labelColor = ErrorRed
                ),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
            SuggestionChip(
                onClick = { onPriceSelected(currentPrice + 50_000) },
                label = { Text("۵۰+ هزار", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Slate100,
                    labelColor = SuccessGreen
                ),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
            SuggestionChip(
                onClick = { onPriceSelected(currentPrice + 100_000) },
                label = { Text("۱۰۰+ هزار", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Slate100,
                    labelColor = SuccessGreen
                ),
                border = BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

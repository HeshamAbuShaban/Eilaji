package dev.anonymous.eilaji.ui.other.orders

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val STEPS = listOf("PENDING", "CONFIRMED", "PREPARING", "SHIPPED", "DELIVERED")
private val Brand = Color(0xFFBA324F)

@Composable
fun TrackingSteps(status: String, progress: Float) {
    val idx = STEPS.indexOf(status.uppercase()).coerceAtLeast(0)
    val animProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "track")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            STEPS.forEachIndexed { i, step ->
                val done = i <= idx
                val dot by animateColorAsState(
                    targetValue = if (done) Brand else Color(0xFFE0E0E0),
                    label = "dot$i"
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(if (i == idx) 16.dp else 12.dp)
                            .clip(CircleShape)
                            .background(dot)
                            .alpha(if (done) 1f else 0.7f)
                    )
                    Text(
                        text = step.take(4),
                        fontSize = 9.sp,
                        fontWeight = if (i == idx) FontWeight.Bold else FontWeight.Normal,
                        color = if (done) Brand else Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = animProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            color = Brand,
            trackColor = Color(0xFFF0E4E8),
        )
    }
}

@Composable
fun CheckoutTotals(subtotal: Double, fee: Double, prepMin: Int?) {
    val total = subtotal + fee
    Column(modifier = Modifier.fillMaxWidth()) {
        TotalRow("Subtotal", subtotal, false)
        TotalRow(if (prepMin != null) "Delivery · ~${prepMin} min" else "Delivery", fee, false)
        TotalRow("Total", total, true)
    }
}

@Composable
private fun TotalRow(label: String, amount: Double, bold: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
        Text(
            text = String.format("%.2f $", amount),
            fontSize = if (bold) 16.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (bold) Brand else MaterialTheme.colorScheme.onSurface
        )
    }
}

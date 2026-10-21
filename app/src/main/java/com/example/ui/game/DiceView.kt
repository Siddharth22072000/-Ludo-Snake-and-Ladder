package com.example.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GameCard
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarGold

@Composable
fun DiceView(
    diceValue: Int,
    isMyTurn: Boolean,
    diceRolled: Boolean,
    onRollClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(diceValue) {
        rotationAnim.snapTo(0f)
        scaleAnim.snapTo(1.2f)
        rotationAnim.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 150)
        )
    }

    val canRoll = isMyTurn && !diceRolled

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(scaleAnim.value)
                .rotate(rotationAnim.value)
                .shadow(
                    elevation = if (canRoll) 12.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = if (canRoll) NeonPurple else Color.Black
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        if (canRoll) listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                        else listOf(Color(0xFF322C5C), Color(0xFF231E44))
                    )
                )
                .border(
                    width = if (canRoll) 2.dp else 1.dp,
                    color = if (canRoll) StarGold else Color(0xFF4C447C),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(enabled = canRoll) {
                    onRollClicked()
                }
                .testTag("roll_dice_button"),
            contentAlignment = Alignment.Center
        ) {
            DicePips(value = diceValue.coerceIn(1, 6), isEnabled = canRoll)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                !isMyTurn -> "Waiting..."
                !diceRolled -> "TAP TO ROLL!"
                else -> "Rolled: $diceValue"
            },
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                canRoll -> StarGold
                isMyTurn -> Color.White
                else -> Color(0xFF9E99C2)
            }
        )
    }
}

@Composable
private fun DicePips(value: Int, isEnabled: Boolean) {
    val pipColor = if (isEnabled) Color.White else Color(0xFFD4D0E8)

    // 3x3 grid pips representation
    val pips = when (value) {
        1 -> listOf(false, false, false, false, true, false, false, false, false)
        2 -> listOf(true, false, false, false, false, false, false, false, true)
        3 -> listOf(true, false, false, false, true, false, false, false, true)
        4 -> listOf(true, false, true, false, false, false, true, false, true)
        5 -> listOf(true, false, true, false, true, false, true, false, true)
        6 -> listOf(true, false, true, true, false, true, true, false, true)
        else -> listOf(false, false, false, false, true, false, false, false, false)
    }

    Column(
        modifier = Modifier.size(46.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.size(width = 46.dp, height = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0..2) {
                    val hasPip = pips[row * 3 + col]
                    if (hasPip) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(pipColor)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}

package com.example.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BoardConstants
import com.example.ui.theme.LadderRailColor
import com.example.ui.theme.LadderRungColor
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SafeTileColor
import com.example.ui.theme.SnakeBodyColor
import com.example.ui.theme.SnakeHeadColor
import com.example.ui.theme.StarGold
import com.example.ui.theme.TileDark
import com.example.ui.theme.TileLight
import com.example.ui.theme.TokenBlue
import com.example.ui.theme.TokenGreen
import com.example.ui.theme.TokenRed
import com.example.ui.theme.TokenYellow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun BoardView(
    playerTokens: List<Long>,
    playerColors: List<String>,
    currentTurnIndex: Int,
    movableTokenIndices: List<Int>, // relative indices (0 or 1) of movable tokens for current turn
    onTokenClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val boardWidth = constraints.maxWidth.toFloat()
        val tileSize = boardWidth / 10f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .testTag("game_board_canvas")
                .pointerInput(playerTokens, movableTokenIndices, currentTurnIndex) {
                    detectTapGestures { tapOffset ->
                        val col = (tapOffset.x / tileSize).toInt().coerceIn(0, 9)
                        val row = (tapOffset.y / tileSize).toInt().coerceIn(0, 9)
                        val tappedTile = getTileNumberFromGrid(col, row)

                        // Check if tapped on or near any movable token
                        val startTokenIdx = currentTurnIndex * BoardConstants.TOKENS_PER_PLAYER
                        for (relIdx in movableTokenIndices) {
                            val globalIdx = startTokenIdx + relIdx
                            val tokenPos = playerTokens.getOrElse(globalIdx) { 0L }.toInt()
                            if (tokenPos == tappedTile || (tokenPos == 0 && (row >= 8 || row <= 1))) {
                                onTokenClicked(relIdx)
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            drawBoardGrid(tileSize, textMeasurer)
            drawLadders(tileSize)
            drawSnakes(tileSize)
            drawTokens(
                tileSize = tileSize,
                playerTokens = playerTokens,
                playerColors = playerColors,
                currentTurnIndex = currentTurnIndex,
                movableTokenIndices = movableTokenIndices,
                pulseScale = pulseScale,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawBoardGrid(tileSize: Float, textMeasurer: TextMeasurer) {
    for (row in 0..9) {
        for (col in 0..9) {
            val tileNum = getTileNumberFromGrid(col, row)
            val isEven = (row + col) % 2 == 0
            val isSafe = BoardConstants.SAFE_TILES.contains(tileNum)

            val bgColor = when {
                tileNum == 100 -> Color(0xFF6D28D9) // Victory crown tile
                isSafe -> Color(0xFF1E3A2F)
                isEven -> TileLight
                else -> TileDark
            }

            drawRoundRect(
                color = bgColor,
                topLeft = Offset(col * tileSize + 1f, row * tileSize + 1f),
                size = Size(tileSize - 2f, tileSize - 2f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Star / Safe badge
            if (isSafe && tileNum != 100) {
                drawCircle(
                    color = SafeTileColor.copy(alpha = 0.25f),
                    radius = tileSize * 0.35f,
                    center = Offset(col * tileSize + tileSize / 2f, row * tileSize + tileSize / 2f)
                )
            }

            // Tile Number text
            val textLayout = textMeasurer.measure(
                text = "$tileNum",
                style = TextStyle(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSafe) SafeTileColor else Color(0xFF8E88B8)
                )
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(col * tileSize + 3.dp.toPx(), row * tileSize + 2.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawLadders(tileSize: Float) {
    for ((start, end) in BoardConstants.LADDERS) {
        val startCenter = getTileCenter(start, tileSize)
        val endCenter = getTileCenter(end, tileSize)

        val dx = endCenter.x - startCenter.x
        val dy = endCenter.y - startCenter.y
        val angle = atan2(dy, dx)
        val perpAngle = angle + (Math.PI / 2).toFloat()
        val railOffset = 6.dp.toPx()

        val ox = railOffset * cos(perpAngle)
        val oy = railOffset * sin(perpAngle)

        val p1 = Offset(startCenter.x - ox, startCenter.y - oy)
        val p2 = Offset(endCenter.x - ox, endCenter.y - oy)
        val p3 = Offset(startCenter.x + ox, startCenter.y + oy)
        val p4 = Offset(endCenter.x + ox, endCenter.y + oy)

        // Rails
        drawLine(
            color = LadderRailColor,
            start = p1,
            end = p2,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = LadderRailColor,
            start = p3,
            end = p4,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Rungs
        val ladderLength = sqrt(dx * dx + dy * dy)
        val numRungs = (ladderLength / 18.dp.toPx()).toInt().coerceAtLeast(2)
        for (i in 1..numRungs) {
            val fraction = i.toFloat() / (numRungs + 1)
            val rx = startCenter.x + dx * fraction
            val ry = startCenter.y + dy * fraction
            drawLine(
                color = LadderRungColor,
                start = Offset(rx - ox, ry - oy),
                end = Offset(rx + ox, ry + oy),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawSnakes(tileSize: Float) {
    for ((head, tail) in BoardConstants.SNAKES) {
        val headCenter = getTileCenter(head, tileSize)
        val tailCenter = getTileCenter(tail, tileSize)

        val midX = (headCenter.x + tailCenter.x) / 2f + (headCenter.y - tailCenter.y) * 0.25f
        val midY = (headCenter.y + tailCenter.y) / 2f - (headCenter.x - tailCenter.x) * 0.25f

        val snakePath = Path().apply {
            moveTo(headCenter.x, headCenter.y)
            quadraticTo(midX, midY, tailCenter.x, tailCenter.y)
        }

        // Snake body shadow and core
        drawPath(
            path = snakePath,
            color = SnakeBodyColor,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Snake head (diamond/oval)
        drawCircle(
            color = SnakeHeadColor,
            radius = 7.dp.toPx(),
            center = headCenter
        )
        // Snake eyes
        drawCircle(
            color = Color.Yellow,
            radius = 2.dp.toPx(),
            center = Offset(headCenter.x - 2.5.dp.toPx(), headCenter.y - 2.5.dp.toPx())
        )
        drawCircle(
            color = Color.Yellow,
            radius = 2.dp.toPx(),
            center = Offset(headCenter.x + 2.5.dp.toPx(), headCenter.y - 2.5.dp.toPx())
        )
    }
}

private fun DrawScope.drawTokens(
    tileSize: Float,
    playerTokens: List<Long>,
    playerColors: List<String>,
    currentTurnIndex: Int,
    movableTokenIndices: List<Int>,
    pulseScale: Float,
    textMeasurer: TextMeasurer
) {
    // Group tokens by position to offset co-located tokens
    val positionGroups = mutableMapOf<Long, MutableList<Pair<Int, Int>>>() // pos -> list of (playerIdx, tokenRelIdx)
    for (i in playerTokens.indices) {
        val pIdx = i / BoardConstants.TOKENS_PER_PLAYER
        val relIdx = i % BoardConstants.TOKENS_PER_PLAYER
        val pos = playerTokens[i]
        positionGroups.getOrPut(pos) { mutableListOf() }.add(Pair(pIdx, relIdx))
    }

    for (i in playerTokens.indices) {
        val pIdx = i / BoardConstants.TOKENS_PER_PLAYER
        val relIdx = i % BoardConstants.TOKENS_PER_PLAYER
        val pos = playerTokens[i]
        val isCurrentPlayer = (pIdx == currentTurnIndex)
        val isMovable = isCurrentPlayer && movableTokenIndices.contains(relIdx)

        val color = resolvePlayerColor(playerColors.getOrElse(pIdx) { "RED" })

        val centerOffset: Offset
        if (pos == 0L) {
            // Render in dedicated corner dock area for base tokens
            val baseStartX = if (pIdx % 2 == 0) tileSize * 0.5f else tileSize * 8.5f
            val baseStartY = if (pIdx < 2) tileSize * 9.2f else tileSize * 0.8f
            centerOffset = Offset(
                baseStartX + (if (relIdx == 0) -8.dp.toPx() else 8.dp.toPx()),
                baseStartY
            )
        } else {
            val tileCenter = getTileCenter(pos.toInt(), tileSize)
            val coLocated = positionGroups[pos] ?: emptyList()
            val listIdx = coLocated.indexOfFirst { it.first == pIdx && it.second == relIdx }
            val totalInTile = coLocated.size
            if (totalInTile > 1 && listIdx >= 0) {
                val offsetDist = 7.dp.toPx()
                val offsetAngle = (2 * Math.PI * listIdx / totalInTile).toFloat()
                centerOffset = Offset(
                    tileCenter.x + offsetDist * cos(offsetAngle),
                    tileCenter.y + offsetDist * sin(offsetAngle)
                )
            } else {
                centerOffset = tileCenter
            }
        }

        val baseRadius = 8.5.dp.toPx()
        val tokenRadius = if (isMovable) baseRadius * pulseScale else baseRadius

        // Glow ring if movable
        if (isMovable) {
            drawCircle(
                color = StarGold.copy(alpha = 0.5f),
                radius = tokenRadius + 4.dp.toPx(),
                center = centerOffset
            )
        }

        // Token outer border and fill
        drawCircle(
            color = Color.White,
            radius = tokenRadius + 1.5.dp.toPx(),
            center = centerOffset
        )
        drawCircle(
            color = color,
            radius = tokenRadius,
            center = centerOffset
        )

        // Token label (T1 or T2)
        val label = "T${relIdx + 1}"
        val labelResult = textMeasurer.measure(
            text = label,
            style = TextStyle(
                fontSize = 7.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )
        drawText(
            textLayoutResult = labelResult,
            topLeft = Offset(
                centerOffset.x - labelResult.size.width / 2f,
                centerOffset.y - labelResult.size.height / 2f
            )
        )
    }
}

private fun resolvePlayerColor(name: String): Color {
    return when (name.uppercase()) {
        "RED" -> TokenRed
        "BLUE" -> TokenBlue
        "GREEN" -> TokenGreen
        "YELLOW" -> TokenYellow
        else -> NeonCoral
    }
}

private fun getTileCenter(tileNumber: Int, tileSize: Float): Offset {
    val (gridX, gridY) = BoardConstants.getTileCoordinates(tileNumber)
    return Offset(
        gridX * tileSize + tileSize / 2f,
        gridY * tileSize + tileSize / 2f
    )
}

private fun getTileNumberFromGrid(col: Int, row: Int): Int {
    val bottomRow = 9 - row
    val naturalCol = if (bottomRow % 2 == 0) col else 9 - col
    return (bottomRow * 10) + naturalCol + 1
}

package com.example.ui.game

import com.example.data.model.BoardConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class OfflineGameState(
    val playerCount: Int = 2,
    val playerNames: List<String> = listOf("You", "Bot Alex"),
    val playerColors: List<String> = listOf("RED", "BLUE"),
    val isAiPlayer: List<Boolean> = listOf(false, true),
    val playerTokens: List<Long> = List(4) { 0L }, // 2 tokens per player
    val currentTurnIndex: Int = 0,
    val diceValue: Int = 1,
    val diceRolled: Boolean = false,
    val lastAction: String = "Game started! Roll the dice to begin.",
    val winnerName: String? = null,
    val isGameOver: Boolean = false
) {
    fun getMovableTokenIndices(): List<Int> {
        if (!diceRolled || isGameOver) return emptyList()
        val startTokenIdx = currentTurnIndex * BoardConstants.TOKENS_PER_PLAYER
        val validIndices = mutableListOf<Int>()
        for (relIdx in 0 until BoardConstants.TOKENS_PER_PLAYER) {
            val pos = playerTokens[startTokenIdx + relIdx]
            if (pos == 0L && diceValue == 6) {
                validIndices.add(relIdx)
            } else if (pos > 0L && pos + diceValue <= 100L) {
                validIndices.add(relIdx)
            }
        }
        return validIndices
    }
}

class OfflineGameEngine(
    initialState: OfflineGameState,
    private val onStateChanged: (OfflineGameState) -> Unit
) {
    var state: OfflineGameState = initialState
        private set(value) {
            field = value
            onStateChanged(value)
        }

    fun rollDice() {
        if (state.diceRolled || state.isGameOver) return

        val rolled = Random.nextInt(1, 7)
        val currentPName = state.playerNames[state.currentTurnIndex]
        val tempState = state.copy(diceValue = rolled, diceRolled = true)

        val movable = tempState.getMovableTokenIndices()
        if (movable.isEmpty()) {
            // No moves available, pass turn
            val nextTurn = (state.currentTurnIndex + 1) % state.playerCount
            state = state.copy(
                diceValue = rolled,
                diceRolled = false,
                currentTurnIndex = nextTurn,
                lastAction = "$currentPName rolled $rolled (No valid moves, turn skipped)"
            )
            triggerAiTurnIfNeeded()
        } else if (movable.size == 1 && state.isAiPlayer[state.currentTurnIndex]) {
            // AI single move
            state = tempState
            triggerAiTurnIfNeeded()
        } else {
            state = tempState.copy(
                lastAction = "$currentPName rolled a $rolled! Select a token to move."
            )
            triggerAiTurnIfNeeded()
        }
    }

    fun moveToken(relativeTokenIndex: Int) {
        if (!state.diceRolled || state.isGameOver) return
        val currentTurn = state.currentTurnIndex
        val tokens = state.playerTokens.toMutableList()
        val globalTokenIdx = currentTurn * BoardConstants.TOKENS_PER_PLAYER + relativeTokenIndex
        val currentPos = tokens[globalTokenIdx]
        val dice = state.diceValue

        // Move validation
        if (currentPos == 0L && dice != 6) return
        if (currentPos > 0L && currentPos + dice > 100L) return

        var newPos = if (currentPos == 0L) 1L else currentPos + dice
        val pName = state.playerNames[currentTurn]
        var actionDesc = "$pName moved to tile $newPos"
        var extraTurn = (dice == 6)

        // Ladder
        val ladder = BoardConstants.LADDERS[newPos.toInt()]
        if (ladder != null) {
            newPos = ladder.toLong()
            actionDesc = "$pName climbed a 🪜 ladder to $newPos!"
        }

        // Snake
        val snake = BoardConstants.SNAKES[newPos.toInt()]
        if (snake != null) {
            newPos = snake.toLong()
            actionDesc = "🐍 Snake bit $pName down to $newPos!"
        }

        // Knockout opponent
        if (!BoardConstants.SAFE_TILES.contains(newPos.toInt())) {
            for (idx in tokens.indices) {
                val otherPlayer = idx / BoardConstants.TOKENS_PER_PLAYER
                if (otherPlayer != currentTurn && tokens[idx] == newPos) {
                    tokens[idx] = 0L
                    extraTurn = true
                    actionDesc = "💥 $pName knocked ${state.playerNames[otherPlayer]}'s token back to base!"
                    break
                }
            }
        }

        tokens[globalTokenIdx] = newPos

        // Check Win
        val startToken = currentTurn * BoardConstants.TOKENS_PER_PLAYER
        val endToken = startToken + BoardConstants.TOKENS_PER_PLAYER
        val hasWon = (startToken until endToken).all { tokens[it] == 100L }

        if (hasWon) {
            state = state.copy(
                playerTokens = tokens,
                isGameOver = true,
                winnerName = pName,
                lastAction = "🏆 $pName has won the game!",
                diceRolled = false
            )
        } else if (extraTurn) {
            state = state.copy(
                playerTokens = tokens,
                diceRolled = false,
                lastAction = "$actionDesc (Roll again!)"
            )
            triggerAiTurnIfNeeded()
        } else {
            val nextTurn = (currentTurn + 1) % state.playerCount
            state = state.copy(
                playerTokens = tokens,
                currentTurnIndex = nextTurn,
                diceRolled = false,
                lastAction = actionDesc
            )
            triggerAiTurnIfNeeded()
        }
    }

    private fun triggerAiTurnIfNeeded() {
        if (state.isGameOver) return
        val currentTurn = state.currentTurnIndex
        if (state.isAiPlayer[currentTurn]) {
            // Run AI coroutine
            CoroutineScope(Dispatchers.Main).launch {
                delay(600)
                if (!state.diceRolled) {
                    rollDice()
                }
                delay(700)
                if (state.diceRolled && !state.isGameOver) {
                    val movable = state.getMovableTokenIndices()
                    if (movable.isNotEmpty()) {
                        // Smart AI Choice: Ladder > Knockout > Escape Snake > Spawn Base > Progress
                        val chosenRelIdx = pickBestAiMove(movable)
                        moveToken(chosenRelIdx)
                    }
                }
            }
        }
    }

    private fun pickBestAiMove(movable: List<Int>): Int {
        if (movable.size == 1) return movable[0]
        val currentTurn = state.currentTurnIndex
        val tokens = state.playerTokens
        val dice = state.diceValue

        var bestScore = -999
        var bestIdx = movable[0]

        for (relIdx in movable) {
            val globalIdx = currentTurn * BoardConstants.TOKENS_PER_PLAYER + relIdx
            val currentPos = tokens[globalIdx]
            val newPos = if (currentPos == 0L) 1L else currentPos + dice
            var score = 0

            // Ladder bonus
            if (BoardConstants.LADDERS.containsKey(newPos.toInt())) score += 50
            // Snake penalty
            if (BoardConstants.SNAKES.containsKey(newPos.toInt())) score -= 40
            // Knockout bonus
            if (!BoardConstants.SAFE_TILES.contains(newPos.toInt())) {
                for (otherIdx in tokens.indices) {
                    val otherPlayer = otherIdx / BoardConstants.TOKENS_PER_PLAYER
                    if (otherPlayer != currentTurn && tokens[otherIdx] == newPos) {
                        score += 60
                    }
                }
            }
            // Spawn from base bonus
            if (currentPos == 0L) score += 30
            // Progress towards 100
            score += (newPos.toInt() / 5)

            if (score > bestScore) {
                bestScore = score
                bestIdx = relIdx
            }
        }
        return bestIdx
    }
}

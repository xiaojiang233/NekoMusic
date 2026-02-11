package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import top.xiaojiang233.nekomusic.utils.LyricsParser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KaraokeLyricLine(
    text: String,
    words: List<LyricsParser.LyricWord>?,
    currentPosition: Long,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.Gray
) {
    val textStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = fontSize,
        lineHeight = fontSize * 1.4,
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold
    )

    // 如果没有逐字歌词数据，直接显示静态文本（默认使用 activeColor 或根据需求调整）
    if (words == null) {
        Text(
            text = text,
            style = textStyle,
            color = activeColor,
            textAlign = TextAlign.Start,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    // 重构：使用 FlowRow 包裹单个单词组件，实现逐词动画
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        words.forEachIndexed { index, word ->
            KaraokeWord(
                word = word,
                currentPosition = currentPosition,
                textStyle = textStyle,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                isLastWord = index == words.lastIndex
            )
        }
    }
}

@Composable
fun KaraokeWord(
    word: LyricsParser.LyricWord,
    currentPosition: Long,
    textStyle: TextStyle,
    activeColor: Color,
    inactiveColor: Color,
    isLastWord: Boolean
) {
    val duration = word.duration
    val startTime = word.startTime
    val endTime = startTime + duration

    // 进度：0 (未开始) -> 1 (结束)
    // 允许 values < 0 或 > 1
    val rawProgress = if (duration > 0) (currentPosition - startTime).toFloat() / duration else if (currentPosition >= startTime) 1f else 0f
    val progress = rawProgress.coerceIn(0f, 1f)

    val isSinging = currentPosition in startTime..endTime

    // Only apply special effect if it's the last word AND it's a long note (e.g. > 500ms)
    val isLongWord = duration > 1500
    val shouldEffect = isLastWord && isLongWord

    // Scale 动画：正在唱时变大，唱完或者没唱时是 1.0
    // 设定目标 scale
    // 改进：使用 progress 来驱动 scale 曲线，或者简单的状态切换
    // 需求：慢慢变大，读完缩回去

    // 如果是 isSinging，目标 scale 设为 1.2 (或者根据 progress 插值)
    // 比如：start: 1.0 -> end: 1.2
    // 用户说“读完缩回去”，所以可以根据 isSinging 状态

    val targetScale = if (isSinging && shouldEffect) {
        // 动态计算：从 1.0 -> 1.3
        1.0f + (0.05f * progress)
    } else {
        1.0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = if (isSinging) 100 else 300), // 唱的时候响应快一点，缩回去慢一点
        label = "wordScale"
    )

    // Blur / Glow 效果
    // 当正在唱的时候，发光强度增加
    val glowAlpha = if (isSinging && shouldEffect) 0.1f * progress else 0f

    Box(
        modifier = Modifier
            .padding(end = 2.dp) // 字间距，视情况调整。原文本可能不含空格如果 words 里没有空格
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                // 简单的发光可以通过 shadow 或者 blur 实现，Compose Desktop/Mobile 在 graphicsLayer 中的 renderEffect 兼容性不一
                // 这里用 alpha 混合或者自定义 draw
            }
    ) {
        // 1. 发光层 (通过绘制模糊的背景文字模拟)
        if (isSinging && shouldEffect) {
            Text(
                text = word.text,
                style = textStyle.copy(
                    color = activeColor.copy(alpha = glowAlpha),
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = activeColor,
                        blurRadius = 1f * progress
                    )
                ),
                modifier = Modifier.matchParentSize()
            )
        }

        // 2. 底层 (灰色)
        Text(
            text = word.text,
            style = textStyle,
            color = inactiveColor
        )

        // 3. 顶层 (高亮裁剪)
        if (rawProgress > 0f) {
            val clipProgress = progress // 0..1

            Text(
                text = word.text,
                style = textStyle,
                color = activeColor, // 字体颜色
                modifier = Modifier
                    .matchParentSize()
                    .drawWithCache {
                        onDrawWithContent {
                            if (clipProgress >= 1f) {
                                drawContent()
                            } else {
                                clipRect(
                                    right = size.width * clipProgress
                                ) {
                                    this@onDrawWithContent.drawContent()
                                }
                            }
                        }
                    }
            )
        }
    }
}

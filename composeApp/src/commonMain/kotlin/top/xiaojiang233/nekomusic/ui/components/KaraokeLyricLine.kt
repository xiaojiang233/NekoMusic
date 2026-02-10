package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import top.xiaojiang233.nekomusic.utils.LyricsParser

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

    // 使用 Box 叠加两层文字：底层为底色，顶层为高亮色（通过裁剪显示进度）
    Box(modifier = modifier.fillMaxWidth()) {
        // 1. 底层：未读部分（灰色）
        Text(
            text = text,
            style = textStyle,
            color = inactiveColor,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // 2. 顶层：已读部分（白色高光），根据进度裁剪
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        Text(
            text = text,
            style = textStyle,
            color = activeColor,
            textAlign = TextAlign.Start,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    onDrawWithContent {
                        val layout = textLayoutResult ?: return@onDrawWithContent

                        // 创建裁剪路径
                        val maskPath = Path()
                        var charIndexOffset = 0
                        val rawTextLength = layout.layoutInput.text.length

                        // 遍历每个词计算进度
                        words.forEach { word ->
                            val wordEnd = word.startTime + word.duration
                            val wordLen = word.text.length
                            val nextCharIndex = charIndexOffset + wordLen

                            // 确保索引安全
                            val start = charIndexOffset.coerceAtMost(rawTextLength)
                            val end = nextCharIndex.coerceAtMost(rawTextLength)

                            if (start < end) {
                                if (currentPosition >= wordEnd) {
                                    // 1. 已经唱完的词：完全显示
                                    // 获取该词对应的文字路径区域
                                    maskPath.addPath(layout.getPathForRange(start, end))
                                } else if (currentPosition in word.startTime until wordEnd) {
                                    // 2. 正在唱的词：按百分比显示
                                    val progress = (currentPosition - word.startTime).toFloat() / word.duration.toFloat()
                                    val coercedProgress = progress.coerceIn(0f, 1f)

                                    val wordPath = layout.getPathForRange(start, end)
                                    val wordBounds = wordPath.getBounds()

                                    // 计算当前进度的矩形区域
                                    val fillWidth = wordBounds.width * coercedProgress
                                    val fillRect = Rect(
                                        left = wordBounds.left,
                                        top = wordBounds.top,
                                        right = wordBounds.left + fillWidth,
                                        bottom = wordBounds.bottom
                                    )

                                    // 将该矩形加入 Mask
                                    maskPath.addRect(fillRect)
                                }
                            }
                            charIndexOffset = nextCharIndex
                        }

                        // 应用裁剪路径绘制高亮层
                        clipPath(maskPath) {
                            this@onDrawWithContent.drawContent()
                        }
                    }
                }
        )
    }
}

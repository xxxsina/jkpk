package com.jiankangpaika.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * HTML工具类，用于将HTML文本转换为AnnotatedString以支持HTML样式
 */
object HtmlUtils {
    
    /**
     * 将HTML文本转换为AnnotatedString以支持HTML样式
     * 支持的HTML标签：
     * - <b>, <strong>: 粗体
     * - <i>, <em>: 斜体
     * - <u>: 下划线
     * - <s>, <strike>, <del>: 删除线
     * - <span style="...">: 内联样式
     * - <h1> - <h6>: 标题
     * - <p>: 段落
     * - <br>: 换行
     * - <font color="...">: 字体颜色
     */
    @Composable
    fun htmlToAnnotatedString(html: String): AnnotatedString {
        return buildAnnotatedString {
            parseHtmlWithStyles(html, this)
        }
    }
    
    /**
     * 解析HTML文本并应用样式
     */
    private fun parseHtmlWithStyles(html: String, builder: AnnotatedString.Builder) {
        var processedHtml = html
        
        // 处理换行标签
        processedHtml = processedHtml.replace("<br\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
        processedHtml = processedHtml.replace("<br>".toRegex(RegexOption.IGNORE_CASE), "\n")
        
        // 处理段落标签
        processedHtml = processedHtml.replace("<p\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
        processedHtml = processedHtml.replace("</p>".toRegex(RegexOption.IGNORE_CASE), "\n\n")
        
        var currentIndex = 0
        
        // 定义所有支持的HTML标签模式
        val patterns = listOf(
            // 内联样式 span
            "<span\\s+style=\"([^\"]*)\">([^<]*)</span>" to { styleAttr: String, content: String ->
                val styles = parseStyleAttribute(styleAttr)
                var spanStyle = SpanStyle()
                
                styles["color"]?.let { color ->
                    spanStyle = spanStyle.copy(color = parseColor(color))
                }
                
                styles["font-weight"]?.let { weight ->
                    if (weight == "bold" || weight.toIntOrNull()?.let { it >= 600 } == true) {
                        spanStyle = spanStyle.copy(fontWeight = FontWeight.Bold)
                    }
                }
                
                styles["font-style"]?.let { style ->
                    if (style == "italic") {
                        spanStyle = spanStyle.copy(fontStyle = FontStyle.Italic)
                    }
                }
                
                styles["text-decoration"]?.let { decoration ->
                    when (decoration) {
                        "underline" -> spanStyle = spanStyle.copy(textDecoration = TextDecoration.Underline)
                        "line-through" -> spanStyle = spanStyle.copy(textDecoration = TextDecoration.LineThrough)
                    }
                }
                
                styles["font-size"]?.let { size ->
                    val fontSize = parseFontSize(size)
                    if (fontSize > 0) {
                        spanStyle = spanStyle.copy(fontSize = fontSize.sp)
                    }
                }
                
                spanStyle
            },
            
            // 粗体标签
            "<(b|strong)\\s*>([^<]*)</(b|strong)>" to { _: String, content: String ->
                SpanStyle(fontWeight = FontWeight.Bold)
            },
            
            // 斜体标签
            "<(i|em)\\s*>([^<]*)</(i|em)>" to { _: String, content: String ->
                SpanStyle(fontStyle = FontStyle.Italic)
            },
            
            // 下划线标签
            "<u\\s*>([^<]*)</u>" to { _: String, content: String ->
                SpanStyle(textDecoration = TextDecoration.Underline)
            },
            
            // 删除线标签
            "<(s|strike|del)\\s*>([^<]*)</(s|strike|del)>" to { _: String, content: String ->
                SpanStyle(textDecoration = TextDecoration.LineThrough)
            },
            
            // 标题标签 h1-h6
            "<h([1-6])\\s*>([^<]*)</h[1-6]>" to { level: String, content: String ->
                val fontSize = when (level) {
                    "1" -> 24
                    "2" -> 22
                    "3" -> 20
                    "4" -> 18
                    "5" -> 16
                    "6" -> 14
                    else -> 16
                }
                SpanStyle(
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            
            // font标签颜色
            "<font\\s+color=\"([^\"]*)\">([^<]*)</font>" to { color: String, content: String ->
                SpanStyle(color = parseColor(color))
            }
        )
        
        // 处理所有模式
        for ((pattern, styleProvider) in patterns) {
            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            val matches = regex.findAll(processedHtml).toList()
            
            if (matches.isNotEmpty()) {
                var lastEnd = 0
                var indexOffset = 0
                
                for (match in matches) {
                    // 添加标签前的普通文本
                    if (match.range.first > lastEnd) {
                        val plainText = processedHtml.substring(lastEnd, match.range.first)
                        val cleanText = cleanHtml(plainText)
                        builder.append(cleanText)
                        indexOffset += cleanText.length
                    }
                    
                    val groups = match.groupValues
                    val content = when {
                        groups.size >= 3 && groups[2].isNotEmpty() -> groups[2]
                        groups.size >= 2 && groups[1].isNotEmpty() -> groups[1]
                        else -> match.value
                    }
                    
                    val startIndex = builder.length
                    builder.append(content)
                    val endIndex = builder.length
                    
                    // 应用样式
                    val style = if (groups.size >= 3) {
                        styleProvider(groups[1], groups[2])
                    } else {
                        styleProvider("", content)
                    }
                    
                    builder.addStyle(
                        style = style,
                        start = startIndex,
                        end = endIndex
                    )
                    
                    lastEnd = match.range.last + 1
                }
                
                // 添加剩余的普通文本
                if (lastEnd < processedHtml.length) {
                    val remainingText = processedHtml.substring(lastEnd)
                    builder.append(cleanHtml(remainingText))
                }
                
                // 更新处理后的HTML，移除已处理的标签
                processedHtml = regex.replace(processedHtml) { matchResult ->
                    val groups = matchResult.groupValues
                    when {
                        groups.size >= 3 && groups[2].isNotEmpty() -> groups[2]
                        groups.size >= 2 && groups[1].isNotEmpty() -> groups[1]
                        else -> matchResult.value
                    }
                }
            }
        }
        
        // 如果没有匹配到任何标签，直接添加清理后的文本
        if (patterns.none { (pattern, _) -> pattern.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(html) }) {
            builder.append(cleanHtml(processedHtml))
        }
    }
    
    /**
     * 解析style属性字符串
     */
    private fun parseStyleAttribute(styleAttr: String): Map<String, String> {
        val styles = mutableMapOf<String, String>()
        val declarations = styleAttr.split(";")
        
        for (declaration in declarations) {
            val parts = declaration.split(":")
            if (parts.size == 2) {
                val property = parts[0].trim()
                val value = parts[1].trim()
                styles[property] = value
            }
        }
        
        return styles
    }
    
    /**
     * 解析颜色值，支持更多颜色格式
     */
    private fun parseColor(colorStr: String): Color {
        return when {
            // 基本颜色名称
            colorStr.equals("red", ignoreCase = true) -> Color.Red
            colorStr.equals("blue", ignoreCase = true) -> Color.Blue
            colorStr.equals("green", ignoreCase = true) -> Color.Green
            colorStr.equals("black", ignoreCase = true) -> Color.Black
            colorStr.equals("white", ignoreCase = true) -> Color.White
            colorStr.equals("yellow", ignoreCase = true) -> Color.Yellow
            colorStr.equals("cyan", ignoreCase = true) -> Color.Cyan
            colorStr.equals("magenta", ignoreCase = true) -> Color.Magenta
            colorStr.equals("gray", ignoreCase = true) -> Color.Gray
            colorStr.equals("grey", ignoreCase = true) -> Color.Gray
            colorStr.equals("orange", ignoreCase = true) -> Color(0xFFFFA500)
            colorStr.equals("purple", ignoreCase = true) -> Color(0xFF800080)
            colorStr.equals("pink", ignoreCase = true) -> Color(0xFFFFC0CB)
            colorStr.equals("brown", ignoreCase = true) -> Color(0xFFA52A2A)
            
            // 十六进制颜色
            colorStr.startsWith("#") -> {
                try {
                    Color(android.graphics.Color.parseColor(colorStr))
                } catch (e: Exception) {
                    Color.Black
                }
            }
            
            // RGB颜色
            colorStr.startsWith("rgb(") -> {
                try {
                    val rgb = colorStr.removePrefix("rgb(").removeSuffix(")")
                    val values = rgb.split(",").map { it.trim().toInt() }
                    if (values.size == 3) {
                        Color(values[0], values[1], values[2])
                    } else {
                        Color.Black
                    }
                } catch (e: Exception) {
                    Color.Black
                }
            }
            
            // RGBA颜色
            colorStr.startsWith("rgba(") -> {
                try {
                    val rgba = colorStr.removePrefix("rgba(").removeSuffix(")")
                    val values = rgba.split(",").map { it.trim() }
                    if (values.size == 4) {
                        val r = values[0].toInt()
                        val g = values[1].toInt()
                        val b = values[2].toInt()
                        val a = (values[3].toFloat() * 255).toInt()
                        Color(r, g, b, a)
                    } else {
                        Color.Black
                    }
                } catch (e: Exception) {
                    Color.Black
                }
            }
            
            else -> Color.Black
        }
    }
    
    /**
     * 解析字体大小，支持多种单位
     */
    private fun parseFontSize(sizeStr: String): Int {
        return try {
            when {
                sizeStr.endsWith("px") -> sizeStr.removeSuffix("px").toInt()
                sizeStr.endsWith("sp") -> sizeStr.removeSuffix("sp").toInt()
                sizeStr.endsWith("dp") -> sizeStr.removeSuffix("dp").toInt()
                sizeStr.endsWith("pt") -> (sizeStr.removeSuffix("pt").toFloat() * 1.33f).toInt()
                sizeStr.endsWith("em") -> (sizeStr.removeSuffix("em").toFloat() * 16).toInt()
                sizeStr.endsWith("%") -> (sizeStr.removeSuffix("%").toFloat() * 16 / 100).toInt()
                else -> sizeStr.toInt()
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 清理HTML标签（保留文本内容）
     */
    private fun cleanHtml(html: String): String {
        return html.replace("<[^>]*>".toRegex(), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }
}
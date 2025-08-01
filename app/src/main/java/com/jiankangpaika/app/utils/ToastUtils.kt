package com.jiankangpaika.app.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

object ToastUtils {
    
    /**
     * 显示顶部Toast
     * @param context 上下文
     * @param message 消息内容
     * @param duration Toast显示时长
     */
    fun showTopToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        // 对于Android 10及以下版本，使用传统Toast
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val toast = Toast.makeText(context, message, duration)
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dpToPx(context, 50))
            android.util.Log.d("ToastUtils", "Android ${Build.VERSION.SDK_INT}: 设置顶部Toast: $message")
            toast.show()
        } else {
            // Android 11+版本，使用Snackbar实现顶部提示
            if (context is Activity) {
                showTopSnackbar(context, message, duration)
            } else {
                // 如果不是Activity上下文，使用默认Toast
                android.util.Log.d("ToastUtils", "Android ${Build.VERSION.SDK_INT}: 非Activity上下文，使用默认Toast: $message")
                Toast.makeText(context, message, duration).show()
            }
        }
    }
    
    /**
     * 使用Snackbar显示顶部提示
     * @param activity Activity上下文
     * @param message 消息内容
     * @param duration Toast显示时长
     */
    private fun showTopSnackbar(activity: Activity, message: String, duration: Int) {
        try {
            val rootView = activity.findViewById<android.view.View>(android.R.id.content)
            val snackbarDuration = if (duration == Toast.LENGTH_LONG) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
            
            val snackbar = Snackbar.make(rootView, message, snackbarDuration)
            
            // 使用现代化的半透明渐变背景样式
            styleModernSnackbar(snackbar, activity, "#E66C5CE7", "#E65B4FE3", Color.WHITE)
            
            // 设置Snackbar显示在顶部
            val snackbarView = snackbar.view
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.topMargin = dpToPx(activity, 60)
            params.leftMargin = dpToPx(activity, 20)
            params.rightMargin = dpToPx(activity, 20)
            params.width = FrameLayout.LayoutParams.MATCH_PARENT // 使用全宽以支持长文本
            params.height = FrameLayout.LayoutParams.WRAP_CONTENT // 高度自适应
            snackbarView.layoutParams = params
            
            android.util.Log.d("ToastUtils", "Android ${Build.VERSION.SDK_INT}: 使用Snackbar显示顶部提示: $message")
            snackbar.show()
        } catch (e: Exception) {
            // 如果Snackbar创建失败，回退到默认Toast
            android.util.Log.e("ToastUtils", "Snackbar创建失败，使用默认Toast: ${e.message}")
            Toast.makeText(activity, message, duration).show()
        }
    }
    
    /**
     * 显示自定义样式的顶部Toast
     * @param context 上下文
     * @param message 消息内容
     * @param duration Toast显示时长
     */
    fun showCustomTopToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        showTopToast(context, message, duration)
    }
    
    /**
     * 获取状态栏高度
     */
    private fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    /**
     * dp转px
     */
    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
    
    /**
     * 显示成功Toast（绿色背景）
     */
    fun showSuccessToast(context: Context, message: String) {
        showStyledToast(context, message, android.R.color.holo_green_dark)
    }
    
    /**
     * 显示错误Toast（红色背景）
     */
    fun showErrorToast(context: Context, message: String) {
        showStyledToast(context, message, android.R.color.holo_red_dark)
    }
    
    /**
     * 显示信息Toast（蓝色背景）
     */
    fun showInfoToast(context: Context, message: String) {
        showStyledToast(context, message, android.R.color.holo_blue_dark)
    }
    
    /**
     * 显示带颜色的Toast
     * 根据Android版本使用不同的实现方式
     */
    private fun showStyledToast(context: Context, message: String, colorRes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && context is Activity) {
            // Android 11+使用Snackbar，可以设置颜色
            showStyledSnackbar(context, message, colorRes)
        } else {
            // 其他情况使用顶部Toast
            showTopToast(context, message, Toast.LENGTH_SHORT)
        }
    }
    
    /**
     * 显示带颜色的Snackbar
     */
    private fun showStyledSnackbar(activity: Activity, message: String, colorRes: Int) {
        try {
            val rootView = activity.findViewById<android.view.View>(android.R.id.content)
            val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
            
            // 根据不同类型设置不同的半透明渐变色
            val (startColor, endColor, textColor) = when (colorRes) {
                android.R.color.holo_green_dark -> Triple("#E634C759", "#E630D158", Color.WHITE) // 成功 - 半透明绿色渐变
                android.R.color.holo_red_dark -> Triple("#E6FF3B30", "#E6FF2D92", Color.WHITE)   // 错误 - 半透明红色渐变
                else -> Triple("#E6FF9500", "#E6FF6D00", Color.WHITE) // 默认 - 半透明橙色渐变
            }
            
            // 使用现代化渐变样式
            styleModernSnackbar(snackbar, activity, startColor, endColor, textColor)
            
            // 设置显示在顶部
            val snackbarView = snackbar.view
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.topMargin = dpToPx(activity, 60)
            params.leftMargin = dpToPx(activity, 20)
            params.rightMargin = dpToPx(activity, 20)
            params.width = FrameLayout.LayoutParams.MATCH_PARENT // 使用全宽以支持长文本
            params.height = FrameLayout.LayoutParams.WRAP_CONTENT // 高度自适应
            snackbarView.layoutParams = params
            
            snackbar.show()
        } catch (e: Exception) {
            // 回退到普通Toast
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 现代化Snackbar样式 - 使用渐变背景和更好的视觉效果
     */
    private fun styleModernSnackbar(snackbar: Snackbar, context: Context, startColor: String, endColor: String, textColor: Int) {
        val snackbarView = snackbar.view
        
        // 创建半透明渐变背景
         val gradientDrawable = GradientDrawable(
             GradientDrawable.Orientation.TL_BR, // 从左上到右下的对角线渐变
             intArrayOf(
                 Color.parseColor(startColor),
                 Color.parseColor(endColor)
             )
         ).apply {
             cornerRadius = dpToPx(context, 24).toFloat() // 更大的圆角
             // 添加半透明白色描边效果
             setStroke(dpToPx(context, 2), Color.parseColor("#40FFFFFF"))
         }
        
        snackbarView.background = gradientDrawable
        
        // 设置阴影和立体效果
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            snackbarView.elevation = dpToPx(context, 8).toFloat()
            snackbarView.translationZ = dpToPx(context, 2).toFloat()
        }
        
        // 设置更大的内边距
        val horizontalPadding = dpToPx(context, 20)
        val verticalPadding = dpToPx(context, 16)
        snackbarView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        
        // 设置文字样式
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView?.apply {
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f) // 稍大的字体
            maxLines = Integer.MAX_VALUE // 移除行数限制，支持自适应长度
            isSingleLine = false // 允许多行显示
            // 设置字体为粗体
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            // 添加文字阴影效果
            setShadowLayer(2f, 1f, 1f, Color.parseColor("#40000000"))
        }
        
        // 移除固定最小高度，让高度完全自适应内容
        // snackbarView.minimumHeight = dpToPx(context, 56)
        
        // 添加缩放动画效果
        snackbarView.scaleX = 0.9f
        snackbarView.scaleY = 0.9f
        snackbarView.alpha = 0.8f
        snackbarView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(200)
            .start()
    }
    
    /**
     * 判断颜色是否为浅色
     */
    private fun isLightColor(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val brightness = (red * 299 + green * 587 + blue * 114) / 1000
        return brightness > 128
    }
}
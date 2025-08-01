package com.jiankangpaika.app.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 截屏工具类
 * 负责页面截屏和保存到相册功能
 */
object ScreenshotUtils {
    private const val TAG = "ScreenshotUtils"
    
    /**
     * 截取当前Activity的屏幕并保存到相册
     * @param activity 当前Activity
     * @return 是否成功
     */
    suspend fun takeScreenshot(activity: Activity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📸 [截屏] 开始截屏")
                
                // 获取根视图
                val rootView = activity.window.decorView.rootView
                
                // 创建bitmap
                val bitmap = withContext(Dispatchers.Main) {
                    createBitmapFromView(rootView)
                }
                
                if (bitmap != null) {
                    // 保存到相册
                    val success = saveBitmapToGallery(activity, bitmap)
                    bitmap.recycle() // 释放内存
                    
                    if (success) {
                        Log.i(TAG, "✅ [截屏] 截屏保存成功")
                    } else {
                        Log.w(TAG, "⚠️ [截屏] 截屏保存失败")
                    }
                    
                    success
                } else {
                    Log.e(TAG, "❌ [截屏] 创建bitmap失败")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 [截屏] 截屏过程中发生异常: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 从View创建Bitmap
     */
    private fun createBitmapFromView(view: View): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "❌ [截屏] 创建bitmap异常: ${e.message}")
            null
        }
    }
    
    /**
     * 保存Bitmap到相册
     */
    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "ShenHuoBao_CheckIn_$timestamp.jpg"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ShenHuoBao")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            if (uri != null) {
                var outputStream: OutputStream? = null
                try {
                    outputStream = resolver.openOutputStream(uri)
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                        }
                        
                        Log.d(TAG, "✅ [截屏] 图片保存成功: $filename")
                        true
                    } else {
                        Log.e(TAG, "❌ [截屏] 无法打开输出流")
                        false
                    }
                } finally {
                    outputStream?.close()
                }
            } else {
                Log.e(TAG, "❌ [截屏] 无法创建媒体URI")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [截屏] 保存图片异常: ${e.message}")
            false
        }
    }
}
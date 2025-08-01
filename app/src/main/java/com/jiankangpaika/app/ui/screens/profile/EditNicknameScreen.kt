package com.jiankangpaika.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.services.UserService
import android.widget.Toast
import com.jiankangpaika.app.utils.ToastUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNicknameScreen(
    currentNickname: String,
    onNavigateBack: () -> Unit,
    onNicknameSaved: (String) -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部导航栏
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "修改昵称",
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.Black
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (nickname.isNotBlank() && nickname != currentNickname) {
                            val validationError = validateNickname(nickname)
                            if (validationError.isEmpty()) {
                                isLoading = true
                                errorMessage = ""
                                
                                // 使用协程调用API更新昵称
                                coroutineScope.launch {
                                    try {
                                        // 获取用户信息
                                        val userId = UserManager.getUserId(context) ?: ""
                                        val token = UserManager.getToken(context) ?: ""
                                        
                                        if (userId.isEmpty() || token.isEmpty()) {
                                            errorMessage = "用户信息不完整，请重新登录"
                                            isLoading = false
                                            return@launch
                                        }
                                        
                                        // 调用API更新昵称
                                        val result = UserService.updateNickname(context, userId, nickname)
                                        
                                        if (result != null) {
                                            // API调用成功，保存到本地
                                            saveNickname(context, nickname)
                                            // 只调用onNavigateBack，避免双重导航
                                            onNavigateBack()
                                        } else {
                                            // API调用失败，错误信息已通过Toast显示
                                            // 不需要额外处理
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "网络错误：${e.message}"
                                        ToastUtils.showSuccessToast(context, errorMessage)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                errorMessage = validationError
                            }
                        }
                    },
                    enabled = !isLoading && nickname.isNotBlank() && nickname != currentNickname
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "保存",
                            color = if (nickname.isNotBlank() && nickname != currentNickname) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Gray
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )
        
        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入框卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "昵称",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { 
                            nickname = it
                            // 实时验证并清除错误信息
                            if (errorMessage.isNotEmpty()) {
                                val validationError = validateNickname(it)
                                if (validationError.isEmpty()) {
                                    errorMessage = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "请输入昵称",
                                color = Color(0xFF9CA3AF)
                            )
                        },
                        singleLine = true,
                        isError = errorMessage.isNotEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (errorMessage.isNotEmpty()) Color.Red else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (errorMessage.isNotEmpty()) Color.Red else Color(0xFFE5E7EB),
                            focusedTextColor = Color(0xFF161823),
                            unfocusedTextColor = Color(0xFF161823),
                            errorBorderColor = Color.Red
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    // 错误信息显示
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // 字符限制提示
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "昵称长度为2-20个字符",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                        
                        Text(
                            text = "${nickname.length}/20",
                            fontSize = 12.sp,
                            color = if (nickname.length > 20) {
                                Color.Red
                            } else {
                                Color(0xFF9CA3AF)
                            }
                        )
                    }
                }
            }
            
            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3F4F6)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "温馨提示",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "• 昵称长度为2-20个字符\n• 不能包含特殊符号和敏感词汇\n• 修改后将在个人资料中显示",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// 保存昵称到UserManager
private fun saveNickname(context: Context, nickname: String) {
    // 使用UserManager保存昵称，确保与PersonalSettingsScreen中的获取方式一致
    UserManager.updateNickname(context, nickname)
    
    // 同时保存到本地SharedPreferences作为备份
    val sharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    sharedPreferences.edit {
        putString("user_nickname", nickname)
    }
}

// 从SharedPreferences获取昵称
fun getNickname(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    return sharedPreferences.getString("user_nickname", "健康派卡") ?: "健康派卡"
}

// 验证昵称是否有效
private fun validateNickname(nickname: String): String {
    // 检查长度
    if (nickname.length < 2) {
        return "昵称长度不能少于2个字符"
    }
    if (nickname.length > 20) {
        return "昵称长度不能超过20个字符"
    }
    
    // 检查特殊符号
    val specialChars = "[!@#$%^&*()_+\\-=\\[\\]{};':,.<>?/|`~\"]"
    if (nickname.matches(Regex(".*$specialChars.*"))) {
        return "昵称不能包含特殊符号"
    }
    
    // 检查敏感词汇
    val sensitiveWords = listOf(
        "管理员", "admin", "系统", "客服", "官方", "test", "测试",
        "fuck", "shit", "damn", "傻逼", "操", "草", "妈的", "卧槽"
    )
    
    for (word in sensitiveWords) {
        if (nickname.contains(word, ignoreCase = true)) {
            return "昵称包含敏感词汇，请重新输入"
        }
    }
    
    return "" // 验证通过
}
package com.jiankangpaika.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.jiankangpaika.app.services.UserService
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BindEmailScreen(
    onNavigateBack: () -> Unit,
    initialEmail: String = ""
) {
    val context = LocalContext.current
    val currentBindedEmail = getBindedEmail(context)
    var email by remember { mutableStateOf(initialEmail.ifEmpty { currentBindedEmail }) }
    var showBindSuccessMessage by remember { mutableStateOf(false) }
    var showUnBindSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isBinding by remember { mutableStateOf(false) }
    var isUnbinding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hasBindedEmail = currentBindedEmail.isNotEmpty()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部导航栏
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "绑定邮箱",
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
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 说明文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
            ) {
                Text(
                    text = "绑定邮箱后，可以接收重要通知",
                    fontSize = 14.sp,
                    color = Color(0xFF1E40AF),
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // 邮箱输入
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (hasBindedEmail) "当前绑定邮箱" else "邮箱地址",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (hasBindedEmail) {
                        // 显示已绑定的邮箱（只读）
                        Text(
                            text = email,
                            fontSize = 16.sp,
                            color = Color(0xFF374151),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        // 显示邮箱输入框
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("请输入邮箱地址") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                    }
                }
            }
            
            // 错误信息
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            // 操作按钮
            if (hasBindedEmail) {
                // 解绑按钮
                OutlinedButton(
                    onClick = {
                        // 解绑邮箱
                        isUnbinding = true
                        errorMessage = ""
                        
                        coroutineScope.launch {
                            try {
                                // 获取用户信息
                                val userId = UserManager.getUserId(context) ?: ""
                                val token = UserManager.getToken(context) ?: ""
                                
                                if (userId.isEmpty() || token.isEmpty()) {
                                    errorMessage = "用户信息获取失败，请重新登录"
                                    isUnbinding = false
                                    return@launch
                                }
                                
                                // 调用邮箱解绑API
                                val result = UserService.updateEmail(context, userId, currentBindedEmail, "unbind")
                                
                                if (result != null) {
                                    // 解绑成功，清除本地存储
                                    saveBindedEmail(context, "")
                                    UserManager.updateEmail(context, "")
                                    showUnBindSuccessMessage = true
                                    isUnbinding = false
                                } else {
                                    // 解绑失败，错误信息已通过Toast显示
                                    isUnbinding = false
                                }
                            } catch (e: Exception) {
                                errorMessage = "网络连接失败，请检查网络后重试"
                                isUnbinding = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    enabled = !isUnbinding
                ) {
                    if (isUnbinding) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.Red
                            )
                            Text(
                                text = "解绑中...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "解绑邮箱",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // 绑定按钮
                Button(
                    onClick = {
                        if (isValidEmail(email)) {
                            // 直接绑定邮箱
                            isBinding = true
                            errorMessage = ""
                            
                            coroutineScope.launch {
                                try {
                                    // 获取用户信息
                                    val userId = UserManager.getUserId(context) ?: ""
                                    val token = UserManager.getToken(context) ?: ""
                                    
                                    if (userId.isEmpty() || token.isEmpty()) {
                                        errorMessage = "用户信息获取失败，请重新登录"
                                        isBinding = false
                                        return@launch
                                    }
                                    
                                    // 调用邮箱绑定API
                                    val result = UserService.updateEmail(context, userId, email, "bind")
                                    
                                    if (result != null) {
                                        // 绑定成功，保存到本地
                                        saveBindedEmail(context, email)
                                        UserManager.updateEmail(context, email)
                                        showBindSuccessMessage = true
                                        isBinding = false
                                    } else {
                                        // 绑定失败，错误信息已通过Toast显示
                                        isBinding = false
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "网络连接失败，请检查网络后重试"
                                    isBinding = false
                                }
                            }
                        } else {
                            errorMessage = "请输入正确的邮箱地址"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B)
                    ),
                    enabled = isValidEmail(email) && !isBinding
                ) {
                    if (isBinding) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text(
                                text = "绑定中...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "绑定邮箱",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // 邮箱安全提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "安全提示",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• 请使用常用邮箱，确保能及时收到通知\n• 绑定后可用于账号安全验证和密码找回\n• 如需更换邮箱，请先解绑当前邮箱",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
    
    // 成功提示
    if (showBindSuccessMessage) {
        LaunchedEffect(showBindSuccessMessage) {
            delay(1500)
            showBindSuccessMessage = false
            onNavigateBack()
        }

        ToastUtils.showSuccessToast(context, "邮箱绑定成功")
    }  else if (showUnBindSuccessMessage) {
        LaunchedEffect(showUnBindSuccessMessage) {
            delay(1500)
            showUnBindSuccessMessage = false
            onNavigateBack()
        }

        ToastUtils.showSuccessToast(context, "邮箱解绑成功")
    }
}

// 验证邮箱格式
fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

// 保存绑定的邮箱到本地存储
fun saveBindedEmail(context: Context, email: String) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("binded_email", email)
    editor.apply()
}

// 获取已绑定的邮箱
fun getBindedEmail(context: Context): String {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("binded_email", "") ?: ""
}

// 验证邮箱验证码（模拟）
fun verifyEmailCode(code: String): Boolean {
    // 这里应该调用实际的验证码验证API
    // 目前返回固定值用于测试
    return code == "123456"
}

// 脱敏显示邮箱
fun maskEmail(email: String): String {
    if (email.isEmpty()) return email
    
    val parts = email.split("@")
    if (parts.size != 2) return email
    
    val username = parts[0]
    val domain = parts[1]
    
    return if (username.length > 3) {
        "${username.substring(0, 2)}***@$domain"
    } else {
        "***@$domain"
    }
}
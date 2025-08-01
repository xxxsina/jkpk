package com.jiankangpaika.app.ui.screens.auth

import android.nfc.Tag
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import com.jiankangpaika.app.data.model.LoginRequest
import com.jiankangpaika.app.data.model.LoginResponse
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.constants.ApiConfig

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToMobileLogin: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 登录函数
    val performLogin: () -> Unit = lambda@{
        if (username.isBlank()) {
            ToastUtils.showErrorToast(context, "请输入用户名")
            return@lambda
        }
        
        // 验证用户名长度（3-50个字符）
        if (username.length < 3) {
            ToastUtils.showErrorToast(context, "用户名长度不能少于3个字符")
            return@lambda
        }
        
        if (username.length > 50) {
            ToastUtils.showErrorToast(context, "用户名长度不能超过50个字符")
            return@lambda
        }
        
        if (password.isBlank()) {
            ToastUtils.showErrorToast(context, "请输入密码")
            return@lambda
        }
        
        // 验证密码长度（6-50位）
        if (password.length < 6) {
            ToastUtils.showErrorToast(context, "密码长度不能少于6位")
            return@lambda
        }
        
        if (password.length > 50) {
            ToastUtils.showErrorToast(context, "密码长度不能超过50位")
            return@lambda
        }
        
        isLoading = true
        
        coroutineScope.launch {
            try {
                val loginRequest = LoginRequest(username, password)

                // 使用POST请求发送JSON数据
                val result = NetworkUtils.postJson(ApiConfig.User.LOGIN, loginRequest)

                when (result) {
                    is NetworkResult.Success -> {
                        val loginResponse = NetworkUtils.parseJson<LoginResponse>(result.data)

                        if (loginResponse != null && loginResponse.isSuccess()) {
                            val loginData = loginResponse.getUserData()!!
                            val userInfo = loginData.user_info
                            
                            // 设置默认头像URL（如果服务器没有返回头像）
                            val defaultAvatarUrl = userInfo.avatar ?: "android.resource://com.jiankangpaika.app/drawable/logo"

                            // 保存用户信息
                            UserManager.saveUserInfo(
                                context = context,
                                userId = userInfo.id.toString(),
                                username = userInfo.username,
                                nickname = userInfo.nickname,
                                phone = userInfo.mobile,
                                email = userInfo.email,
                                avatarUrl = defaultAvatarUrl,
                                token = loginData.token
                            )
                            
                            ToastUtils.showSuccessToast(context, "登录成功")
                            onLoginSuccess()
                        } else {
                            // 处理失败情况 - 直接使用 message 字段作为错误信息
                            val errorMessage = loginResponse?.message ?: "登录失败"
                            ToastUtils.showErrorToast(context, errorMessage)
                        }
                    }
                    is NetworkResult.Error -> {
                        val errorMessage = result.parseApiErrorMessage()
                        ToastUtils.showErrorToast(context, errorMessage)
                    }
                    is NetworkResult.Exception -> {
                        ToastUtils.showErrorToast(context, "网络异常：${result.exception.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginScreen", "登录异常", e)
                ToastUtils.showErrorToast(context, "登录异常：${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "健康派卡",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = performLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B6B)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("登录中...")
            } else {
                Text("登录")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 将两个按钮放在同一行并右对齐
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onNavigateToMobileLogin,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF5E5D5D)
                )
            ) {
                Text("短信登录/注册")
            }
            
            TextButton(
                onClick = onNavigateToRegister,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF5E5D5D)
                )
            ) {
                Text("立即注册")
            }
        }
    }
}
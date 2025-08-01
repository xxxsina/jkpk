package com.jiankangpaika.app.ui.screens.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.jiankangpaika.app.data.model.RegisterRequest
import com.jiankangpaika.app.data.model.RegisterResponse
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.constants.ApiConfig

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToUserAgreement: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
    onBackToLogin: () -> Unit = {},
    initialUsername: String = "",
    initialPassword: String = "",
    initialConfirmPassword: String = "",
    initialAgreeToTerms: Boolean = false
) {
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var confirmPassword by remember { mutableStateOf(initialConfirmPassword) }
    var agreeToTerms by remember { mutableStateOf(initialAgreeToTerms) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 注册函数
    val performRegister: () -> Unit = lambda@{
        if (username.isBlank()) {
            ToastUtils.showErrorToast(context, "请输入账号")
            return@lambda
        }
        
        // 验证账号长度（2-50个字符）
        if (username.length < 2) {
            ToastUtils.showErrorToast(context, "账号长度不能少于2个字符")
            return@lambda
        }
        
        if (username.length > 50) {
            ToastUtils.showErrorToast(context, "账号长度不能超过50个字符")
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
        
        if (confirmPassword.isBlank()) {
            ToastUtils.showErrorToast(context, "请确认密码")
            return@lambda
        }
        
        if (password != confirmPassword) {
            ToastUtils.showErrorToast(context, "两次密码输入不一致")
            return@lambda
        }
        
        if (!agreeToTerms) {
            ToastUtils.showErrorToast(context, "请同意用户协议")
            return@lambda
        }
        
        isLoading = true
        
        coroutineScope.launch {
            try {
                val registerRequest = RegisterRequest(username, password, confirmPassword)

                // 使用POST请求发送JSON数据
                val result = NetworkUtils.postJson(ApiConfig.User.REGISTER, registerRequest)
                
                Log.d("RegisterScreen", "注册响应: ${result}")
                
                when (result) {
                    is NetworkResult.Success -> {
                        val registerResponse = NetworkUtils.parseJson<RegisterResponse>(result.data)

                        if (registerResponse != null && registerResponse.isSuccess()) {
                            val registerData = registerResponse.getUserData()!!
                            val userInfo = registerData.user_info
                            
                            // 设置默认头像URL（如果服务器没有返回头像）
                            val defaultAvatarUrl = userInfo.avatar ?: "android.resource://com.jiankangpaika.app/drawable/logo"
                            
                            // 保存用户信息
                            UserManager.saveUserInfo(
                                context = context,
                                userId = userInfo.id.toInt().toString(),
                                username = userInfo.username,
                                nickname = userInfo.nickname,
                                phone = userInfo.mobile,
                                email = userInfo.email,
                                avatarUrl = defaultAvatarUrl,
                                token = registerData.token
                            )
                            
                            ToastUtils.showSuccessToast(context, "注册成功")
                            onRegisterSuccess()
                        } else {
                            // 处理失败情况 - 直接使用 message 字段作为错误信息
                            val errorMessage = registerResponse?.message ?: "注册失败"
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
                Log.e("RegisterScreen", "注册异常", e)
                ToastUtils.showErrorToast(context, "注册异常：${e.message}")
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
            text = "注册账号",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("账号") },
            placeholder = { Text("支持手机、邮箱、用户名") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            placeholder = { Text("6-50位密码") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("确认密码") },
            placeholder = { Text("再次输入密码") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = agreeToTerms,
                onCheckedChange = { agreeToTerms = it },
                enabled = !isLoading
            )
            
            Text(text = "我同意")
            
            ClickableText(
                text = AnnotatedString("用户协议"),
                onClick = { onNavigateToUserAgreement(username, password, confirmPassword, agreeToTerms) },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = performRegister,
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
                Text("注册中...")
            } else {
                Text("注册")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 将返回登录按钮右对齐
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onBackToLogin,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF5E5D5D)
                )
            ) {
                Text("返回登录")
            }
        }
    }
}
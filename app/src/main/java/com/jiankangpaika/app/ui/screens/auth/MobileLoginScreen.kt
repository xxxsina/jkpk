package com.jiankangpaika.app.ui.screens.auth

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.jiankangpaika.app.data.model.*
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.constants.ApiConfig

@Composable
fun MobileLoginScreen(
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToPasswordLogin: () -> Unit = {}
) {
    var mobile by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var captchaCode by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var captchaImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSendingSms by remember { mutableStateOf(false) }
    var smsCountdown by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 获取图形验证码
    val getCaptcha: () -> Unit = {
        coroutineScope.launch {
            try {
                val result = NetworkUtils.get(ApiConfig.User.GET_CAPTCHA)
                
                when (result) {
                    is NetworkResult.Success -> {
                        val captchaResponse = NetworkUtils.parseJson<CaptchaResponse>(result.data)
                        
                        if (captchaResponse != null && captchaResponse.isSuccess()) {
                            val captchaData = captchaResponse.getCaptchaData()!!
                            sessionId = captchaData.session_id
                            
                            // 解析base64图片
                            try {
                                val imageData = captchaData.image.substringAfter("base64,")
                                val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                captchaImage = bitmap.asImageBitmap()
                            } catch (e: Exception) {
                                Log.e("MobileLoginScreen", "解析验证码图片失败", e)
                                ToastUtils.showErrorToast(context, "验证码图片加载失败")
                            }
                        } else {
                            val errorMessage = captchaResponse?.message ?: "获取验证码失败"
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
                Log.e("MobileLoginScreen", "获取验证码异常", e)
                ToastUtils.showErrorToast(context, "获取验证码异常：${e.message}")
            }
        }
    }
    
    // 发送短信验证码
    val sendSmsCode: () -> Unit = lambda@{
        if (mobile.isBlank()) {
            ToastUtils.showErrorToast(context, "请输入手机号")
            return@lambda
        }
        
        // 验证手机号格式
        if (!mobile.matches(Regex("^1[3-9]\\d{9}$"))) {
            ToastUtils.showErrorToast(context, "请输入正确的手机号")
            return@lambda
        }
        
//        if (captchaCode.isBlank()) {
//            ToastUtils.showErrorToast(context, "请输入图形验证码")
//            return@lambda
//        }
//
//        if (sessionId.isBlank()) {
//            ToastUtils.showErrorToast(context, "请先获取图形验证码")
//            return@lambda
//        }
        
        isSendingSms = true
        
        coroutineScope.launch {
            try {
                val sendSmsRequest = SendSmsRequest(mobile, captchaCode, sessionId, "login")
                val result = NetworkUtils.postJson(ApiConfig.User.SEND_SMS, sendSmsRequest)
                
                when (result) {
                    is NetworkResult.Success -> {
                        val sendSmsResponse = NetworkUtils.parseJson<SendSmsResponse>(result.data)
                        
                        if (sendSmsResponse != null && sendSmsResponse.isSuccess()) {
                            ToastUtils.showSuccessToast(context, "短信验证码发送成功")
                            // 开始倒计时
                            smsCountdown = 60
                            // 启动倒计时协程
                            coroutineScope.launch {
                                while (smsCountdown > 0) {
                                    kotlinx.coroutines.delay(1000)
                                    smsCountdown--
                                }
                            }
                        } else {
                            val errorMessage = sendSmsResponse?.message ?: "发送短信验证码失败"
                            ToastUtils.showErrorToast(context, errorMessage)
                            // 重新获取图形验证码
                            getCaptcha()
                        }
                    }
                    is NetworkResult.Error -> {
                        val errorMessage = result.parseApiErrorMessage()
                        ToastUtils.showErrorToast(context, errorMessage)
                        // 重新获取图形验证码
                        getCaptcha()
                    }
                    is NetworkResult.Exception -> {
                        ToastUtils.showErrorToast(context, "网络异常：${result.exception.message}")
                        // 重新获取图形验证码
                        getCaptcha()
                    }
                }
            } catch (e: Exception) {
                Log.e("MobileLoginScreen", "发送短信验证码异常", e)
                ToastUtils.showErrorToast(context, "发送短信验证码异常：${e.message}")
                // 重新获取图形验证码
                getCaptcha()
            } finally {
                isSendingSms = false
            }
        }
    }
    
    // 手机短信登录函数
    val performMobileLogin: () -> Unit = lambda@{
        if (mobile.isBlank()) {
            ToastUtils.showErrorToast(context, "请输入手机号")
            return@lambda
        }
        
        // 验证手机号格式
        if (!mobile.matches(Regex("^1[3-9]\\d{9}$"))) {
            ToastUtils.showErrorToast(context, "请输入正确的手机号")
            return@lambda
        }
        
        if (smsCode.isBlank()) {
            ToastUtils.showErrorToast(context, "请输入短信验证码")
            return@lambda
        }
        
        // 验证短信验证码格式（6位数字）
        if (!smsCode.matches(Regex("^\\d{6}$"))) {
            ToastUtils.showErrorToast(context, "请输入6位数字验证码")
            return@lambda
        }
        
//        if (captchaCode.isBlank()) {
//            ToastUtils.showErrorToast(context, "请输入图形验证码")
//            return@lambda
//        }
//
//        if (sessionId.isBlank()) {
//            ToastUtils.showErrorToast(context, "请先获取图形验证码")
//            return@lambda
//        }
        
        isLoading = true
        
        coroutineScope.launch {
            try {
                val mobileLoginRequest = MobileLoginRequest(mobile, smsCode, captchaCode, sessionId)
                val result = NetworkUtils.postJson(ApiConfig.User.LOGIN_MOBILE, mobileLoginRequest)
                
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
                            // 重新获取图形验证码
                            getCaptcha()
                        }
                    }
                    is NetworkResult.Error -> {
                        val errorMessage = result.parseApiErrorMessage()
                        ToastUtils.showErrorToast(context, errorMessage)
                        // 重新获取图形验证码
                        getCaptcha()
                    }
                    is NetworkResult.Exception -> {
                        ToastUtils.showErrorToast(context, "网络异常：${result.exception.message}")
                        // 重新获取图形验证码
                        getCaptcha()
                    }
                }
            } catch (e: Exception) {
                Log.e("MobileLoginScreen", "登录异常", e)
                ToastUtils.showErrorToast(context, "登录异常：${e.message}")
                // 重新获取图形验证码
                getCaptcha()
            } finally {
                isLoading = false
            }
        }
    }
    
    // 页面加载时获取验证码
//    LaunchedEffect(Unit) {
//        getCaptcha()
//    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        Text(
            text = "健康派卡",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFEF3C7)
            )
        ) {
            Text(
                text = "如果手机号还未注册，系统会根据您填入的手机号自动注册",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD97706)
            )
        }

//        Text(
//            text = "手机短信登录",
//            style = MaterialTheme.typography.titleMedium,
//            color = MaterialTheme.colorScheme.primary
//        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 手机号输入框
        OutlinedTextField(
            value = mobile,
            onValueChange = { newValue ->
                // 只允许输入数字，并且限制长度为11位
                val filteredValue = newValue.filter { it.isDigit() }.take(11)
                mobile = filteredValue
            },
            label = { Text("手机号") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = mobile.isNotEmpty() && !mobile.matches(Regex("^1[3-9]\\d{9}$")),
            supportingText = {
                if (mobile.isNotEmpty() && !mobile.matches(Regex("^1[3-9]\\d{9}$"))) {
                    Text(
                        text = "请输入正确的手机号码",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
        
//        Spacer(modifier = Modifier.height(16.dp))
        
        // 图形验证码输入框
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            OutlinedTextField(
//                value = captchaCode,
//                onValueChange = { captchaCode = it },
//                label = { Text("图形验证码") },
//                modifier = Modifier.weight(1f),
//                enabled = !isLoading,
//                singleLine = true
//            )
//
//            Spacer(modifier = Modifier.width(8.dp))
//
//            // 图形验证码图片
//            Card(
//                modifier = Modifier
//                    .size(width = 120.dp, height = 56.dp)
//                    .clickable { getCaptcha() },
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
//            ) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    if (captchaImage != null) {
//                        Image(
//                            bitmap = captchaImage!!,
//                            contentDescription = "图形验证码",
//                            modifier = Modifier.fillMaxSize(),
//                            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
//                        )
//                    } else {
//                        Text(
//                            text = "点击获取\n验证码",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                }
//            }
//        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 短信验证码输入框和发送按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = smsCode,
                onValueChange = { smsCode = it },
                label = { Text("短信验证码") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 发送短信验证码按钮
            Button(
                onClick = sendSmsCode,
                enabled = !isSendingSms && !isLoading && smsCountdown == 0,
                modifier = Modifier.width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B6B)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                if (isSendingSms) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else if (smsCountdown > 0) {
                    Text(
                        text = "${smsCountdown}s",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "获取验证码",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 登录按钮
        Button(
            onClick = performMobileLogin,
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
                Text(
                    text = "登录中...",
                    fontSize = 18.sp
                )
            } else {
                Text(
                    text = "登录",
                    fontSize = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 将两个按钮放在同一行并右对齐
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.End
//        ) {
//            TextButton(
//                onClick = onNavigateToPasswordLogin,
//                enabled = !isLoading,
//                colors = ButtonDefaults.textButtonColors(
//                    contentColor = Color(0xFF5E5D5D)
//                )
//            ) {
//                Text("密码登录")
//            }

//            TextButton(
//                onClick = onNavigateToRegister,
//                enabled = !isLoading,
//                colors = ButtonDefaults.textButtonColors(
//                    contentColor = Color(0xFF5E5D5D)
//                )
//            ) {
//                Text("立即注册")
//            }
//        }
    }
}
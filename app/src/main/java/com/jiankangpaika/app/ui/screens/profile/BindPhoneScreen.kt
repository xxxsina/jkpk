package com.jiankangpaika.app.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.graphics.BitmapFactory
import android.nfc.Tag
import android.util.Base64
import android.util.Log
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.services.UserService
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.data.model.CaptchaResponse
import com.jiankangpaika.app.data.model.SendSmsRequest
import com.jiankangpaika.app.data.model.SendSmsResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BindPhoneScreen(
    onNavigateBack: () -> Unit,
    initialPhone: String = ""
) {
    val context = LocalContext.current
    
    // 获取当前绑定的手机号
    val currentBindedPhone = UserManager.getPhone(context) ?: ""
    Log.d("BindPhoneScreen", "currentBindedPhone = $currentBindedPhone")
    var phoneNumber by remember { mutableStateOf(initialPhone.ifEmpty { currentBindedPhone }) }
    var errorMessage by remember { mutableStateOf("") }
    var isBinding by remember { mutableStateOf(false) }
    var isUnbinding by remember { mutableStateOf(false) }
    
    // 短信验证码相关状态
    var smsCode by remember { mutableStateOf("") }
    var isSendingSms by remember { mutableStateOf(false) }
    var smsCountdown by remember { mutableStateOf(0) }
    
    // 图形验证码相关状态
    var captchaCode by remember { mutableStateOf("") }
    var captchaSessionId by remember { mutableStateOf("") }
    var captchaImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    // 操作模式：bind（绑定）或 unbind（解绑）
    var operationMode by remember { mutableStateOf(if (currentBindedPhone.isNotEmpty()) "unbind" else "bind") }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 倒计时效果
    LaunchedEffect(smsCountdown) {
        if (smsCountdown > 0) {
            delay(1000)
            smsCountdown--
        }
    }
    
    // 初始化时加载图形验证码
    LaunchedEffect(Unit) {
        
        loadCaptcha(
            context = context,
            onSuccess = { sessionId, imageBitmap ->
                captchaSessionId = sessionId
                captchaImage = imageBitmap
                captchaCode = ""
            },
            onError = { error ->
                errorMessage = error
            }
        )
    }
    

    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部导航栏
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = if (currentBindedPhone.isNotEmpty()) "手机号管理" else "绑定手机",
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
                    text = if (currentBindedPhone.isNotEmpty()) 
                        "当前绑定手机号：${formatPhoneNumber(currentBindedPhone)}" 
                    else 
                        "绑定手机号后，可以接收重要通知",
                    fontSize = 14.sp,
                    color = Color(0xFF1E40AF),
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // 手机号输入
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "手机号码",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { 
                            if (currentBindedPhone.isEmpty() && it.length <= 11) {
                                phoneNumber = it
                                errorMessage = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (currentBindedPhone.isNotEmpty()) "当前绑定手机号" else "请输入手机号码") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        enabled = currentBindedPhone.isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            disabledBorderColor = Color(0xFFE5E7EB),
                            disabledTextColor = Color(0xFF6B7280)
                        )
                    )
                }
            }
            
            // 图形验证码输入
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "图形验证码",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = captchaCode,
                            onValueChange = { 
                                captchaCode = it
                                errorMessage = ""
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("请输入验证码") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                        
                        // 图形验证码图片
                        Card(
                            modifier = Modifier
                                .size(120.dp, 56.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        loadCaptcha(
                                            context = context,
                                            onSuccess = { sessionId, imageBitmap ->
                                                captchaSessionId = sessionId
                                                captchaImage = imageBitmap
                                                captchaCode = ""
                                            },
                                            onError = { error ->
                                                errorMessage = error
                                            }
                                        )
                                    }
                                },
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (captchaImage != null) {
                                    Image(
                                        bitmap = captchaImage!!,
                                        contentDescription = "图形验证码",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds
                                    )
                                } else {
                                    Text(
                                        text = "点击获取\n验证码",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 短信验证码输入
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "短信验证码",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = smsCode,
                            onValueChange = { 
                                if (it.length <= 6) {
                                    smsCode = it
                                    errorMessage = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("请输入短信验证码") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                        
                        // 发送验证码按钮
                        Button(
                            onClick = {
                                if (isValidPhoneNumber(phoneNumber) && captchaCode.isNotEmpty()) {
                                    isSendingSms = true
                                    coroutineScope.launch {
                                        sendSmsCode(
                                            context,
                                            phoneNumber,
                                            captchaCode,
                                            captchaSessionId,
                                            operationMode,
                                            onSuccess = {
                                                isSendingSms = false
                                                smsCountdown = 60
                                                ToastUtils.showSuccessToast(context, "验证码已发送")
                                            },
                                            onError = { error ->
                                                isSendingSms = false
                                                errorMessage = error
                                            }
                                        )
                                    }
                                } else {
                                    errorMessage = "请先输入正确的手机号和图形验证码"
                                }
                            },
                            enabled = !isSendingSms && smsCountdown == 0 && isValidPhoneNumber(phoneNumber) && captchaCode.isNotEmpty(),
                            modifier = Modifier.width(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B6B)
                            )
                        ) {
                            if (isSendingSms) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else if (smsCountdown > 0) {
                                Text(
                                    text = "${smsCountdown}s",
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = "发送",
                                    fontSize = 12.sp
                                )
                            }
                        }
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
            
            // 操作按钮区域
            
            if (currentBindedPhone.isNotEmpty()) {
                // 如果已有绑定手机号，只显示解绑手机按钮
                OutlinedButton(
                    onClick = {
                        if (smsCode.isEmpty()) {
                            errorMessage = "请先获取并输入短信验证码"
                            return@OutlinedButton
                        }
                        
                        isUnbinding = true
                        errorMessage = ""
                        
                        coroutineScope.launch {
                            try {
                                val userId = UserManager.getUserId(context) ?: ""
                                val token = UserManager.getToken(context) ?: ""
                                
                                if (userId.isEmpty() || token.isEmpty()) {
                                    errorMessage = "用户信息无效，请重新登录"
                                    isUnbinding = false
                                    return@launch
                                }
                                
                                // 调用手机号解绑API
                                val result = UserService.updatePhoneWithSms(context, userId, currentBindedPhone, smsCode, "unbind")
                                
                                if (result != null) {
                                    // UserManager.updatePhone 已在 UserService 中调用
                                    phoneNumber = ""
                                    smsCode = ""
                                    onNavigateBack()
                                }
                            } catch (e: Exception) {
                                errorMessage = "网络错误：${e.message}"
                            } finally {
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
                    enabled = !isUnbinding && !isBinding && smsCode.isNotEmpty()
                ) {
                    if (isUnbinding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Red,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "解绑手机号",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                    

            } else {
                // 如果没有绑定手机号，只显示绑定按钮
                Button(
                    onClick = {
                        if (!isValidPhoneNumber(phoneNumber)) {
                            errorMessage = "请输入正确的手机号码"
                            return@Button
                        }
                        
                        if (smsCode.isEmpty()) {
                            errorMessage = "请先获取并输入短信验证码"
                            return@Button
                        }
                        
                        isBinding = true
                        errorMessage = ""
                        
                        coroutineScope.launch {
                            try {
                                val userId = UserManager.getUserId(context) ?: ""
                                val token = UserManager.getToken(context) ?: ""
                                
                                if (userId.isEmpty() || token.isEmpty()) {
                                    errorMessage = "用户信息无效，请重新登录"
                                    isBinding = false
                                    return@launch
                                }
                                
                                // 调用手机号绑定API
                                val result = UserService.updatePhoneWithSms(context, userId, phoneNumber, smsCode, "bind")
                                
                                if (result != null) {
                                    // UserManager.updatePhone 已在 UserService 中调用
                                    onNavigateBack()
                                }
                            } catch (e: Exception) {
                                errorMessage = "网络错误：${e.message}"
                            } finally {
                                isBinding = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B)
                    ),
                    enabled = isValidPhoneNumber(phoneNumber) && !isBinding && smsCode.isNotEmpty()
                ) {
                    if (isBinding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "绑定手机号",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// 验证手机号格式
fun isValidPhoneNumber(phone: String): Boolean {
    return phone.matches(Regex("^1[3-9]\\d{9}$"))
}

// 模拟验证码验证（实际项目中应该调用后端API）
fun verifyCode(code: String): Boolean {
    // 这里简单模拟，实际应该调用后端验证
    return code == "123456" || code.length == 6
}

// 注意：手机号现在通过 UserManager 统一管理，不再使用单独的 SharedPreferences

// 格式化显示手机号（隐藏中间4位）
fun formatPhoneNumber(phone: String): String {
    return if (phone.length == 11) {
        "${phone.substring(0, 3)}****${phone.substring(7)}"
    } else {
        phone
    }
}

// 加载图形验证码的辅助函数
private suspend fun loadCaptcha(
    context: Context,
    onSuccess: (String, androidx.compose.ui.graphics.ImageBitmap?) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val result = NetworkUtils.get(ApiConfig.User.GET_CAPTCHA)
        when (result) {
            is NetworkResult.Success -> {
                val response = NetworkUtils.parseJson<CaptchaResponse>(result.data)
                if (response != null && response.isSuccess()) {
                    val captchaData = response.getCaptchaData()
                    if (captchaData != null) {
                        val sessionId = captchaData.session_id
                        
                        // 解析base64图片
                        try {
                            val imageData = captchaData.image.substringAfter("base64,")
                            val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            val imageBitmap = bitmap.asImageBitmap()
                            onSuccess(sessionId, imageBitmap)
                        } catch (e: Exception) {
                            Log.e("BindPhoneScreen", "解析验证码图片失败", e)
                            onError("验证码图片加载失败")
                        }
                    } else {
                        onError("获取验证码数据失败")
                    }
                } else {
                    onError(response?.message ?: "获取图形验证码失败")
                }
            }
            is NetworkResult.Error -> {
                onError(result.parseApiErrorMessage())
            }
            is NetworkResult.Exception -> {
                onError("网络异常: ${result.exception.message}")
            }
        }
    } catch (e: Exception) {
        onError("加载图形验证码失败: ${e.message}")
    }
}

// 发送短信验证码的辅助函数
private suspend fun sendSmsCode(
    context: Context,
    phoneNumber: String,
    captchaCode: String,
    sessionId: String,
    event: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val request = SendSmsRequest(
            mobile = phoneNumber,
            captcha_code = captchaCode,
            session_id = sessionId,
            event = event
        )
        
        val result = NetworkUtils.postJson(ApiConfig.User.SEND_SMS, request)
        when (result) {
            is NetworkResult.Success -> {
                val response = NetworkUtils.parseJson<SendSmsResponse>(result.data)
                if (response != null && response.isSuccess()) {
                    onSuccess()
                } else {
                    onError(response?.message ?: "发送短信验证码失败")
                }
            }
            is NetworkResult.Error -> {
                onError(result.parseApiErrorMessage())
            }
            is NetworkResult.Exception -> {
                onError("网络异常: ${result.exception.message}")
            }
        }
    } catch (e: Exception) {
        onError("发送短信验证码失败: ${e.message}")
    }
}
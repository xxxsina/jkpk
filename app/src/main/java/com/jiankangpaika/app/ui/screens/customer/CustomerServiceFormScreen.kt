package com.jiankangpaika.app.ui.screens.customer

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.jiankangpaika.app.ui.components.CustomButton
import com.jiankangpaika.app.ui.components.CustomTextField
import com.jiankangpaika.app.ui.components.FileUploadSection
import com.jiankangpaika.app.ui.components.TopAppBarWithBack
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.SystemConfigManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerServiceFormScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToMessageList: () -> Unit = {},
    viewModel: CustomerServiceViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // 获取用户信息
    val userNickname = UserManager.getNickname(context) ?: ""
    val userPhone = UserManager.getPhone(context) ?: ""
    
    // 获取系统配置
    var customerServiceConfig by remember { mutableStateOf(SystemConfigManager.getCustomerServiceConfig(context)) }
    
    // 页面进入时请求配置
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                SystemConfigManager.manualUpdate(context)
                customerServiceConfig = SystemConfigManager.getCustomerServiceConfig(context)
            } catch (e: Exception) {
                // 配置更新失败，使用默认配置
            }
        }
    }
    
    // 表单状态
    var realname by remember { mutableStateOf(userNickname) }
    var mobile by remember { mutableStateOf(userPhone) }
    var problem by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    
    // 文件选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
    }
    
    // 监听提交结果
    LaunchedEffect(uiState.isSubmitSuccess) {
        if (uiState.isSubmitSuccess) {
            ToastUtils.showSuccessToast(context, "提交成功，我们会尽快为您处理")
            // 清空表单
            problem = ""
            selectedImageUri = null
            selectedVideoUri = null
            viewModel.resetSubmitState()
            // 提交成功后返回到来时的路径
            onNavigateBack()
        }
    }
    
    // 监听错误信息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            ToastUtils.showErrorToast(context, message)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBarWithBack(
                title = "联系客服",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateToMessageList) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "问题列表",
                            tint = Color.Black
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))
            // 表单说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEF3C7)
                )
            ) {
                Text(
                    text = "请详细描述您遇到的问题，我们会尽快为您解决；点击右上角，查看所有提问",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD97706)
                )
            }
            
            // 姓名输入
            CustomTextField(
                value = realname,
                onValueChange = { realname = it },
                label = "姓名",
                placeholder = "请输入您的姓名",
                isRequired = true
            )
            
            // 手机号输入
            CustomTextField(
                value = mobile,
                onValueChange = { newValue ->
                    // 只允许输入数字，最多11位
                    if (newValue.all { it.isDigit() } && newValue.length <= 11) {
                        mobile = newValue
                    }
                },
                label = "手机号",
                placeholder = "请输入您的手机号",
                isRequired = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            // 问题描述
            CustomTextField(
                value = problem,
                onValueChange = { problem = it },
                label = "遇到的问题",
                placeholder = "请详细描述您遇到的问题，我们会尽快为您解决",
                isRequired = true,
                singleLine = false,
                minLines = 2,
                maxLines = 8
            )
            
            // 图片上传（根据配置显示）
            if (customerServiceConfig.imageUploadEnabled) {
                FileUploadSection(
                    title = "上传图片（可选）",
                    fileUris = selectedImageUri?.let { listOf(it) } ?: emptyList(),
                    onAddFile = { imagePickerLauncher.launch("image/*") },
                    onRemoveFile = { _ -> selectedImageUri = null },
                    maxFiles = 1
                )
            }
            
            // 视频上传（根据配置显示）
            if (customerServiceConfig.videoUploadEnabled) {
                FileUploadSection(
                    title = "上传视频（可选）",
                    fileUris = selectedVideoUri?.let { listOf(it) } ?: emptyList(),
                    onAddFile = { videoPickerLauncher.launch("video/*") },
                    onRemoveFile = { _ -> selectedVideoUri = null },
                    maxFiles = 1
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 提交按钮
            CustomButton(
                text = if (uiState.isSubmitting) "提交中..." else "确认提交",
                onClick = {
                    if (validateForm(context, realname, mobile, problem)) {
                        viewModel.submitForm(
                            context = context,
                            realname = realname,
                            mobile = mobile,
                            problem = problem,
                            imageUri = selectedImageUri,
                            videoUri = selectedVideoUri
                        )
                    }
                },
                enabled = !uiState.isSubmitting && realname.isNotBlank() && mobile.isNotBlank() && problem.isNotBlank(),
                backgroundColor = Color(0xFFFF6B6B),
                disabledBackgroundColor = Color(0xFFE0E0E0),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 验证表单
 */
private fun validateForm(
    context: Context,
    realname: String,
    mobile: String,
    problem: String
): Boolean {
    when {
        realname.isBlank() -> {
            ToastUtils.showErrorToast(context, "请输入姓名")
            return false
        }
        mobile.isBlank() -> {
            ToastUtils.showErrorToast(context, "请输入手机号")
            return false
        }
        !isValidMobile(mobile) -> {
            ToastUtils.showErrorToast(context, "请输入正确的手机号")
            return false
        }
        problem.isBlank() -> {
            ToastUtils.showErrorToast(context, "请描述您遇到的问题")
            return false
        }
        problem.length < 10 -> {
            ToastUtils.showErrorToast(context, "问题描述至少需要10个字符")
            return false
        }
    }
    return true
}

/**
 * 验证手机号格式
 */
private fun isValidMobile(mobile: String): Boolean {
    val mobileRegex = "^1[3-9]\\d{9}$".toRegex()
    return mobileRegex.matches(mobile)
}
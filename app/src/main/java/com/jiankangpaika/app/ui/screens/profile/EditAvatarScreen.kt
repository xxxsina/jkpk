package com.jiankangpaika.app.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jiankangpaika.app.R
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.constants.ApiConfig
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.jiankangpaika.app.data.model.UpdateUserResponse
import com.jiankangpaika.app.data.model.UpdateUserRequest
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 预设头像资源列表
val presetAvatars = listOf(
    R.drawable.avatar_1,
    R.drawable.avatar_2,
    R.drawable.avatar_3,
    R.drawable.avatar_4,
    R.drawable.avatar_5,
    R.drawable.avatar_6,
    R.drawable.avatar_7,
    R.drawable.avatar_8
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAvatarScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedAvatarIndex by remember { mutableStateOf(getSelectedAvatarIndex(context)) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var customAvatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var customAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var networkAvatarUrl by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    
    // 拍照结果处理
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d("EditAvatar", "拍照结果: success=$success, photoUri=$photoUri")
        if (success && photoUri != null) {
            try {
                Log.d("EditAvatar", "开始处理拍照图片: $photoUri")
                val result = processAndSaveImageWithBitmap(context, photoUri!!)
                if (result != null) {
                    Log.d("EditAvatar", "图片处理成功: uri=${result.first}, bitmap=${result.second}")
                    customAvatarUri = result.first
                    customAvatarBitmap = result.second
                    selectedAvatarIndex = -1 // 标记为自定义头像
                    networkAvatarUrl = null // 清除网络头像
                    // 立即保存到SharedPreferences
                    saveCustomAvatar(context, result.first)
                    Log.d("EditAvatar", "状态更新完成: customAvatarUri=$customAvatarUri, customAvatarBitmap=$customAvatarBitmap, selectedAvatarIndex=$selectedAvatarIndex")
                    ToastUtils.showSuccessToast(context, "拍照成功")
                } else {
                    Log.e("EditAvatar", "图片处理返回null")
                    ToastUtils.showErrorToast(context, "图片处理失败")
                }
            } catch (e: Exception) {
                Log.e("EditAvatar", "拍照处理异常", e)
                e.printStackTrace()
                ToastUtils.showErrorToast(context, "拍照失败：${e.message}")
            }
        } else {
            Log.w("EditAvatar", "拍照失败或取消: success=$success, photoUri=$photoUri")
        }
    }
    
    // 相册选择结果处理
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        Log.d("EditAvatar", "相册选择结果: uri=$uri")
        uri?.let {
            try {
                Log.d("EditAvatar", "开始处理相册图片: $it")
                val result = processAndSaveImageWithBitmap(context, it)
                if (result != null) {
                    Log.d("EditAvatar", "图片处理成功: uri=${result.first}, bitmap=${result.second}")
                    customAvatarUri = result.first
                    customAvatarBitmap = result.second
                    selectedAvatarIndex = -1 // 标记为自定义头像
                    networkAvatarUrl = null // 清除网络头像
                    // 立即保存到SharedPreferences
                    saveCustomAvatar(context, result.first)
                    Log.d("EditAvatar", "状态更新完成: customAvatarUri=$customAvatarUri, customAvatarBitmap=$customAvatarBitmap, selectedAvatarIndex=$selectedAvatarIndex")
                    ToastUtils.showSuccessToast(context, "图片选择成功")
                } else {
                    Log.e("EditAvatar", "图片处理返回null")
                    ToastUtils.showErrorToast(context, "图片处理失败")
                }
            } catch (e: Exception) {
                Log.e("EditAvatar", "相册图片处理异常", e)
                e.printStackTrace()
                ToastUtils.showErrorToast(context, "图片选择失败：${e.message}")
            }
        } ?: Log.w("EditAvatar", "相册选择取消或失败")
    }
    
    // 权限请求
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("EditAvatar", "相机权限请求结果: isGranted=$isGranted")
        if (isGranted) {
            // 权限已授予，启动相机
            photoUri = createImageUri(context)
            photoUri?.let { uri ->
                Log.d("EditAvatar", "权限授予后启动拍照: photoUri=$uri")
                takePictureLauncher.launch(uri)
            }
        } else {
            Log.w("EditAvatar", "相机权限被拒绝")
            ToastUtils.showTopToast(context, "需要相机权限才能拍照")
        }
    }
    
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("EditAvatar", "存储权限请求结果: isGranted=$isGranted")
        if (isGranted) {
            // 权限已授予，打开相册
            Log.d("EditAvatar", "权限授予后启动相册选择")
            pickImageLauncher.launch("image/*")
        } else {
            Log.w("EditAvatar", "存储权限被拒绝")
            ToastUtils.showTopToast(context, "需要存储权限才能访问相册")
        }
    }
    
    // 初始化时加载用户头像
    LaunchedEffect(Unit) {
        Log.d("EditAvatar", "开始初始化头像加载逻辑")
        
        // 首先检查本地是否有自定义头像
        val localCustomAvatar = getCustomAvatar(context)
        val hasLocalCustom = hasCustomAvatar(context)
        Log.d("EditAvatar", "本地自定义头像检查: hasLocalCustom=$hasLocalCustom, localCustomAvatar=$localCustomAvatar")
        
        if (hasLocalCustom && localCustomAvatar != null) {
            // 优先使用本地自定义头像
            customAvatarBitmap = localCustomAvatar
            selectedAvatarIndex = -1
            networkAvatarUrl = null
            Log.d("EditAvatar", "使用本地自定义头像")
        } else {
            // 本地没有自定义头像，检查服务器头像
            val userAvatarUrl = UserManager.getAvatarUrl(context)
            Log.d("EditAvatar", "检查服务器头像: userAvatarUrl=$userAvatarUrl")
            
            if (!userAvatarUrl.isNullOrEmpty()) {
                if (userAvatarUrl.contains("http")) {
                    // 这是网络自定义头像URL
                    networkAvatarUrl = userAvatarUrl
                    customAvatarBitmap = null
                    selectedAvatarIndex = -1
                    Log.d("EditAvatar", "使用网络自定义头像: $userAvatarUrl")
                } else {
                    // 这是预设头像，根据avatar确定选中的预设头像索引
                    val avatarIndex = when {
                        userAvatarUrl.contains("avatar_1") -> 0
                        userAvatarUrl.contains("avatar_2") -> 1
                        userAvatarUrl.contains("avatar_3") -> 2
                        userAvatarUrl.contains("avatar_4") -> 3
                        userAvatarUrl.contains("avatar_5") -> 4
                        userAvatarUrl.contains("avatar_6") -> 5
                        userAvatarUrl.contains("avatar_7") -> 6
                        userAvatarUrl.contains("avatar_8") -> 7
                        else -> 0
                    }
                    selectedAvatarIndex = avatarIndex
                    customAvatarBitmap = null
                    networkAvatarUrl = null
                    Log.d("EditAvatar", "使用预设头像索引: $avatarIndex")
                }
            } else {
                // 没有任何头像，使用默认头像
                selectedAvatarIndex = 0
                customAvatarBitmap = null
                networkAvatarUrl = null
                Log.d("EditAvatar", "使用默认头像")
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = {
                Text(
                    text = "编辑头像",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
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
                        if (customAvatarUri != null) {
                            // 上传自定义头像到服务器
                            scope.launch {
                                uploadAvatarToServer(
                                    context = context,
                                    avatarType = "custom",
                                    avatarValue = customAvatarUri.toString(),
                                    onSuccess = { avatarUrl ->
                                        Log.d("EditAvatar", "自定义头像上传成功: $avatarUrl")
                                        // 保存到本地
                                        saveCustomAvatar(context, customAvatarUri!!)
                                        // 更新UserManager
                                        UserManager.updateAvatarUrl(context, avatarUrl)
                                        // 显示成功提示
                                        ToastUtils.showSuccessToast(context, "头像更新成功")
                                        onNavigateBack()
                                    },
                                    onError = { errorMsg ->
                                        Log.e("EditAvatar", "自定义头像上传失败: $errorMsg")
                                        ToastUtils.showErrorToast(context, "头像更新失败: $errorMsg")
                                    }
                                )
                            }
                        } else if (selectedAvatarIndex >= 0) {
                            // 上传预设头像到服务器
                            scope.launch {
                                val avatarResourceName = when (selectedAvatarIndex) {
                                    0 -> "avatar_1"
                                    1 -> "avatar_2"
                                    2 -> "avatar_3"
                                    3 -> "avatar_4"
                                    4 -> "avatar_5"
                                    5 -> "avatar_6"
                                    6 -> "avatar_7"
                                    7 -> "avatar_8"
                                    else -> "avatar_1"
                                }
                                
                                uploadAvatarToServer(
                                    context = context,
                                    avatarType = "default",
                                    avatarValue = avatarResourceName,
                                    onSuccess = { avatarUrl ->
                                        Log.d("EditAvatar", "默认头像上传成功: $avatarUrl")
                                        // 保存到本地
                                        saveSelectedAvatar(context, selectedAvatarIndex)
                                        // 更新UserManager
                                        UserManager.updateAvatarUrl(context, avatarUrl)
                                        // 显示成功提示
                                        ToastUtils.showSuccessToast(context, "头像更新成功")
                                        onNavigateBack()
                                    },
                                    onError = { errorMsg ->
                                        Log.e("EditAvatar", "默认头像上传失败: $errorMsg")
                                        ToastUtils.showErrorToast(context, "头像更新失败: $errorMsg")
                                    }
                                )
                            }
                        } else {
                            // 没有选择任何头像
                            ToastUtils.showTopToast(context, "请选择一个头像")
                        }
                    }
                ) {
                    Text(
                        text = "保存",
                        color = Color(0xFF007AFF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // 当前头像预览
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFFE5E5E5), CircleShape)
                    .background(Color.White)
            ) {
                Log.d("EditAvatar", "UI渲染头像: customAvatarBitmap=$customAvatarBitmap, selectedAvatarIndex=$selectedAvatarIndex, networkAvatarUrl=$networkAvatarUrl")
                when {
                    customAvatarBitmap != null -> {
                        // 显示本地自定义头像
                        Log.d("EditAvatar", "显示本地自定义头像")
                        Image(
                            bitmap = customAvatarBitmap!!,
                            contentDescription = "自定义头像",
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !networkAvatarUrl.isNullOrEmpty() -> {
                        // 显示网络自定义头像
                        Log.d("EditAvatar", "显示网络自定义头像: $networkAvatarUrl")
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(networkAvatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "网络头像",
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        // 显示预设头像
                        Log.d("EditAvatar", "显示预设头像: index=${selectedAvatarIndex.coerceAtLeast(0)}")
                        Image(
                            painter = painterResource(id = presetAvatars[selectedAvatarIndex.coerceAtLeast(0)]),
                            contentDescription = "当前头像",
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 头像来源选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 拍照按钮
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        Log.d("EditAvatar", "拍照按钮被点击")
                        // 检查相机权限
                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        Log.d("EditAvatar", "相机权限检查结果: hasPermission=$hasPermission")
                        if (hasPermission) {
                            // 权限已授予，直接拍照
                            photoUri = createImageUri(context)
                            photoUri?.let { uri ->
                                Log.d("EditAvatar", "直接启动拍照: photoUri=$uri")
                                takePictureLauncher.launch(uri)
                            }
                        } else {
                            // 请求相机权限
                            Log.d("EditAvatar", "请求相机权限")
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "拍照",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "拍照",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                // 相册按钮
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        // 检查存储权限
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            // 权限已授予，直接打开相册
                            pickImageLauncher.launch("image/*")
                        } else {
                            // 请求存储权限
                            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "相册",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "相册",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
            
            // 分割线
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFFE5E5E5),
                thickness = 1.dp
            )
            
            // 预设头像标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "选择预设头像",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 预设头像网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presetAvatars.size) { index ->
                    val isSelected = index == selectedAvatarIndex
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF007AFF) else Color(0xFFE5E5E5),
                                shape = CircleShape
                            )
                            .background(Color.White)
                            .clickable {
                                Log.d("EditAvatar", "点击了预设头像: index=$index")
                                selectedAvatarIndex = index
                                customAvatarBitmap = null
                                customAvatarUri = null
                                networkAvatarUrl = null
                            }
                    ) {
                        Image(
                            painter = painterResource(id = presetAvatars[index]),
                            contentDescription = "头像 ${index + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isSelected) 3.dp else 1.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        // 选中状态指示器
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 图片来源选择对话框
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = {
                Text(
                    text = "选择图片来源",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = "请选择获取头像的方式",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            // 检查相机权限
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                // 权限已授予，直接拍照
                                photoUri = createImageUri(context)
                                photoUri?.let { uri ->
                                    takePictureLauncher.launch(uri)
                                }
                            } else {
                                // 请求相机权限
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Text(
                            text = "拍照",
                            color = Color(0xFF007AFF)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            // 检查存储权限
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                // 权限已授予，直接打开相册
                                pickImageLauncher.launch("image/*")
                            } else {
                                // 请求存储权限
                                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    ) {
                        Text(
                            text = "相册",
                            color = Color(0xFF007AFF)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImageSourceDialog = false }
                ) {
                    Text(
                        text = "取消",
                        color = Color(0xFF666666)
                    )
                }
            }
        )
    }
}

// 获取当前选中的头像索引
fun getSelectedAvatarIndex(context: Context): Int {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getInt("selected_avatar_index", 0)
}

// 获取当前头像资源ID
fun getCurrentAvatarResource(context: Context): Int {
    // 如果有自定义头像，返回默认头像（因为自定义头像不是资源ID）
    if (hasCustomAvatar(context)) {
        return R.drawable.touxiang
    }
    val index = getSelectedAvatarIndex(context)
    return presetAvatars[index.coerceAtLeast(0)]
}

// 保存选中的头像索引
fun saveSelectedAvatar(context: Context, avatarIndex: Int) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putInt("selected_avatar_index", avatarIndex)
        putBoolean("has_custom_avatar", false) // 清除自定义头像标记
        remove("custom_avatar_uri") // 清除自定义头像URI
        apply()
    }
    
    // 同时更新UserManager中的头像URL
    val avatarResourceName = when (avatarIndex) {
        0 -> "avatar_1"
        1 -> "avatar_2"
        2 -> "avatar_3"
        3 -> "avatar_4"
        4 -> "avatar_5"
        5 -> "avatar_6"
        6 -> "avatar_7"
        7 -> "avatar_8"
        else -> "avatar_1"
    }
    val avatarUrl = "android.resource://${context.packageName}/drawable/$avatarResourceName"
    UserManager.updateAvatarUrl(context, avatarUrl)
}

/**
 * 上传头像到服务器
 * @param context 上下文
 * @param avatarType 头像类型："default"表示默认头像，"custom"表示自定义头像
 * @param avatarValue 头像值：默认头像为资源名称（如"avatar_1"），自定义头像为文件URI
 * @param onSuccess 成功回调
 * @param onError 失败回调
 */
suspend fun uploadAvatarToServer(
    context: Context,
    avatarType: String,
    avatarValue: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val userId = UserManager.getUserId(context)
        
        if (userId.isNullOrEmpty()) {
            onError("用户未登录")
            return
        }
        
        Log.d("EditAvatar", "开始上传头像: type=$avatarType, value=$avatarValue")
        
        val result = if (avatarType == "default") {
            // 上传默认头像
            uploadDefaultAvatar(context, userId, avatarValue)
        } else {
            // 上传自定义头像文件
            uploadCustomAvatar(context, userId, avatarValue)
        }
        
        when (result) {
            is NetworkResult.Success -> {
                val response = NetworkUtils.parseJson<UpdateUserResponse>(result.data)
                if (response?.code == 200) {
                    val avatarUrl = response.data?.avatar ?: ""
                    Log.d("EditAvatar", "头像上传成功: $avatarUrl")
                    onSuccess(avatarUrl)
                } else {
                    val errorMsg = response?.message ?: "上传失败"
                    Log.e("EditAvatar", "头像上传失败: $errorMsg")
                    onError(errorMsg)
                }
            }
            is NetworkResult.Error -> {
                val errorMessage = result.parseApiErrorMessage()
                Log.e("EditAvatar", "头像上传网络错误: $errorMessage")
                onError(errorMessage)
            }
            is NetworkResult.Exception -> {
                Log.e("EditAvatar", "头像上传异常: ${result.exception.message}")
                onError("上传异常: ${result.exception.message}")
            }
        }
    } catch (e: Exception) {
        Log.e("EditAvatar", "头像上传异常: ${e.message}", e)
        onError("上传异常: ${e.message}")
    }
}

/**
 * 上传默认头像
 */
private suspend fun uploadDefaultAvatar(
    context: Context,
    userId: String,
    avatarValue: String
): NetworkResult {
    val request = UpdateUserRequest(
        action = "update_avatar",
        user_id = userId,
        value = avatarValue
    )
    
    // 使用带认证的方法
    return NetworkUtils.postJsonWithAuth(
        context = context,
        url = ApiConfig.User.UPDATE_USER,
        data = request
    )
}

/**
 * 上传自定义头像文件
 */
private suspend fun uploadCustomAvatar(
    context: Context,
    userId: String,
    avatarUri: String
): NetworkResult {
    val uri = Uri.parse(avatarUri)
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: return NetworkResult.Exception(Exception("无法读取文件"))
    
    // 检查文件大小（不超过1MB）
    val fileSize = inputStream.available()
    if (fileSize > 1024 * 1024) {
        inputStream.close()
        return NetworkResult.Exception(Exception("文件大小不能超过1MB"))
    }
    
    val formData = mapOf(
        "action" to "update_avatar",
        "user_id" to userId
    )
    
    val fileData = mapOf(
        "avatar" to NetworkUtils.FileUploadData(
            fileName = "avatar_${System.currentTimeMillis()}.jpg",
            mimeType = "image/jpeg",
            inputStream = inputStream
        )
    )
    
    // 使用带认证的文件上传方法
    return NetworkUtils.uploadFileWithAuth(context, ApiConfig.User.UPDATE_USER, formData, fileData)
}

// 保存自定义头像
fun saveCustomAvatar(context: Context, uri: Uri) {
    try {
        // 保存自定义头像URI
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("has_custom_avatar", true)
            putString("custom_avatar_uri", uri.toString())
            apply()
        }
        
        // 同时更新UserManager中的头像URL（自定义头像使用URI字符串）
        UserManager.updateAvatarUrl(context, uri.toString())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// 获取自定义头像
fun getCustomAvatar(context: Context): ImageBitmap? {
    return try {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val avatarUriString = sharedPreferences.getString("custom_avatar_uri", null)
        val hasCustom = sharedPreferences.getBoolean("has_custom_avatar", false)
        Log.d("EditAvatar", "getCustomAvatar: avatarUriString=$avatarUriString, hasCustom=$hasCustom")
        
        if (avatarUriString != null) {
            val uri = Uri.parse(avatarUriString)
            Log.d("EditAvatar", "尝试加载头像文件: $uri")
            
            // 检查文件是否存在
            val file = File(uri.path ?: "")
            if (file.exists()) {
                Log.d("EditAvatar", "头像文件存在: ${file.absolutePath}, 大小: ${file.length()}")
                val bitmap = BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(uri)
                )
                if (bitmap != null) {
                    Log.d("EditAvatar", "头像解码成功: ${bitmap.width}x${bitmap.height}")
                    return bitmap.asImageBitmap()
                } else {
                    Log.e("EditAvatar", "头像解码失败")
                }
            } else {
                Log.e("EditAvatar", "头像文件不存在: ${file.absolutePath}")
            }
        } else {
            Log.d("EditAvatar", "SharedPreferences中没有自定义头像URI")
        }
        null
    } catch (e: Exception) {
        Log.e("EditAvatar", "getCustomAvatar异常", e)
        e.printStackTrace()
        null
    }
}

// 检查是否有自定义头像
fun hasCustomAvatar(context: Context): Boolean {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("has_custom_avatar", false)
}

// 创建图片URI用于拍照
fun createImageUri(context: Context): Uri? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_avatar.jpg"
        val storageDir = File(context.cacheDir, "images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val imageFile = File(storageDir, imageFileName)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 处理和保存图片
fun processAndSaveImage(context: Context, sourceUri: Uri): Uri? {
    return try {
        // 读取原始图片
        val inputStream = context.contentResolver.openInputStream(sourceUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        if (originalBitmap == null) {
            return null
        }
        
        // 处理图片旋转
        val rotatedBitmap = rotateImageIfRequired(context, originalBitmap, sourceUri)
        
        // 压缩图片到合适大小（512x512像素，质量85%）
        val resizedBitmap = resizeBitmap(rotatedBitmap, 512, 512)
        
        // 保存到应用私有目录
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "avatar_${timeStamp}.jpg"
        val avatarDir = File(context.filesDir, "avatars")
        if (!avatarDir.exists()) {
            avatarDir.mkdirs()
        }
        val avatarFile = File(avatarDir, fileName)
        
        val outputStream = FileOutputStream(avatarFile)
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.close()
        
        // 清理旧的头像文件
        cleanOldAvatarFiles(context)
        
        Uri.fromFile(avatarFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 处理和保存图片，同时返回Uri和ImageBitmap
fun processAndSaveImageWithBitmap(context: Context, sourceUri: Uri): Pair<Uri, ImageBitmap>? {
    return try {
        Log.d("EditAvatar", "processAndSaveImageWithBitmap开始: sourceUri=$sourceUri")
        
        // 读取原始图片
        val inputStream = context.contentResolver.openInputStream(sourceUri)
        Log.d("EditAvatar", "打开输入流: inputStream=$inputStream")
        
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        Log.d("EditAvatar", "解码原始图片: originalBitmap=$originalBitmap, size=${originalBitmap?.width}x${originalBitmap?.height}")
        
        if (originalBitmap == null) {
            Log.e("EditAvatar", "原始图片解码失败")
            return null
        }
        
        // 处理图片旋转
        val rotatedBitmap = rotateImageIfRequired(context, originalBitmap, sourceUri)
        Log.d("EditAvatar", "旋转处理完成: rotatedBitmap=$rotatedBitmap, size=${rotatedBitmap.width}x${rotatedBitmap.height}")
        
        // 压缩图片到合适大小（512x512像素，质量85%）
        val resizedBitmap = resizeBitmap(rotatedBitmap, 512, 512)
        Log.d("EditAvatar", "压缩处理完成: resizedBitmap=$resizedBitmap, size=${resizedBitmap.width}x${resizedBitmap.height}")
        
        // 保存到应用私有目录
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "avatar_${timeStamp}.jpg"
        val avatarDir = File(context.filesDir, "avatars")
        if (!avatarDir.exists()) {
            avatarDir.mkdirs()
            Log.d("EditAvatar", "创建头像目录: ${avatarDir.absolutePath}")
        }
        val avatarFile = File(avatarDir, fileName)
        Log.d("EditAvatar", "准备保存到: ${avatarFile.absolutePath}")
        
        val outputStream = FileOutputStream(avatarFile)
        val compressResult = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.close()
        Log.d("EditAvatar", "文件保存完成: compressResult=$compressResult, fileExists=${avatarFile.exists()}, fileSize=${avatarFile.length()}")
        
        // 清理旧的头像文件
        cleanOldAvatarFiles(context)
        
        // 同时返回Uri和ImageBitmap
        val uri = Uri.fromFile(avatarFile)
        val imageBitmap = resizedBitmap.asImageBitmap()
        Log.d("EditAvatar", "返回结果: uri=$uri, imageBitmap=$imageBitmap")
        
        Pair(uri, imageBitmap)
    } catch (e: Exception) {
        Log.e("EditAvatar", "processAndSaveImageWithBitmap异常", e)
        e.printStackTrace()
        null
    }
}

// 旋转图片（处理EXIF信息）
fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val exif = inputStream?.let { ExifInterface(it) }
        inputStream?.close()
        
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL
        
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        bitmap
    }
}

// 旋转Bitmap
fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// 调整图片大小
fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    
    // 计算缩放比例
    val scaleWidth = maxWidth.toFloat() / width
    val scaleHeight = maxHeight.toFloat() / height
    val scale = minOf(scaleWidth, scaleHeight)
    
    // 如果图片已经足够小，直接返回
    if (scale >= 1.0f) {
        return bitmap
    }
    
    // 计算新的尺寸
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()
    
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

// 加载图片为ImageBitmap
fun loadImageBitmap(context: Context, uri: Uri): ImageBitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 清理旧的头像文件（保留最新的3个）
fun cleanOldAvatarFiles(context: Context) {
    try {
        val avatarDir = File(context.filesDir, "avatars")
        if (avatarDir.exists()) {
            val files = avatarDir.listFiles()?.filter { it.name.startsWith("avatar_") && it.name.endsWith(".jpg") }
            if (files != null && files.size > 3) {
                // 按修改时间排序，删除最旧的文件
                val sortedFiles = files.sortedBy { it.lastModified() }
                for (i in 0 until sortedFiles.size - 3) {
                    sortedFiles[i].delete()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
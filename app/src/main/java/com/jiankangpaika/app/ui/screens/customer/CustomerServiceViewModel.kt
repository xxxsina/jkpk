package com.jiankangpaika.app.ui.screens.customer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiankangpaika.app.data.model.CustomerServiceRequest
import com.jiankangpaika.app.data.model.CustomerServiceResponse
import com.jiankangpaika.app.data.model.CustomerMessage
import com.jiankangpaika.app.data.model.CustomerMessagesResponse
import com.jiankangpaika.app.data.model.PaginationInfo
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.constants.ApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 客服表单ViewModel
 */
class CustomerServiceViewModel : ViewModel() {
    private val TAG = "CustomerServiceViewModel"
    
    private val _uiState = MutableStateFlow(CustomerServiceUiState())
    val uiState: StateFlow<CustomerServiceUiState> = _uiState.asStateFlow()
    
    private val _messageListState = MutableStateFlow(MessageListUiState())
    val messageListState: StateFlow<MessageListUiState> = _messageListState.asStateFlow()
    
    /**
     * 提交客服表单
     */
    fun submitForm(
        context: Context,
        realname: String,
        mobile: String,
        problem: String,
        imageUri: Uri? = null,
        videoUri: Uri? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
                
                // 检查用户是否登录
                val userId = UserManager.getUserId(context)
                if (userId.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "请先登录"
                    )
                    return@launch
                }
                
                Log.d(TAG, "🚀 [表单提交] 开始提交客服表单: userId=$userId, realname=$realname")
                
                // 准备表单数据
                val formData = mapOf(
                    "user_id" to userId,
                    "realname" to realname,
                    "mobile" to mobile,
                    "problem" to problem
                )
                
                // 准备文件数据
                val fileData = mutableMapOf<String, NetworkUtils.FileUploadData>()
                
                // 添加图片文件（如果有）
                imageUri?.let { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                            ?: throw Exception("无法读取图片文件")
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val fileName = "image_${userId}_${System.currentTimeMillis()}.${getFileExtension(context, uri)}"
                        
                        fileData["image"] = NetworkUtils.FileUploadData(
                            fileName = fileName,
                            mimeType = mimeType,
                            inputStream = inputStream
                        )
                        Log.d(TAG, "📁 [表单提交] 添加图片文件: $fileName")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [表单提交] 处理图片文件失败: ${e.message}")
                    }
                }
                
                // 添加视频文件（如果有）
                videoUri?.let { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                            ?: throw Exception("无法读取视频文件")
                        val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                        val fileName = "video_${userId}_${System.currentTimeMillis()}.${getFileExtension(context, uri)}"
                        
                        fileData["video"] = NetworkUtils.FileUploadData(
                            fileName = fileName,
                            mimeType = mimeType,
                            inputStream = inputStream
                        )
                        Log.d(TAG, "📁 [表单提交] 添加视频文件: $fileName")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [表单提交] 处理视频文件失败: ${e.message}")
                    }
                }
                
                // 提交表单和文件
                val result = NetworkUtils.uploadFileWithAuth(
                    context = context,
                    url = ApiConfig.CustomerService.SUBMIT_FORM,
                    formData = formData,
                    fileData = fileData
                )
                
                when (result) {
                    is NetworkResult.Success -> {
                        val response = NetworkUtils.parseJson<CustomerServiceResponse>(result.data)
                        if (response?.isSuccess() == true) {
                            Log.d(TAG, "✅ [表单提交] 提交成功: ${response.message}")
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                isSubmitSuccess = true
                            )
                        } else {
                            Log.w(TAG, "⚠️ [表单提交] 提交失败: ${response?.message}")
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                errorMessage = response?.message ?: "提交失败"
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "❌ [表单提交] 网络错误: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = "网络错误: ${result.message}"
                        )
                    }
                    is NetworkResult.Exception -> {
                        Log.e(TAG, "💥 [表单提交] 请求异常: ${result.exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = "请求失败: ${result.exception.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 [表单提交] 提交异常: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "提交失败: ${e.message}"
                )
            }
        }
    }
    

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(context: Context, uri: Uri): String {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/jpg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "video/mp4" -> "mp4"
                "video/avi" -> "avi"
                "video/quicktime" -> "mov"
                else -> "tmp"
            }
        } catch (e: Exception) {
            "tmp"
        }
    }
    
    /**
     * 获取客服消息列表
     */
    fun loadMessageList(context: Context, page: Int = 1) {
        viewModelScope.launch {
            try {
                _messageListState.value = _messageListState.value.copy(isLoading = true, errorMessage = null)
                
                // 检查用户是否登录
                val userId = UserManager.getUserId(context)
                if (userId.isNullOrEmpty()) {
                    _messageListState.value = _messageListState.value.copy(
                        isLoading = false,
                        errorMessage = "请先登录"
                    )
                    return@launch
                }
                
                Log.d(TAG, "🔍 [消息列表] 开始获取客服消息列表: userId=$userId, page=$page")
                
                // 构建请求数据
                val requestData = mapOf(
                    "user_id" to userId,
                    "page" to page.toString()
                )
                
                // 获取认证token
                val token = UserManager.getToken(context)
                val headers = if (!token.isNullOrEmpty()) {
                    mapOf("token" to token)
                } else {
                    emptyMap()
                }
                
                val result = NetworkUtils.postForm(
                    url = ApiConfig.CustomerService.GET_MESSAGES,
                    formData = requestData,
                    context = context,
                    headers = headers
                )
                
                when (result) {
                    is NetworkResult.Success -> {
                        val response = NetworkUtils.parseJson<CustomerMessagesResponse>(result.data)
                        if (response?.isSuccess() == true) {
                            val messages = response.getMessages()
                            val pagination = response.getPagination()
                            Log.d(TAG, "✅ [消息列表] 获取成功，消息数量: ${messages.size}, 总页数: ${pagination?.totalPages ?: 0}")
                            
                            // 如果是第一页，直接替换；否则追加到现有列表
                            val updatedMessages = if (page == 1) {
                                messages
                            } else {
                                _messageListState.value.messages + messages
                            }
                            
                            _messageListState.value = _messageListState.value.copy(
                                isLoading = false,
                                messages = updatedMessages,
                                pagination = pagination,
                                currentPage = page
                            )
                        } else {
                            Log.w(TAG, "⚠️ [消息列表] 获取失败: ${response?.message}")
                            _messageListState.value = _messageListState.value.copy(
                                isLoading = false,
                                errorMessage = response?.message ?: "获取失败"
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "❌ [消息列表] 网络错误: ${result.message}")
                        _messageListState.value = _messageListState.value.copy(
                            isLoading = false,
                            errorMessage = "网络错误: ${result.message}"
                        )
                    }
                    is NetworkResult.Exception -> {
                        Log.e(TAG, "💥 [消息列表] 请求异常: ${result.exception.message}")
                        _messageListState.value = _messageListState.value.copy(
                            isLoading = false,
                            errorMessage = "请求失败: ${result.exception.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 [消息列表] 获取异常: ${e.message}", e)
                _messageListState.value = _messageListState.value.copy(
                    isLoading = false,
                    errorMessage = "获取失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 加载更多消息
     */
    fun loadMoreMessages(context: Context) {
        val currentState = _messageListState.value
        val pagination = currentState.pagination
        
        // 检查是否有下一页且当前不在加载中
        if (pagination?.hasNext == true && !currentState.isLoading) {
            loadMessageList(context, currentState.currentPage + 1)
        }
    }
    
    /**
     * 重置提交状态
     */
    fun resetSubmitState() {
        _uiState.value = _uiState.value.copy(isSubmitSuccess = false)
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * 清除消息列表错误信息
     */
    fun clearMessageListError() {
        _messageListState.value = _messageListState.value.copy(errorMessage = null)
    }
}

/**
 * 客服表单UI状态
 */
data class CustomerServiceUiState(
    val isSubmitting: Boolean = false,
    val isSubmitSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 消息列表UI状态
 */
data class MessageListUiState(
    val isLoading: Boolean = false,
    val messages: List<CustomerMessage> = emptyList(),
    val pagination: PaginationInfo? = null,
    val currentPage: Int = 1,
    val errorMessage: String? = null
) {
    /**
     * 是否有更多数据可以加载
     */
    fun hasMoreData(): Boolean = pagination?.hasNext == true
    
    /**
     * 是否可以加载更多（有更多数据且当前不在加载中）
     */
    fun canLoadMore(): Boolean = hasMoreData() && !isLoading
}
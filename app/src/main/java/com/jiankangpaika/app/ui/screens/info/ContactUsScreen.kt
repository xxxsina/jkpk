package com.jiankangpaika.app.ui.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    onBackClick: () -> Unit = {},
    onNavigateToCustomerForm: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // 顶部导航栏
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "联系我们",
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
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
        
        // 联系方式内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 欢迎信息
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFED5144)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "欢迎联系健康派卡",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "我们随时为您提供帮助和支持",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            // 在线客服表单
            ContactSection(
                title = "在线客服",
                items = listOf(
                    ContactItem(
                        icon = Icons.Default.SupportAgent,
                        title = "提交问题",
                        content = "在线提交您遇到的问题",
                        subtitle = "我们会尽快为您解答",
                        iconColor = Color(0xFF10B981)
                    )
                ),
                onItemClick = { item ->
                    if (item.title == "提交问题") {
                        // 检查登录状态
                        if (UserManager.isLoggedIn(context)) {
                            onNavigateToCustomerForm()
                        } else {
                            onNavigateToLogin()
                        }
                    }
                }
            )
            
            // 客服邮箱
            ContactSection(
                title = "其他联系方式",
                items = listOf(
                    ContactItem(
                        icon = Icons.Default.Email,
                        title = "客服邮箱",
                        content = "kefu@blcwg.com",
                        subtitle = "我们会在24小时内回复您的邮件",
                        iconColor = Color(0xFFEA3323)
                    )
                ),
                onItemClick = { item ->
                    if (item.title == "客服邮箱") {
                        clipboardManager.setText(AnnotatedString(item.content))
                        ToastUtils.showSuccessToast(context, "邮箱地址已复制到剪贴板")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ContactSection(
    title: String,
    items: List<ContactItem>,
    onItemClick: (ContactItem) -> Unit = {}
) {
    Column {
        // 分组标题
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        
        // 联系方式卡片
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
            Column {
                items.forEachIndexed { index, item ->
                    ContactItemRow(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                    
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.Gray.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItemRow(
    item: ContactItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = item.iconColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 内容
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF161823)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.content,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = item.iconColor
            )
            
            if (item.subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

data class ContactItem(
    val icon: ImageVector,
    val title: String,
    val content: String,
    val subtitle: String = "",
    val iconColor: Color
)
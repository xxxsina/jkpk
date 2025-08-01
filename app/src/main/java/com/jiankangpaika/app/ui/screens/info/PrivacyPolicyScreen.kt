package com.jiankangpaika.app.ui.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部导航栏
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "隐私政策",
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
        
        // 隐私政策内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 更新时间
            Text(
                text = "最后更新时间：2025年6月17日",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 协议标题
            Text(
                text = "健康派卡APP隐私政策",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF161823),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 引言
            PolicySection(
                title = "引言",
                content = "健康派卡APP（以下简称\"我们\"、\"本应用\"或\"平台\"）深知个人信息对您的重要性，并会尽全力保护您的个人信息安全可靠。我们致力于维持您对我们的信任，恪守以下原则，保护您的个人信息：权责一致原则、目的明确原则、选择同意原则、最少够用原则、确保安全原则、主体参与原则、公开透明原则等。\n\n本隐私政策将帮助您了解以下内容：\n\n我们如何收集和使用您的个人信息\n我们如何使用本地存储技术\n我们如何保护您的个人信息\n您的权利\n本政策如何更新\n如何联系我们"
            )
            
            // 第一部分 我们如何收集和使用您的个人信息
            PolicySection(
                title = "第一部分 我们如何收集和使用您的个人信息",
                content = "1.1 我们收集您个人信息的方式\n我们会出于本政策所述的以下目的，收集和使用您的个人信息：\n\n1.1.1 您主动提供的信息\n账户注册信息：当您注册账户时，我们会收集您的手机号码、邮箱地址、用户名、密码等基本信息\n个人资料信息：您可以选择完善个人资料，包括头像、昵称、性别等信息\n反馈信息：当您联系我们或参与调研时，我们会收集您提供的相关信息\n\n1.1.2 我们在您使用服务过程中收集的信息\n日志信息：当您使用我们的服务时，我们会自动收集您对我们服务的详细使用情况，作为有关网络日志保存。\n包括：\n设备信息：设备型号、操作系统版本、设备设置、唯一设备标识符等软硬件特征信息\n软件信息：软件的版本号、浏览器类型\nIP地址、访问日期和时间\n您搜索或浏览的信息，如您使用的网页搜索词语、访问的页面地址、以及您在使用我们服务时浏览或要求提供的其他信息和内容详情\n其他相关信息：为了帮助您更好地使用我们的产品或服务，我们会收集相关信息"
            )
            
            // 1.2 我们如何使用您的个人信息
            PolicySection(
                title = "1.2 我们如何使用您的个人信息",
                content = "我们会出于以下目的使用我们收集的个人信息：\n\n1.2.1 向您提供服务\n向您提供您使用的各项功能服务\n身份验证、客户服务、安全防范、诈骗监测、存档和备份用途，确保我们向您提供的产品和服务的安全性\n帮助我们设计新服务，改善我们现有服务\n使我们更加了解您如何接入和使用我们的服务，从而针对性地回应您的个性化需求\n\n1.2.2 满足您的个性化需求\n向您推荐您可能感兴趣的健康文章、内容或功能\n软件认证或管理软件升级\n让您参与有关我们产品和服务的调查\n\n1.2.3 向您推送消息\n信息发布、紧急情况下的安全提醒\n身份验证、安全防范、用户投诉处理、公告通知\n\n1.2.4 为您提供安全保障\n我们使用您的个人信息来预防、发现、调查欺诈、危害安全、非法或违反与我们或其关联方协议、政策或规则的行为，以保护您、其他用户、我们或关联方的合法权益"
            )
            
            // 第二部分 我们如何使用本地存储技术
            PolicySection(
                title = "第二部分 我们如何使用本地存储技术",
                content = "2.1 APP本地存储\n为了提供更好的用户体验和服务功能，我们的APP会在您的移动设备上进行本地数据存储。这些本地存储的信息包括但不限于：\n\n2.1.1 用户偏好设置\n应用界面设置（如主题、字体大小等）\n阅读偏好（如收藏的文章类别、阅读历史等）\n通知设置和推送偏好\n语言和地区设置\n\n2.1.2 缓存数据\n已浏览文章的缓存，以便离线阅读\n图片和媒体文件的临时缓存\n应用配置信息和更新数据\n\n2.1.3 用户行为数据\n应用使用统计信息（如使用时长、功能使用频率等）\n搜索历史和浏览记录\n用户交互行为数据"
            )
            
            // 2.2 本地存储的目的
            PolicySection(
                title = "2.2 本地存储的目的",
                content = "我们使用本地存储技术的目的包括：\n\n提升应用性能和响应速度\n支持离线功能，让您在无网络时也能浏览已缓存的内容\n记住您的个人偏好设置，提供个性化体验\n减少数据流量消耗\n改善应用稳定性和用户体验"
            )
            
            // 2.3 本地存储的管理
            PolicySection(
                title = "2.3 本地存储的管理",
                content = "您可以通过以下方式管理本地存储的数据：\n\n在应用设置中清除缓存数据\n在设备系统设置中管理应用存储权限\n卸载并重新安装应用以清除所有本地数据\n通过应用内的\"清除数据\"功能删除特定类型的存储信息\n\n请注意，清除某些本地存储数据可能会影响应用的正常使用，如需要重新登录、重新设置偏好等。"
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PolicySection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF161823)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = content,
            fontSize = 14.sp,
            color = Color(0xFF374151),
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
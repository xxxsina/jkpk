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
fun UserAgreementScreen(
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
                    text = "用户协议",
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
        
        // 用户协议内容
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
                text = "健康派卡APP用户协议",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF161823),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 第一条 协议的接受
            AgreementSection(
                title = "第一条 协议的接受",
                content = "欢迎使用健康派卡APP（以下简称\"本应用\"或\"我们\"）。本用户协议（以下简称\"本协议\"）是您与本应用运营方之间关于使用本应用服务的法律协议。\n\n在您注册、下载、安装、启动、浏览、使用本应用之前，请您仔细阅读本协议的全部内容。您点击\"同意\"、\"注册\"或以其他方式使用本应用服务，即表示您已充分理解并同意接受本协议的全部条款。如果您不同意本协议的任何条款，请您立即停止使用本应用。"
            )
            
            // 第二条 服务内容
            AgreementSection(
                title = "第二条 服务内容",
                content = "2.1 服务描述\n本应用是一款专注于健康生活的移动应用程序，主要提供以下服务：\n\n• 生活相关文章的阅读\n• 用户注册与账户管理\n• 生活知识分享与交流\n• 个性化生活内容推荐\n\n2.2 服务特点\n• 本应用仅发布与生活相关的内容\n• 所有文章内容均经过专业审核\n• 为注册用户提供个性化服务体验"
            )
            
            // 第三条 用户注册与账户管理
            AgreementSection(
                title = "第三条 用户注册与账户管理",
                content = "3.1 注册条件\n• 用户必须年满18周岁，具有完全民事行为能力\n• 未满18周岁的用户需在监护人同意下使用本应用\n• 用户提供的注册信息必须真实、准确、完整\n\n3.2 注册流程\n• 用户需提供有效的手机号码或邮箱地址\n• 设置安全的登录密码\n• 完成身份验证（如短信验证码或邮箱验证）\n• 同意本用户协议和隐私政策\n\n3.3 账户安全\n• 用户有义务妥善保管账户信息和密码\n• 如发现账户被盗用或存在安全风险，应立即通知我们\n• 用户对其账户下的所有活动承担责任\n\n3.4 账户注销\n• 用户可随时申请注销账户\n• 注销后，账户信息将按照相关法律法规进行处理\n• 注销不影响注销前已产生的权利义务关系"
            )
            
            // 第四条 用户行为规范
            AgreementSection(
                title = "第四条 用户行为规范",
                content = "4.1 合法使用\n用户在使用本应用时，必须遵守以下规定：\n\n• 遵守中华人民共和国相关法律法规\n• 不得发布违法、有害、虚假信息\n• 不得侵犯他人合法权益\n• 不得进行任何危害网络安全的行为\n\n4.2 禁止行为\n用户不得进行以下行为：\n• 进行商业广告或营销活动（除非获得授权）\n• 恶意攻击、骚扰其他用户\n• 使用技术手段干扰应用正常运行\n• 盗用他人账户或冒充他人身份\n\n4.3 内容规范\n• 尊重知识产权，不得侵犯他人著作权"
            )
            
            // 第五条 内容与知识产权
            AgreementSection(
                title = "第五条 内容与知识产权",
                content = "5.1 平台内容\n• 本应用发布的所有文章内容均受知识产权法保护\n• 用户仅获得阅读和个人学习使用的权利\n• 未经授权，不得复制、传播、商业使用平台内容\n\n5.2 用户内容\n• 用户发布的内容，用户保留著作权\n• 用户授权本应用在平台范围内使用其发布的内容\n• 用户保证其发布的内容不侵犯第三方权益\n\n5.3 侵权处理\n• 如发现侵权内容，我们将及时处理\n• 权利人可通过官方渠道举报侵权行为\n• 我们保留删除侵权内容和处罚违规用户的权利"
            )
            
            // 第六条 隐私保护
            AgreementSection(
                title = "第六条 隐私保护",
                content = "6.1 信息收集\n我们可能收集以下信息：\n• 注册时提供的基本信息\n• 使用应用时产生的行为数据\n• 用户主动提供的其他信息\n\n6.2 信息使用\n收集的信息将用于：\n• 提供和改善应用服务\n• 个性化内容推荐\n• 安全防护和风险控制\n• 法律法规要求的其他用途\n\n6.3 信息保护\n• 我们采用行业标准的安全措施保护用户信息\n• 不会向第三方出售用户个人信息\n• 仅在法律要求或用户同意的情况下共享信息\n• 用户有权查询、更正、删除个人信息"
            )
            
            // 第七条 免责声明
            AgreementSection(
                title = "第七条 免责声明",
                content = "7.1 内容免责\n• 本应用提供的健康信息仅供参考，不构成医疗建议\n• 我们不对用户因使用平台信息而产生的后果承担责任\n\n7.2 服务免责\n• 因不可抗力导致的服务中断，我们不承担责任\n• 因用户设备或网络问题导致的使用障碍，我们不承担责任\n• 因第三方原因导致的损失，我们不承担责任\n\n7.3 损失限制\n• 在法律允许的最大范围内，我们的责任限于直接损失\n• 对于间接损失、利润损失等，我们不承担责任"
            )
            
            // 第八条 服务变更与终止
            AgreementSection(
                title = "第八条 服务变更与终止",
                content = "8.1 服务变更\n• 我们有权根据业务需要调整服务内容\n• 重大变更将提前通知用户\n• 用户不同意变更的，可以终止使用服务\n\n8.2 服务暂停\n以下情况下，我们可能暂停服务：\n• 系统维护和升级\n• 发生安全事件\n• 法律法规要求\n• 其他不可抗力因素\n\n8.3 服务终止\n以下情况下，我们可能终止服务：\n• 用户严重违反本协议\n• 用户长期未使用账户\n• 法律法规要求\n• 业务调整需要"
            )
            
            // 第九条 争议解决
            AgreementSection(
                title = "第九条 争议解决",
                content = "9.1 协商解决\n因本协议产生的争议，双方应首先通过友好协商解决。\n\n9.2 法律适用\n本协议的签订、履行、解释均适用中华人民共和国法律。\n\n9.3 管辖法院\n如协商不成，任何一方均可向本应用运营方所在地人民法院提起诉讼。"
            )
            
            // 第十条 其他条款
            AgreementSection(
                title = "第十条 其他条款",
                content = "10.1 协议修改\n• 我们有权根据法律法规变化和业务需要修改本协议\n• 修改后的协议将在应用内公布\n• 用户继续使用服务视为同意修改后的协议\n\n10.2 协议效力\n• 本协议自用户同意之日起生效\n• 如协议部分条款无效，不影响其他条款的效力\n• 本协议的标题仅为方便阅读，不影响条款解释\n\n健康派卡APP运营方"
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AgreementSection(
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
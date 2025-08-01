<?php
/**
 * 客服消息签到接口
 * 获取用户最近七天内最新的一条未确认提问
 * 用于签到页面显示待处理的客服消息
 */

require_once __DIR__ . '/../utils/ApiUtils.php';
require_once __DIR__ . '/../services/UserService.php';
require_once __DIR__ . '/../models/RedisModel.php';
$config = require_once __DIR__ . '/../config/config.php';

// 处理CORS
ApiUtils::handleCors();

// 获取请求方法
$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'POST') {
        handleGetCheckinMessage();
    } else {
        ApiUtils::error('不支持的请求方法', 405);
    }
} catch (Exception $e) {
    error_log('Customer Message Checkin API Error: ' . $e->getMessage());
    ApiUtils::error('服务器内部错误', 500);
}

/**
 * 处理获取签到消息请求
 */
function handleGetCheckinMessage() {
    $redis = RedisModel::getInstance();
    
    // 获取请求参数
    $params = ApiUtils::getRequestParams(['user_id']);
    
    // 从header中获取token
    $token = ApiUtils::getTokenFromHeader();
    
    // 创建用户服务实例
    $userService = new UserService();

    // 验证用户令牌
    if (!$userService->validateUserToken($params['user_id'], $token)) {
        ApiUtils::unauthorized('登录已过期，请退出后重新登陆');
    }
    
    try {
        // 获取用户消息列表的key
        $userKey = "customer_messages:user:{$params['user_id']}";
        
        // 计算七天前的时间戳
        $sevenDaysAgo = time() - (7 * 24 * 60 * 60);
        
        // 使用zRevRange获取所有消息ID列表（按时间倒序）
        $messageIds = $redis->zRevRange($userKey, 0, -1);
        
        $latestUnconfirmedMessage = null;
        
        // 遍历消息，找到最新的未确认消息
        foreach ($messageIds as $messageId) {
            $messageKey = "customer_messages:userId:{$params['user_id']}:msgId:{$messageId}";
            $messageData = $redis->hGetAll($messageKey);

            if (!empty($messageData)) {
                // 检查消息是否在最近七天内
                $createTime = isset($messageData['createtime']) ? intval($messageData['createtime']) : 0;
                if ($createTime < $sevenDaysAgo) {
                    continue; // 跳过七天前的消息
                }

                // 检查是否未确认 (is_overcome = 0)
                if (isset($messageData['is_overcome']) && intval($messageData['is_overcome']) == 0) {
                    // 格式化时间字段
                    if (isset($messageData['createtime'])) {
                        $messageData['createtime_formatted'] = date('Y-m-d H:i:s', $messageData['createtime']);
                    }
                    if (isset($messageData['updatetime'])) {
                        $messageData['updatetime_formatted'] = date('Y-m-d H:i:s', $messageData['updatetime']);
                    }

                    // 格式化媒体文件URL
                    $messageData['image'] = formatMediaUrl($messageData['image'] ?? '', 'image');
                    $messageData['video'] = formatMediaUrl($messageData['video'] ?? '', 'video');
                    $messageData['answer_image'] = formatMediaUrlAdmin($messageData['answer_image'] ?? '', 'image');
                    $messageData['answer_video'] = formatMediaUrlAdmin($messageData['answer_video'] ?? '', 'video');
                    
                    // 转换数值字段
                    $messageData['id'] = intval($messageData['id']);
                    $messageData['user_id'] = intval($messageData['user_id']);
                    $messageData['looked'] = intval($messageData['looked']);
                    $messageData['is_overcome'] = intval($messageData['is_overcome']);
                    $messageData['createtime'] = intval($messageData['createtime']);
                    $messageData['updatetime'] = intval($messageData['updatetime']);
                    
                    $latestUnconfirmedMessage = $messageData;
                    break; // 找到最新的未确认消息，跳出循环
                }
            }
        }
        
        // 返回结果
        $result = [
            'message' => $latestUnconfirmedMessage,
            'has_unconfirmed' => $latestUnconfirmedMessage !== null
        ];
        
        ApiUtils::success('获取成功', $result);
        
    } catch (Exception $e) {
        error_log('Redis Error: ' . $e->getMessage());
        ApiUtils::error('获取签到消息失败，请稍后重试', 500);
    }
}

/**
 * 格式化媒体文件URL
 * @param string $mediaUrl 媒体文件URL
 * @param string $type 媒体类型 (image/video)
 * @return string 格式化后的URL
 */
function formatMediaUrl($mediaUrl, $type = 'image') {
    global $config;
    if (empty($mediaUrl)) {
        return '';
    }

    // 外部URL，直接返回
    if (filter_var($mediaUrl, FILTER_VALIDATE_URL)) {
        return $mediaUrl;
    }

    // 本地文件，返回完整HTTP地址
//    $basePath = $type === 'video' ? '/data/videos/' : '/data/images/';
    return $config['HTTP_HOST'] . $mediaUrl;
}

/**
 * ADMIN格式化媒体文件URL
 * @param string $mediaUrl 媒体文件URL
 * @param string $type 媒体类型 (image/video)
 * @return string 格式化后的URL
 */
function formatMediaUrlAdmin($mediaUrl, $type = 'image') {
    global $config;
    if (empty($mediaUrl)) {
        return '';
    }

    // 外部URL，直接返回
    if (filter_var($mediaUrl, FILTER_VALIDATE_URL)) {
        return $mediaUrl;
    }

    // 本地文件，返回完整HTTP地址
    return $config['HTTP_HOST'] . $mediaUrl;
}

/**
 * API使用示例:

1. 获取签到消息:
   POST http://jiankangpaika.blcwg.com/jkpk/api/customer_message_checkin.php
   Headers: Authorization: Bearer {token}
   Body: {
       "user_id": 123
   }

成功响应（有未确认消息）:
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "message": {
            "id": 1,
            "user_id": 123,
            "status": "new",// 提问状态，new新问题，answer已回答
            "looked": 0,// 管理员是否查看，1是，0否
            "realname": "张三",// 用户姓名
            "mobile": "13800138000",// 用户手机号
            "problem": "问题描述",// 用户提问的内容
            "answer": "",// 回答的内容
            "image": "",// 用户提问的图片
            "video": "",// 用户提问的视频
            "answer_image": "", // 回答的图片
            "answer_video": "", // 回答的视频
            "is_overcome": 0,   // 是否解决，用户点击，1是，0否
            "createtime": 1752411019,
            "updatetime": 1752411019,
            "createtime_formatted": "2025-07-13 20:50:19",
            "updatetime_formatted": "2025-07-13 20:50:19"
        },
        "has_unconfirmed": true
    },
    "timestamp": 1752411019,
    "datetime": "2025-07-13 20:50:19"
}

成功响应（无未确认消息）:
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "message": null,
        "has_unconfirmed": false
    },
    "timestamp": 1752411019,
    "datetime": "2025-07-13 20:50:19"
}

失败响应:
{
    "code": 401,
    "message": "登录已过期，请退出后重新登陆",
    "timestamp": 1752411019,
    "datetime": "2025-07-13 20:50:19"
}

使用场景:
- 在签到页面调用此接口，检查是否有待处理的客服消息
- 如果has_unconfirmed为true，可以在签到页面显示提醒用户处理客服消息
- 用户可以点击消息跳转到客服消息详情页面进行处理
 */
?>
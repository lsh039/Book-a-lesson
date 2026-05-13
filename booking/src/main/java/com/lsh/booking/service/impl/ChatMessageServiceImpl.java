package com.lsh.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsh.booking.common.UserContext;
import com.lsh.booking.entity.ChatMessage;
import com.lsh.booking.VO.ChatSessionVO;
import com.lsh.booking.mapper.ChatMessageMapper;
import com.lsh.booking.service.IChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsh.booking.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lsh
 * @since 2026-04-12
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public List<ChatMessage> getUnread(Long userId) {
        return chatMessageMapper.getUnread(userId);
    }

    @Override
    public void markRead(Long senderId) {
        // 1. 获取我自己 (JwtInterceptor 帮我们存的)
        Long myId = UserContext.getUserId();

        // 2. 查出对方发给我、且我还没读的消息
        // (假设你的实体类字段是 toUserId, fromUserId, status，如果不是请自行修改)
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getToUserId, myId)
                .eq(ChatMessage::getFromUserId, senderId)
                .eq(ChatMessage::getStatus, 0); // 0代表未读

        List<ChatMessage> unreadList = this.list(queryWrapper);
        if (unreadList.isEmpty()) {
            return; // 没有未读消息，直接结束
        }

        // 3. 批量更新为已读 (1代表已读)
        for (ChatMessage msg : unreadList) {
            msg.setStatus(1);
        }
        this.updateBatchById(unreadList);

        // 4. 🚀 灵魂一步：通过 WebSocket 给对方发个回执！
        String receiptMessage = "{\"type\":\"read\", \"fromUserId\":" + myId + "}";
        WebSocketServer.sendMessageToUser(senderId, receiptMessage);
    }

    @Override
    public List<ChatMessage> getRecentChats(Long userId) {
        return chatMessageMapper.getRecentChats(userId);
    }

    @Override
    public Map<String, Integer> getUnreadCountMap(Long userId) {
        // 1. 调用 Mapper 查出原始数据
        List<Map<String, Object>> list = chatMessageMapper.getUnreadCountList(userId);

        // 2. 准备一个干净的 Map 用来返回给前端
        Map<String, Integer> resultMap = new HashMap<>();

        if (list != null) {
            for (Map<String, Object> map : list) {
                String sessionId = (String) map.get("sessionId");
                // ⚠️ 避坑：数据库的 COUNT() 返回的通常是 Long 类型，直接强转 Integer 会报错，用 Number 过渡一下最安全
                Integer count = ((Number) map.get("unreadCount")).intValue();

                resultMap.put(sessionId, count);
            }
        }
        return resultMap;
    }

    public List<ChatSessionVO> getSessionList(Long userId) {

        // 1️⃣ 查最近消息
        List<ChatMessage> recentList = chatMessageMapper.getRecentChats(userId);

        // 2️⃣ 查未读数
        List<Map<String, Object>> unreadList = chatMessageMapper.getUnreadCount(userId);

        // 3️⃣ 转 Map（sessionId -> count）
        Map<String, Integer> unreadMap = new HashMap<>();
        for (Map<String, Object> m : unreadList) {
            String sessionId = (String) m.get("session_id");
            Long count = (Long) m.get("unreadCount");
            unreadMap.put(sessionId, count.intValue());
        }

        // 4️⃣ 组装 VO
        List<ChatSessionVO> result = new ArrayList<>();

        for (ChatMessage msg : recentList) {

            ChatSessionVO vo = new ChatSessionVO();

            vo.setSessionId(msg.getSessionId());
            vo.setLastMessage(msg.getContent());
            vo.setUnreadCount(unreadMap.getOrDefault(msg.getSessionId(), 0));
            vo.setLastTime(msg.getCreateTime().toString());

            // 👉 算对方是谁（核心！）
            Long targetUserId = msg.getFromUserId().equals(userId)
                    ? msg.getToUserId()
                    : msg.getFromUserId();

            vo.setTargetUserId(targetUserId);

            result.add(vo);
        }

        return result;
    }
}

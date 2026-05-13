package com.lsh.booking.websocket;

import com.alibaba.fastjson.JSON;
import com.lsh.booking.entity.ChatMessage;
import com.lsh.booking.service.IChatMessageService;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/{userId}")
@Component
public class WebSocketServer {

    // 存所有在线用户
    private static final ConcurrentHashMap<Long, Session> sessionMap = new ConcurrentHashMap<>();

    private static IChatMessageService chatMessageService;

    @Autowired
    public void setChatMessageService(IChatMessageService chatMessageService) {
        WebSocketServer.chatMessageService = chatMessageService;
    }

    /**
     * 建立连接
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        sessionMap.put(userId, session);
        System.out.println("用户连接：" + userId);

        // ================= 🚀 第三步：上线时补发离线消息 =================
        List<ChatMessage> unreadList = chatMessageService.getUnread(userId);

        if (unreadList != null && !unreadList.isEmpty()) {
            for (ChatMessage msg : unreadList) {
                try {
                    // 推送给刚上线的用户
                    session.getBasicRemote().sendText(JSON.toJSONString(msg));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 推送完立刻标记为已读
            chatMessageService.markRead(userId);
            System.out.println("✅ 已为用户 " + userId + " 补发 " + unreadList.size() + " 条离线消息");
        }
        // ===============================================================
    }

    /**
     * 收到消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") Long fromUserId) {
        System.out.println("收到消息：" + message);


        // 转对象并补全发送人 ID
        ChatMessage msg = JSON.parseObject(message, ChatMessage.class);
        msg.setFromUserId(fromUserId);

        Long toUserId = msg.getToUserId();
        Long a = fromUserId;
        Long b = toUserId;

        String sessionId = a < b ? a + "_" + b : b + "_" + a;

        msg.setSessionId(sessionId);


        // ================= 🚀 第一步：无论在不在，先存数据库 =================
        // 这里建议你的 msg 对象里有个状态字段，比如 status: 0(未读), 1(已读)
        // 存库时默认是未读状态
        chatMessageService.save(msg);
        // ===============================================================

        // ================= 🚀 第二步：判断在线状态 =================
        Session toSession = sessionMap.get(toUserId);

        if (toSession != null && toSession.isOpen()) {
            try {
                toSession.getBasicRemote().sendText(JSON.toJSONString(msg));


                System.out.println("✅ 实时推送给：" + toUserId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // ===============================================================
    }

    /**
     * 断开连接
     */
    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        sessionMap.remove(userId);
        System.out.println("用户断开：" + userId);
    }

    /**
     * 🌟 给外部调用的核心方法：向指定用户推送任意系统消息（比如已读回执）
     */
    public static void sendMessageToUser(Long userId, String message) {
        Session session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
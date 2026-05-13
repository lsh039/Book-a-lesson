package com.lsh.booking.VO;

import lombok.Data;

@Data
public class ChatSessionVO {

    private String sessionId;

    private String lastMessage;

    private Integer unreadCount;

    private Long targetUserId; // 👉 对方是谁（前端要用）

    private String lastTime; // 可选（加分项）
}
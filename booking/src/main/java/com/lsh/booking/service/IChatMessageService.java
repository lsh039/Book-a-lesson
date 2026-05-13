package com.lsh.booking.service;

import com.lsh.booking.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsh.booking.VO.ChatSessionVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lsh
 * @since 2026-04-12
 */
public interface IChatMessageService extends IService<ChatMessage> {

    List<ChatMessage> getUnread(Long userId);

    void markRead(Long userId);

    List<ChatMessage> getRecentChats(Long userId);

    Map<String, Integer> getUnreadCountMap(Long userId);

    List<ChatSessionVO> getSessionList(Long userId);
}

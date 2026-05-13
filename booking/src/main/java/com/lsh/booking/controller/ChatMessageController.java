package com.lsh.booking.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsh.booking.common.Result;
import com.lsh.booking.entity.ChatMessage;
import com.lsh.booking.VO.ChatSessionVO;
import com.lsh.booking.service.IChatMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lsh
 * @since 2026-04-12
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final IChatMessageService chatMessageService;

    /*
    * 获取最近消息
    * */
    @GetMapping("/recent")
    public Result<?> recent(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        List<ChatMessage> list = chatMessageService.getRecentChats(userId);

        return Result.success( list);
    }

    /*
    *
    * */
    @GetMapping("/history")
    public Result<List<ChatMessage>> history(
            @RequestParam String sessionId,
            @RequestParam int page,
            @RequestParam int size) {

        Page<ChatMessage> pageData = chatMessageService.page(
                new Page<>(page, size),
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByDesc(ChatMessage::getCreateTime)
        );

        return Result.success(pageData.getRecords());
    }

    /**
     * 获取未读消息
     */

    @GetMapping("/unread")
    public Result<Map<String, Integer>> unread(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Map<String, Integer> map = chatMessageService.getUnreadCountMap(userId);

        return Result.success(map);
    }

    @GetMapping("/session/list")
    public Result<List<ChatSessionVO>> sessionList(HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        return Result.success(chatMessageService.getSessionList(userId));
    }
}

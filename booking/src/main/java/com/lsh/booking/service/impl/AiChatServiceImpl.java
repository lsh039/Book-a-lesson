package com.lsh.booking.service.impl;

import com.lsh.booking.service.AiChatService;
import com.lsh.booking.service.ICourseService;
import com.lsh.booking.service.IReservationService;
import com.lsh.booking.tool.ReservationTool;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;



    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             ChatMemory chatMemory,
                             ReservationTool reservationTool
                              ) {



        this.chatClient = chatClientBuilder
                .defaultSystem("你是智能预约系统的前台美女客服。你的任务是解答用户疑问，并主动调用工具帮助用户查询或预约课程。")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(reservationTool)
                .build();
    }

    @Override
    public String chat(String sessionId, String message){
        try {
            return chatClient.prompt()
                    .user(message)
                    .advisors(a -> a.param("conversationId",sessionId))
                    .call()
                    .content();

        } catch (Exception e) {
            e.printStackTrace();
            return "当前智能客服大脑正在升级，请稍后再试哦~";
        }
    }


}
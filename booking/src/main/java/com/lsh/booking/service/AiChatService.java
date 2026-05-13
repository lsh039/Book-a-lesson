package com.lsh.booking.service;

import org.springframework.stereotype.Service;

@Service
public interface AiChatService {

    Object chat(String sessionId, String message);
}

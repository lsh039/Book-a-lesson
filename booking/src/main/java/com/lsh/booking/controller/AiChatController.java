package com.lsh.booking.controller;

import com.lsh.booking.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;
    @PostMapping("/chat")
    public String chat(
            @RequestParam String sessionId,
            @RequestBody String message
    ){
        return (String) aiChatService.chat(sessionId,message);
    }
}

package com.example.rest.controller;

import com.example.dto.MessageRequest;
import com.example.dto.MessageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
public class RestMessageController {

    @PostMapping
    public MessageResponse handleMessage(@RequestBody MessageRequest request) {
        String content = request.getContent();

        String reply = "Hi from REST! You said: " + content;
        return new MessageResponse(reply);
    }

}
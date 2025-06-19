package com.example.rest.controller;

import com.example.dto.MessageRequest;
import com.example.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class RestMessageController {

    private static final Logger log = LoggerFactory.getLogger(RestMessageController.class);

    @PostMapping("/message")
    public ResponseEntity<MessageResponse> handleMessage(@RequestBody MessageRequest request) {
        log.info("[REST Service] 메시지 수신: {}", request.getContent());
        if (request == null || request.getContent() == null || request.getContent().isEmpty()) {
            log.warn("[REST Service] 유효하지 않은 요청: content가 비어있음.");
            // Bad Request 응답 (직접 에러 응답 구성)
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Content cannot be empty."));
        }
        String replyContent = "REST Response: " + request.getContent().toUpperCase();
        MessageResponse response = new MessageResponse(replyContent);
        log.info("[REST Service] 응답 전송: {}", replyContent);
        return ResponseEntity.ok(response);
    }
}
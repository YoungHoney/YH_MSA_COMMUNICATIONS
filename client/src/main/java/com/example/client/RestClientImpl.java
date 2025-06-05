package com.example.client.impl;

import com.example.client.Client;
import com.example.dto.MessageRequest;
import com.example.dto.MessageResponse;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class RestClientImpl implements Client {

    @Override
    public void sendMessage(String content) {
        RestTemplate restTemplate = new RestTemplate();

        MessageRequest request = new MessageRequest();
        request.setContent(content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MessageRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<MessageResponse> response = restTemplate.exchange(
                "http://localhost:8080/api/message",
                HttpMethod.POST,
                entity,
                MessageResponse.class
        );

        System.out.println("[REST] 응답: " + response.getBody().getReply());
    }

}
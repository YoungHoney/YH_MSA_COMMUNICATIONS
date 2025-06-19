package com.example.dto;


import lombok.Data;

@Data
public class MessageRequest {
    private String content;


    public MessageRequest(){}
    public MessageRequest(String content) {
        this.content = content;
    }


}

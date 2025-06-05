package com.example.dto;

public class MessageResponse {

    public String reply;

    public MessageResponse() {}
    public MessageResponse(String reply) {
        this.reply = reply;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}

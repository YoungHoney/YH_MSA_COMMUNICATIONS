package com.example;

import com.example.client.Client;
import com.example.client.impl.RestClientImpl;


public class ClientApplication {
    public static void main(String[] args) {
        System.out.println("== 클라이언트 시작 ==");

        Client client=new RestClientImpl(); //추후 factory같은걸로 리팩토링 필요
        client.sendMessage("hello world gyh");

        System.out.println("== 클라이언트 종료 ==");
    }

}
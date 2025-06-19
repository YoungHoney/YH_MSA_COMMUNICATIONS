package com.example.client;

import org.springframework.stereotype.Component;

@Component // Spring 빈으로 등록
public class ClientFactory {

    private final RestClientImpl restClient;
    private final GrpcClientImpl grpcClient;

    // 생성자 주입 (Spring이 자동으로 빈을 찾아 주입해줍니다.)
    public ClientFactory(RestClientImpl restClient, GrpcClientImpl grpcClient) {
        this.restClient = restClient;
        this.grpcClient = grpcClient;
    }

    public Client getClient(ClientType type) {
        switch (type) {
            case REST:
                return restClient;
            case GRPC:
                return grpcClient;
            default:
                throw new IllegalArgumentException("Unknown client type: " + type);
        }
    }

    public enum ClientType {
        REST,
        GRPC
        // RABBITMQ 등은 추후 추가
    }
}
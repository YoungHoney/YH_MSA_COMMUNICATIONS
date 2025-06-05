package com.example.client;

import com.example.client.Client;
import com.example.grpc.MessageRequest;
import com.example.grpc.MessageResponse;
import com.example.grpc.MessageServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClientImpl implements Client {

    @Override
    public void sendMessage(String content) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        MessageServiceGrpc.MessageServiceBlockingStub stub = MessageServiceGrpc.newBlockingStub(channel);

        MessageRequest request = MessageRequest.newBuilder()
                .setContent(content)
                .build();

        MessageResponse response = stub.sendMessage(request);

        System.out.println("[gRPC] 응답: " + response.getReply());

        channel.shutdown();
    }

}
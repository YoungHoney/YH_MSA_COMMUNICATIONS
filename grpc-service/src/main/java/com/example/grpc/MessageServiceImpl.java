package com.example.grpc;

import com.example.grpc.MessageRequest; //grpc통신에선 dto가 아니라 gRPC 생성된 코드가 필요함.
import com.example.grpc.MessageResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class MessageServiceImpl extends MessageServiceGrpc.MessageServiceImplBase {

    @Override
    public void sendMessage(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        String content = request.getContent();
        String reply = "Hi from gRPC! You said: " + content;

        MessageResponse response = MessageResponse.newBuilder() //grpc .proto에서 생성된 클래스들은 new로 생성될수없음, 빌더패턴으로만 생성가능
                .setReply(reply)
                .build();


        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
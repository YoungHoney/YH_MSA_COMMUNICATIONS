package com.example.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService // gRPC 서비스임을 선언 (Spring Boot gRPC Starter 사용 시)
public class GrpcMessageServiceImpl extends MessageServiceGrpc.MessageServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcMessageServiceImpl.class);


    // 1. Unary RPC (단항 RPC), 동기, 블로킹 통신
    @Override
    public void sendUnaryMessage(MessageRequestGrpc request, StreamObserver<MessageResponseGrpc> responseObserver) {
        log.info("[Unary RPC - Server] 메시지 수신: {}", request.getContent());

        try {
            // 💡 의도적인 지연 추가: 클라이언트가 블로킹되는 것을 체감
            log.info("[Unary RPC - Server] 2초 지연 시작...");
            Thread.sleep(2000); // 2초 지연
            log.info("[Unary RPC - Server] 2초 지연 종료.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Unary RPC - Server] 지연 중 인터럽트 발생: {}", e.getMessage());
        }

        String replyContent = "Unary Response: " + request.getContent().toUpperCase();
        MessageResponseGrpc response = MessageResponseGrpc.newBuilder()
                .setReply(replyContent)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        log.info("[Unary RPC - Server] 응답 전송: {}", replyContent);
    }




    // 2. Server Streaming RPC, 동기, 블로킹 통신
    @Override
    public void getServerStreamMessages(UserRequest request, StreamObserver<MessageResponseGrpc> responseObserver) {
        log.info("[Server Streaming - Server] 메시지 수신: userId={}", request.getUserId());

        try {
            for (int i = 0; i < 5; i++) {
                String message = String.format("Server Stream Message %d for User %s", i + 1, request.getUserId());

                MessageResponseGrpc response = MessageResponseGrpc.newBuilder()
                        .setReply(message)
                        .build();
                responseObserver.onNext(response);
                log.info("[Server Streaming - Server] 메시지 전송: {}", message);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Server Streaming - Server] 스트리밍 중 인터럽트 발생: {}", e.getMessage());
            responseObserver.onError(io.grpc.Status.CANCELLED.withDescription("Server stream interrupted").asRuntimeException());
        } finally {
            responseObserver.onCompleted();
            log.info("[Server Streaming - Server] 스트림 전송 완료.");
        }
    }


    // 💡 3. Client Streaming RPC (클라이언트 스트리밍) 구현, 비동기, 논블로킹
    @Override
    public StreamObserver<LogEntry> sendClientStreamLogs(StreamObserver<UploadStatusResponse> responseObserver) {
        log.info("[Client Streaming - Server] 클라이언트 스트림 수신 준비.");

        return new StreamObserver<LogEntry>() {
            private int receivedLogCount = 0;
            private StringBuilder receivedLogContent = new StringBuilder();

            @Override
            public void onNext(LogEntry logEntry) {
                // 클라이언트로부터 로그 메시지를 받을 때마다 호출됨.
                receivedLogCount++;
                receivedLogContent.append(String.format("Level: %s, Msg: %s\n", logEntry.getLevel(), logEntry.getMessage()));
                log.info("[Client Streaming - Server] 로그 수신: Level={}, Msg={}", logEntry.getLevel(), logEntry.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                // 스트림 처리 중 에러 발생 시 호출됨.
                log.error("[Client Streaming - Server] 클라이언트 스트림 에러 발생: {}", t.getMessage());
                responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("Server encountered an error processing stream").asRuntimeException());
            }

            @Override
            public void onCompleted() {
                // 클라이언트가 모든 메시지 전송을 완료하고 스트림을 닫을 때 호출됨.
                log.info("[Client Streaming - Server] 클라이언트 스트림 완료. 총 {}개의 로그 수신.", receivedLogCount);
                log.info("[Client Streaming - Server] 수신된 전체 로그 내용:\n{}", receivedLogContent.toString());

                // 최종 응답 메시지 생성 및 전송
                UploadStatusResponse response = UploadStatusResponse.newBuilder()
                        .setStatus("SUCCESS")
                        .setReceivedCount(receivedLogCount)
                        .build();
                responseObserver.onNext(response); // 최종 응답 전송
                responseObserver.onCompleted();    // 서버 스트림 종료
                log.info("[Client Streaming - Server] 최종 응답 전송: status={}, count={}", response.getStatus(), response.getReceivedCount());
            }
        };
    }

    // 💡 4. Bidirectional Streaming RPC (양방향 스트리밍) 구현, 비동기, 논블로킹
    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
        log.info("[Bidirectional Streaming - Server] 양방향 스트림 수신 준비.");

        return new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage clientMessage) {
                // 클라이언트로부터 메시지를 받을 때마다 호출됨 .
                log.info("[Bidirectional Streaming - Server] 클라이언트 메시지 수신 (From: {}, Msg: {})",
                        clientMessage.getSender(), clientMessage.getMessage());

                // 받은 메시지에 대한 응답을 즉시 클라이언트로 전송
                String reply = String.format("Server received '%s' from %s. Sending back a reply.",
                        clientMessage.getMessage(), clientMessage.getSender());
                ChatMessage serverMessage = ChatMessage.newBuilder()
                        .setSender("Server")
                        .setMessage(reply)
                        .build();
                responseObserver.onNext(serverMessage); // 서버 -> 클라이언트로 메시지 전송
                log.info("[Bidirectional Streaming - Server] 서버 메시지 전송: {}", reply);
            }

            @Override
            public void onError(Throwable t) {
                // 스트림 처리 중 에러 발생 시 호출됨.
                log.error("[Bidirectional Streaming - Server] 양방향 스트림 에러 발생: {}", t.getMessage());
                responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("Server encountered an error in chat stream").asRuntimeException());
            }

            @Override
            public void onCompleted() {
                // 클라이언트가 스트림을 닫을 때 호출됨.
                log.info("[Bidirectional Streaming - Server] 클라이언트 스트림 완료. 서버 스트림 종료.");
                responseObserver.onCompleted(); // 서버 스트림 종료
            }
        };
    }


}
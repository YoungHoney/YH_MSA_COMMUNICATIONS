package com.example.client;

import com.example.grpc.MessageRequestGrpc;
import com.example.grpc.MessageServiceGrpc;
import com.example.grpc.MessageResponseGrpc;
import com.example.grpc.UserRequest;
import com.example.grpc.LogEntry;
import com.example.grpc.UploadStatusResponse;
import com.example.grpc.ChatMessage;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GrpcClientImpl implements Client {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientImpl.class);

    private final ManagedChannel channel;
    private final MessageServiceGrpc.MessageServiceBlockingStub blockingStub;
    private final MessageServiceGrpc.MessageServiceStub asyncStub;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    public GrpcClientImpl(
            @Value("${grpc.server.host}") String grpcHost,
            @Value("${grpc.server.port}") int grpcPort
    ) {
        log.info("gRPC 서버에 연결을 시도합니다. -> host: {}, port: {}", grpcHost, grpcPort);

        // 주입받은 값으로 채널을 생성합니다.
        this.channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();

        this.blockingStub = MessageServiceGrpc.newBlockingStub(channel);
        this.asyncStub = MessageServiceGrpc.newStub(channel);
    }
/*
    @PreDestroy
    public void shutdown() {
        log.info("[gRPC Client] 채널 종료 시도.");
        channel.shutdown();
    }*/

    @PreDestroy
    public void shutdown() {
        log.info("[gRPC Client] 스케줄러 종료 시도.");
        scheduler.shutdown();
    }

    // 1. Unary 통신
    @Override
    public String sendMessage(String content) {
        MessageRequestGrpc request = MessageRequestGrpc.newBuilder()
                .setContent(content)
                .build();

        try {
            log.info("[Unary RPC - Client] 메시지 전송 시도: {}", content);
            long startTime = System.currentTimeMillis();
            MessageResponseGrpc response = blockingStub.sendUnaryMessage(request);

            long endTime = System.currentTimeMillis();
            log.info("[Unary RPC - Client] 응답 수신 (처리 시간: {}ms): {}", (endTime - startTime), response.getReply());
            return "[Unary RPC] 응답: " + response.getReply();
        } catch (StatusRuntimeException e) {
            log.error("[Unary RPC - Client] RPC failed: {} - {}", e.getStatus(), e.getMessage());
            return "[Unary RPC] 통신 실패: " + e.getStatus().getCode() + " - " + e.getMessage();
        } catch (Exception e) {
            log.error("[Unary RPC - Client] 예기치 않은 오류 발생: {}", e.getMessage(), e);
            return "[Unary RPC] 예기치 않은 오류: " + e.getMessage();
        }
    }

    // 2. Server Streaming RPC 호출 메서드
    public String getServerStreamMessages(String userId) {
        UserRequest request = UserRequest.newBuilder()
                .setUserId(userId)
                .build();

        StringBuilder receivedMessages = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1); // 💡 비동기 작업 완료 대기용 Latch

        StreamObserver<MessageResponseGrpc> responseObserver = new StreamObserver<MessageResponseGrpc>() {
            @Override
            public void onNext(MessageResponseGrpc response) {
                log.info("[Server Streaming - Client] 메시지 수신: {}", response.getReply());
                receivedMessages.append(response.getReply()).append("\n");
            }

            @Override
            public void onError(Throwable t) {
                log.error("[Server Streaming - Client] 스트림 에러 발생: {}", t.getMessage());
                receivedMessages.append("Error: ").append(t.getMessage()).append("\n");
                latch.countDown(); // 에러 발생 시 Latch 감소
            }

            @Override
            public void onCompleted() {
                log.info("[Server Streaming - Client] 스트림 완료.");
                latch.countDown(); // 완료 시 Latch 감소
            }
        };

        log.info("[Server Streaming - Client] 요청 전송: userId={}", userId);
        asyncStub.getServerStreamMessages(request, responseObserver);

        // 💡 비동기 작업 완료를 기다림
        try {
            latch.await(5, TimeUnit.SECONDS); // 최대 5초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Server Streaming - Client] 대기 중 인터럽트 발생.", e);
        }

        return "[Server Streaming] 호출 완료. 수신 메시지:\n" + receivedMessages.toString();
    }


    // 💡 3. Client Streaming RPC (클라이언트 스트리밍) 호출 메서드
    public String sendClientStreamLogs() {
        CountDownLatch latch = new CountDownLatch(1); // 비동기 작업 완료 대기용 Latch
        StringBuilder statusMessage = new StringBuilder();

        // 서버로부터 최종 응답을 받을 StreamObserver
        StreamObserver<UploadStatusResponse> responseObserver = new StreamObserver<UploadStatusResponse>() {
            @Override
            public void onNext(UploadStatusResponse response) {
                log.info("[Client Streaming - Client] 최종 응답 수신: Status={}, Count={}", response.getStatus(), response.getReceivedCount());
                statusMessage.append("Status: ").append(response.getStatus()).append(", Received: ").append(response.getReceivedCount());
            }

            @Override
            public void onError(Throwable t) {
                log.error("[Client Streaming - Client] 스트림 에러 발생: {}", t.getMessage());
                statusMessage.append("Error: ").append(t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("[Client Streaming - Client] 최종 응답 완료.");
                latch.countDown();
            }
        };

        // 서버로 요청 스트림을 보낼 StreamObserver
        StreamObserver<LogEntry> requestObserver = asyncStub.sendClientStreamLogs(responseObserver);

        try {

            log.info("[Client Streaming - Client] 로그 스트림 전송 시작...");
            for (int i = 0; i < 3; i++) {
                LogEntry logEntry = LogEntry.newBuilder()
                        .setLevel("INFO")
                        .setMessage("Log message from client " + (i + 1))
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                requestObserver.onNext(logEntry); // 로그 메시지 전송
                log.info("[Client Streaming - Client] 로그 전송: {}", logEntry.getMessage());
                Thread.sleep(200); // 짧은 간격
            }
            log.info("[Client Streaming - Client] 로그 스트림 전송 완료. 클라이언트 스트림 종료.");
            requestObserver.onCompleted(); // 클라이언트 스트림 종료 알림
        } catch (RuntimeException | InterruptedException e) {
            log.error("[Client Streaming - Client] 로그 전송 중 오류 발생: {}", e.getMessage());
            requestObserver.onError(e); // 오류 발생 시 스트림에 에러 알림
        }

        // 비동기 작업 완료 대기
        try {
            latch.await(5, TimeUnit.SECONDS); // 최대 5초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Client Streaming - Client] 대기 중 인터럽트 발생.", e);
        }

        return "[Client Streaming] 호출 완료. 최종 상태: " + statusMessage.toString();
    }


    // 💡 4. Bidirectional Streaming RPC (양방향 스트리밍) 호출 메서드

    public CountDownLatch chatWithServer(String user) {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder chatLog = new StringBuilder();

        StreamObserver<ChatMessage> responseObserver = new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage message) {
                log.info("[Bidirectional Streaming - Client] 메시지 수신 (From: {}, Msg: {})", message.getSender(), message.getMessage());
                chatLog.append(String.format("[%s] %s\n", message.getSender(), message.getMessage()));
            }

            @Override
            public void onError(Throwable t) {
                // 이 블록은 이제 실제 네트워크 오류 등이 발생했을 때만 호출됩니다.
                log.error("[Bidirectional Streaming - Client] 스트림 에러 발생", t);
                chatLog.append("Error: ").append(t.getMessage()).append("\n");
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("[Bidirectional Streaming - Client] 서버 스트림 완료. 최종 채팅 로그:\n{}", chatLog.toString());
                latch.countDown();
            }
        };

        StreamObserver<ChatMessage> requestObserver = asyncStub.chat(responseObserver);
        AtomicInteger messageCount = new AtomicInteger(0);

        // ⭐️ ScheduledFuture<?> 변수를 선언하여 스케줄링 작업을 제어할 수 있도록 합니다.
        // 람다 내부에서 참조해야 하므로, 배열이나 final 래퍼 클래스로 감쌉니다.
        final ScheduledFuture<?>[] self = new ScheduledFuture<?>[1];

        // ⭐️ 스케줄링 작업을 self 변수에 할당합니다.
        self[0] = scheduler.scheduleAtFixedRate(() -> {
            try {
                int count = messageCount.incrementAndGet();

                // ⭐️ 3번보다 많이 보내려고 하면, 스트림을 정상 종료하고 스케줄링을 취소합니다.
                if (count > 3) {
                    log.info("[Bidirectional Streaming - Client] 3회 전송 완료. 클라이언트 스트림을 정상 종료합니다.");
                    // ⭐️ 1. 오류가 아닌, 정상 완료 신호를 보냅니다.
                    requestObserver.onCompleted();
                    // ⭐️ 2. 스케줄링 작업을 취소합니다. (false: 현재 진행중인 작업은 완료하도록 둠)
                    self[0].cancel(false);
                    return; // 람다 실행 종료
                }

                String clientMsg = String.format("Hello Server, this is %s - Message %d", user, count);
                ChatMessage message = ChatMessage.newBuilder()
                        .setSender(user)
                        .setMessage(clientMsg)
                        .build();
                requestObserver.onNext(message);
                log.info("[Bidirectional Streaming - Client] 클라이언트 메시지 전송: {}", clientMsg);

            } catch (Exception e) {
                // 예상치 못한 실제 오류 발생 시 처리
                log.error("[Bidirectional Streaming - Client] 메시지 전송 스케줄링 중 예기치 않은 오류 발생", e);
                requestObserver.onError(e);
                self[0].cancel(true); // 즉시 중단
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        return latch;
    }
}
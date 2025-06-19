package com.example;

import com.example.client.Client;
import com.example.client.ClientFactory;
import com.example.client.GrpcClientImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(ClientFactory clientFactory) {
        return args -> {
            System.out.println("== 클라이언트 시작 ==");

            // REST 클라이언트 테스트 (기존과 동일)
            Client restClient = clientFactory.getClient(ClientFactory.ClientType.REST);
            String restResponse = restClient.sendMessage("Hello from REST client!");
            System.out.println(restResponse);

            System.out.println("\n-----------------------------------\n");

            // 모든 gRPC 테스트는 GrpcClientImpl 인스턴스를 사용
            GrpcClientImpl grpcClient = (GrpcClientImpl) clientFactory.getClient(ClientFactory.ClientType.GRPC);

            // 1. Unary RPC 테스트
            System.out.println("--- Unary RPC Test ---");
            String unaryResponse = grpcClient.sendMessage("Hello from Unary gRPC client!");
            System.out.println(unaryResponse);
            System.out.println("Unary RPC 호출 이후 바로 실행되는 코드 (지연 후 출력 확인).");

            System.out.println("\n-----------------------------------\n");

            // 2. Server Streaming RPC 테스트
            System.out.println("--- Server Streaming RPC Test ---");
            String serverStreamResult = grpcClient.getServerStreamMessages("user123");
            System.out.println(serverStreamResult);
            System.out.println("Server Streaming RPC 호출 이후 바로 실행되는 코드 (서버에서 스트림 전송 중에도 출력됨).");

            System.out.println("\n-----------------------------------\n");

            // 💡 3. Client Streaming RPC 테스트
            System.out.println("--- Client Streaming RPC Test ---");
            String clientStreamResult = grpcClient.sendClientStreamLogs();
            System.out.println(clientStreamResult);
            System.out.println("Client Streaming RPC 호출 이후 바로 실행되는 코드 (클라이언트 스트림 전송 중에도 출력됨).");

            System.out.println("\n-----------------------------------\n");


            // 💡 4. Bidirectional Streaming RPC 테스트
            System.out.println("--- Bidirectional Streaming RPC Test ---");
            // 🔑 chatWithServer가 반환한 CountDownLatch를 받습니다.
            CountDownLatch chatLatch = grpcClient.chatWithServer("YoungHoney");

            // 🔑 CommandLineRunner의 메인 스레드가 Latch가 0이 될 때까지 여기서 대기합니다.
            // 이 코드가 비동기 스트림이 완전히 종료될 때까지 애플리케이션 종료를 막아줍니다.
            System.out.println("Bidirectional Streaming RPC 호출 시작. 서버로부터의 스트림이 완료될 때까지 대기합니다...");
            if (!chatLatch.await(10, TimeUnit.SECONDS)) {
                System.out.println("Bidirectional Streaming 테스트 시간 초과!");
            }
            System.out.println("Bidirectional Streaming RPC 테스트 완료.");


            System.out.println("\n== 클라이언트 종료 (모든 통신 테스트 완료) ==");
        };
    }
}
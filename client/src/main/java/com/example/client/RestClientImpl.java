package com.example.client;

import com.example.dto.MessageRequest;
import com.example.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service // Spring 서비스 빈으로 등록
public class RestClientImpl implements Client {

    private static final Logger log = LoggerFactory.getLogger(RestClientImpl.class); // 로깅 사용

    // RestTemplate은 Spring Boot가 자동으로 빈으로 등록해줌 (Web Starter에 포함)
    // 여기서는 별도로 생성자 주입을 통해 받거나, @Autowired를 사용할 수 있습니다.
    // 여기서는 예시를 위해 직접 생성하지만, 실제로는 WebClient 사용이 더 권장됩니다.
    // private final RestTemplate restTemplate; // Spring Bean으로 주입받을 경우

    // public RestClientImpl(RestTemplate restTemplate) {
    //     this.restTemplate = restTemplate;
    // }

    @Override
    public String sendMessage(String content) {
        RestTemplate restTemplate = new RestTemplate(); // 예시를 위해 여기에서 생성 (실제로는 Spring Bean 주입 권장)
        String url = "http://localhost:8080/api/message";

        MessageRequest request = new MessageRequest(content); // DTO 생성자 활용

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MessageRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("[REST Client] 메시지 전송 시도: {}", content);
            ResponseEntity<MessageResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    MessageResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String reply = response.getBody().getReply();
                log.info("[REST Client] 응답 수신: {}", reply);
                return "[REST] 응답: " + reply; // 응답 반환
            } else {
                log.warn("[REST Client] 응답 실패 - 상태 코드: {}", response.getStatusCode());
                return "[REST] 통신 실패: 상태 코드 " + response.getStatusCode();
            }
        } catch (HttpClientErrorException e) {
            // 4xx 클라이언트 에러 (예: 404 Not Found, 400 Bad Request)
            log.error("[REST Client] 클라이언트 에러 발생: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "[REST] 클라이언트 에러: " + e.getStatusCode();
        } catch (ResourceAccessException e) {
            // 네트워크 연결 실패, 서버 응답 없음 등
            log.error("[REST Client] 서버 연결 실패: {}", e.getMessage());
            return "[REST] 서버 연결 실패: " + e.getMessage();
        } catch (Exception e) {
            // 그 외 알 수 없는 에러
            log.error("[REST Client] 예기치 않은 오류 발생: {}", e.getMessage(), e);
            return "[REST] 예기치 않은 오류: " + e.getMessage();
        }
    }
}
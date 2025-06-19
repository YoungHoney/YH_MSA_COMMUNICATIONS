package com.example.client;

import com.example.dto.MessageRequest;
import com.example.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // ⭐️ @Value 임포트
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class RestClientImpl implements Client {

    private static final Logger log = LoggerFactory.getLogger(RestClientImpl.class);


    private final RestTemplate restTemplate;
    private final String restApiUrl;


    public RestClientImpl(
            @Value("${Rest.server.host}") String restHost,
            @Value("${Rest.server.port}") int restPort
    ) {

        this.restTemplate = new RestTemplate();


        this.restApiUrl = String.format("http://%s:%d/api/message", restHost, restPort);
        log.info("REST API 서버 최종 URL 설정 완료: {}", this.restApiUrl);
    }

    @Override
    public String sendMessage(String content) {


        MessageRequest request = new MessageRequest(content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MessageRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("[REST Client] 메시지 전송 시도: {} -> {}", content, restApiUrl);
            ResponseEntity<MessageResponse> response = restTemplate.exchange(
                    restApiUrl, // ⭐️ 하드코딩된 url 대신 필드 사용
                    HttpMethod.POST,
                    entity,
                    MessageResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String reply = response.getBody().getReply();
                log.info("[REST Client] 응답 수신: {}", reply);
                return "[REST] 응답: " + reply;
            } else {
                log.warn("[REST Client] 응답 실패 - 상태 코드: {}", response.getStatusCode());
                return "[REST] 통신 실패: 상태 코드 " + response.getStatusCode();
            }
            // ... (catch 블록은 기존과 동일) ...
        } catch (HttpClientErrorException e) {
            log.error("[REST Client] 클라이언트 에러 발생: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "[REST] 클라이언트 에러: " + e.getStatusCode();
        } catch (ResourceAccessException e) {
            log.error("[REST Client] 서버 연결 실패: {}", e.getMessage());
            return "[REST] 서버 연결 실패: " + e.getMessage();
        } catch (Exception e) {
            log.error("[REST Client] 예기치 않은 오류 발생: {}", e.getMessage(), e);
            return "[REST] 예기치 않은 오류: " + e.getMessage();
        }
    }
}
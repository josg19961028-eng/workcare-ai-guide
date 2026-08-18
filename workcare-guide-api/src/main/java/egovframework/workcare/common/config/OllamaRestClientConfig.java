package egovframework.workcare.common.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 로컬 Ollama API 호출에 사용할 HTTP 클라이언트를 설정한다.
 *
 * <p>공공데이터 API와 Ollama는 주소와 응답 시간이 다르므로
 * 서로 다른 RestClient Bean으로 분리한다.</p>
 */
@Configuration(proxyBeanMethods = false)
public class OllamaRestClientConfig {

    /**
     * Ollama 전용 RestClient를 생성한다.
     *
     * @param builderConfigurer Spring Boot의 RestClient 기본 설정
     * @param properties        Ollama 환경설정
     * @return Ollama 전용 RestClient
     */
    @Bean("ollamaRestClient")
    public RestClient ollamaRestClient(
            RestClientBuilderConfigurer builderConfigurer,
            OllamaProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        /*
         * 로컬 Ollama 서버에 연결할 수 없는 경우
         * 3초 안에 연결 실패로 처리한다.
         */
        requestFactory.setConnectTimeout(
                Duration.ofSeconds(3)
        );

        /*
         * 모델을 처음 호출할 때 메모리에 적재하는 시간이 필요할 수 있다.
         * Intel Mac 로컬 실습환경을 고려해 최대 60초까지 기다린다.
         */
        requestFactory.setReadTimeout(
                Duration.ofSeconds(60)
        );

        RestClient.Builder builder =
                builderConfigurer.configure(
                        RestClient.builder()
                );

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
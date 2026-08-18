package egovframework.workcare.common.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 기관의 API를 호출하는 HTTP 클라이언트를 설정한다.
 *
 * <p>
 * 각 공공데이터 서비스는 공통 주소가 다르므로
 * API 종류별 RestClient Bean을 구분해서 생성한다.
 * </p>
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    /**
     * 사회복귀 지원기관 API 전용 RestClient를 생성한다.
     *
     * @param builderConfigurer Spring Boot의 RestClient 기본 설정
     * @param properties        공공데이터 환경설정
     * @return 사회복귀 지원기관 API 전용 RestClient
     */
    @Bean
    @Qualifier("rehabilitationRestClient")
    public RestClient rehabilitationRestClient(
            RestClientBuilderConfigurer builderConfigurer,
            PublicDataProperties properties
    ) {
        return createRestClient(
                builderConfigurer,
                properties.rehabilitationBaseUrl()
        );
    }

    /**
     * 산재 의료기관 API 전용 RestClient를 생성한다.
     *
     * <p>
     * 앞으로 다음  가지 상세기능에서 이 클라이언트를 재사용한다.
     * </p>
     *
     * <ul>
     *   <li>산재지정 의료기관</li>
     *   <li>산재지정 약국</li>
     *   <li>재활인증 의료기관</li>
     * </ul>
     *
     * @param builderConfigurer Spring Boot의 RestClient 기본 설정
     * @param properties        공공데이터 환경설정
     * @return 산재 의료기관 API 전용 RestClient
     */
    @Bean
    @Qualifier("medicalInstitutionRestClient")
    public RestClient medicalInstitutionRestClient(
            RestClientBuilderConfigurer builderConfigurer,
            PublicDataProperties properties
    ) {
        return createRestClient(
                builderConfigurer,
                properties.medicalBaseUrl()
        );
    }

    /**
     * 산재보험 판례 판결문 API 전용 RestClient를 생성한다.
     *
     * <p>
     * 판례 API는 의료기관 API와 공통 주소가 다르므로
     * 별도의 RestClient Bean으로 분리한다.
     * </p>
     *
     * <p>
     * 여러 RestClient Bean이 동시에 존재하기 때문에
     * precedentRestClient라는 Qualifier로 구분한다.
     * 나중에 판례 Client 클래스에서 같은 이름을 지정해 주입받는다.
     * </p>
     *
     * @param builderConfigurer Spring Boot의 RestClient 기본 설정
     * @param properties        공공데이터 환경설정
     * @return 산재보험 판례 API 전용 RestClient
     */
    @Bean
    @Qualifier("precedentRestClient")
    public RestClient precedentRestClient(
            RestClientBuilderConfigurer builderConfigurer,
            PublicDataProperties properties
    ) {
        /*
         * 기존 공통 생성 메서드를 재사용한다.
         *
         * 따라서 판례 API에도 연결 제한 5초와
         * 응답 제한 10초가 동일하게 적용된다.
         */
        return createRestClient(
                builderConfigurer,
                properties.precedentBaseUrl()
        );
    }

    /**
     * 공통 통신 설정을 적용해 RestClient를 생성한다.
     *
     * <p>
     * 두 RestClient에서 연결시간과 응답시간 설정을 중복 작성하지
     * 않도록 공통 메서드로 분리했다.
     * </p>
     *
     * @param builderConfigurer Spring Boot의 RestClient 기본 설정
     * @param baseUrl           외부 API의 공통 주소
     * @return 공통 통신 정책이 적용된 RestClient
     */
    private RestClient createRestClient(
            RestClientBuilderConfigurer builderConfigurer,
            String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        /*
         * 외부 서버와 연결을 맺는 시간을 최대 5초로 제한한다.
         *
         * 외부 서버 장애 시 연결이 무한정 대기하면
         * 우리 서버의 요청 처리 자원이 고갈될 수 있다.
         */
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));

        /*
         * 연결 이후 응답을 읽는 시간을 최대 10초로 제한한다.
         *
         * 이는 외부 API 장애 전파와 자원 고갈 위험을 줄이기 위한
         * 안정성·보안 설정이다.
         */
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestClient.Builder builder = builderConfigurer.configure(
                RestClient.builder()
        );

        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
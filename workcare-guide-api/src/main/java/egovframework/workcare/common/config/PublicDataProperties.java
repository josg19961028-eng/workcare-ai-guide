package egovframework.workcare.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 공공데이터포털 연동에 필요한 설정을 관리한다.
 *
 * <p>
 * application-local.yml에 작성한 설정값을
 * Java 객체로 안전하게 전달받는 역할을 한다.
 * </p>
 *
 * @param serviceKey            공공데이터포털 인증키
 * @param rehabilitationBaseUrl 사회복귀 지원기관 API 공통 주소
 * @param medicalBaseUrl        산재 의료기관 API 공통 주소
 * @param precedentBaseUrl     산재보험 판례 판결문 API 공통 주소
 */
@Validated
@ConfigurationProperties(prefix = "workcare.public-data")
public record PublicDataProperties(

        /*
         * 인증키가 없으면 외부 API를 사용할 수 없다.
         *
         * @NotBlank를 적용하면 인증키 누락 상태로 서버가 실행되는 것을
         * 시작 단계에서 차단할 수 있다.
         */
        @NotBlank(
                message = "공공데이터포털 서비스 인증키가 설정되지 않았습니다."
        )
        String serviceKey,

        /*
         * 직업훈련기관·재활스포츠기관·심리재활기관 등의
         * 사회복귀 지원기관 정보를 조회하는 API 주소다.
         */
        @NotBlank(
                message = "산재재활기관 API 주소가 설정되지 않았습니다."
        )
        String rehabilitationBaseUrl,

        /*
         * 산재지정 의료기관, 지정 약국, 재활인증 의료기관을 조회하는 API 주소다.
         */
        @NotBlank(
                message = "산재 의료기관 API 주소가 설정되지 않았습니다."
        )
        String medicalBaseUrl,

        /*
         * 산재보험 판례의 사건번호, 판결 결과, 사건유형,
         * 사고·질병 구분과 판결문 전문을 조회하는 API 주소다.
         *
         * URL을 Java 코드에 직접 작성하지 않고 설정으로 분리하면
         * 테스트 및 운영환경에서 주소를 안전하게 변경할 수 있다.
         */
        @NotBlank(
                message = "산재보험 판례 API 주소가 설정되지 않았습니다."
        )
        String precedentBaseUrl
) {
}
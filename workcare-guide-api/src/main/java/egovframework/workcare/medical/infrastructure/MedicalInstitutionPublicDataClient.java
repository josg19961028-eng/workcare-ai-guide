package egovframework.workcare.medical.infrastructure;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;

import egovframework.workcare.common.config.PublicDataProperties;
import egovframework.workcare.common.xml.ExternalXmlParser;
import egovframework.workcare.medical.infrastructure.dto.MedicalInstitutionApiResponse;

/**
 * 근로복지공단 산재 의료기관 공공데이터 API를 호출한다.
 *
 * <p>
 * 이 클래스의 책임은 다음 두 가지다.
 * </p>
 *
 * <ol>
 *   <li>근로복지공단 API에 HTTP 요청을 전송한다.</li>
 *   <li>반환된 XML을 MedicalInstitutionApiResponse로 변환한다.</li>
 * </ol>
 *
 * <p>
 * 페이지 값 검증, 결과 코드 판단, 화면용 데이터 변환은
 * 이 클래스에서 처리하지 않고 Service 계층에서 처리한다.
 * </p>
 */
@Component
public class MedicalInstitutionPublicDataClient {

    /**
     * 일반 산재지정 의료기관 조회 경로다.
     */
    private static final String MEDICAL_INSTITUTION_LIST_PATH =
            "/getSjJijeongHptChakgiList";

    /*
     * 산재지정 약국 조회 경로다.
     */
    private static final String PHARMACY_LIST_PATH =
            "/getSjJijeongyakgukChakgiList";

    /*
     * 재활인증 의료기관 조회 경로다.
     */
    private static final String CERTIFIED_REHABILITATION_LIST_PATH =
            "/getSjBoheomJhCftHptPstateList";


    /**
     * 산재 의료기관 API 전용 HTTP 클라이언트다.
     */
    private final RestClient restClient;

    /**
     * 공공데이터포털 인증키가 포함된 설정 객체다.
     */
    private final PublicDataProperties properties;

    /**
     * 외부 XML을 안전하게 Java 객체로 변환하는 공통 Parser다.
     *
     * <p>
     * DTD와 외부 엔티티 처리를 차단하여
     * XXE 공격 가능성을 줄이는 설정이 적용되어 있다.
     * </p>
     */
    private final ExternalXmlParser xmlParser;

    /**
     * 필요한 의존성을 생성자 주입으로 전달받는다.
     *
     * @param restClient 산재 의료기관 API 전용 RestClient
     * @param properties 공공데이터 API 설정
     * @param xmlParser  보안 설정이 적용된 XML Parser
     */
    public MedicalInstitutionPublicDataClient(
            @Qualifier("medicalInstitutionRestClient")
            RestClient restClient,
            PublicDataProperties properties,
            ExternalXmlParser xmlParser
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.xmlParser = xmlParser;
    }

    /**
     * 산재지정 의료기관 목록을 조회한다.
     *
     * @param pageNo    조회할 페이지 번호
     * @param numOfRows 한 페이지에서 조회할 의료기관 수
     * @return 산재지정 의료기관 XML 응답
     */
    public MedicalInstitutionApiResponse fetchMedicalInstitutions(
            int pageNo,
            int numOfRows
    ) {
        /*
         * 의료기관 전용 경로를 공통 조회 메서드에 전달한다.
         */
        return fetchList(
                MEDICAL_INSTITUTION_LIST_PATH,
                pageNo,
                numOfRows,
                "산재지정 의료기관"
        );
    }

    /**
     * 산재지정 약국 목록을 조회한다.
     *
     * @param pageNo    조회할 페이지 번호
     * @param numOfRows 한 페이지에서 조회할 약국 수
     * @return 산재지정 약국 XML 응답
     */
    public MedicalInstitutionApiResponse fetchPharmacies(
            int pageNo,
            int numOfRows
    ) {
        /*
         * 약국 전용 경로를 공통 조회 메서드에 전달한다.
         */
        return fetchList(
                PHARMACY_LIST_PATH,
                pageNo,
                numOfRows,
                "산재지정 약국"
        );
    }

    /**
     * 산재보험 재활인증 의료기관 목록을 조회한다.
     *
     * <p>
     * 일반 의료기관 및 약국 API와 XML 계층이 동일하므로
     * 기존 공통 호출 및 XML 변환 메서드를 재사용한다.
     * </p>
     *
     * @param pageNo    조회할 페이지 번호
     * @param numOfRows 한 페이지에서 조회할 기관 수
     * @return 재활인증 의료기관 XML 응답
     */
    public MedicalInstitutionApiResponse
            fetchCertifiedRehabilitationInstitutions(
                    int pageNo,
                    int numOfRows
            ) {

        /*
         * 재활인증 의료기관 상세기능 경로를
         * 공통 API 호출 메서드에 전달한다.
         */
        return fetchList(
                CERTIFIED_REHABILITATION_LIST_PATH,
                pageNo,
                numOfRows,
                "재활인증 의료기관"
        );
    }

    /**
     * 의료기관과 약국 API의 공통 호출 및 XML 변환을 처리한다.
     *
     * <p>
     * 두 API는 상세기능 경로만 다르고 다음 구조는 동일하다.
     * </p>
     *
     * <ul>
     *   <li>ServiceKey 요청 방식</li>
     *   <li>pageNo와 numOfRows</li>
     *   <li>header, body, items, item XML 계층</li>
     *   <li>기관명, 주소, 연락처 및 관할 지사 필드</li>
     * </ul>
     *
     * <p>
     * 공통 코드를 한곳에서 처리하면 인증키 전송 방식이나
     * XML 보안 설정을 변경할 때 한 메서드만 수정하면 된다.
     * </p>
     *
     * @param requestPath 상세기능 요청 경로
     * @param pageNo      조회할 페이지 번호
     * @param numOfRows   한 페이지 결과 수
     * @param dataName    안전한 로그·예외 메시지용 기능명
     * @return XML에서 변환한 공공데이터 응답
     */
    private MedicalInstitutionApiResponse fetchList(
            String requestPath,
            int pageNo,
            int numOfRows,
            String dataName
    ) {
        /*
         * 인증키와 페이지 값을 URI 템플릿 변수로 관리한다.
         *
         * 문자열을 직접 연결하는 방식보다 특수문자를 안전하게
         * 인코딩할 수 있으며 URL 생성 오류도 줄일 수 있다.
         */
        Map<String, Object> uriVariables = Map.of(
                "serviceKey", properties.serviceKey(),
                "pageNo", pageNo,
                "numOfRows", numOfRows
        );

        /*
         * 전달받은 상세기능 경로로 공공데이터 API를 호출한다.
         *
         * requestPath는 사용자 입력이 아니라
         * 클래스 내부의 상수만 전달하므로 임의의 외부 주소를
         * 호출하는 SSRF 위험을 만들지 않는다.
         */
        String responseXml = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(requestPath)
                        .queryParam(
                                "ServiceKey",
                                "{serviceKey}"
                        )
                        .queryParam(
                                "pageNo",
                                "{pageNo}"
                        )
                        .queryParam(
                                "numOfRows",
                                "{numOfRows}"
                        )
                        .build(uriVariables)
                )
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .body(String.class);

        /*
         * 빈 XML은 Java 객체로 변환할 수 없으므로
         * 파싱 전에 방어적으로 검사한다.
         */
        if (responseXml == null || responseXml.isBlank()) {
            throw new IllegalStateException(
                    dataName + " API가 빈 응답을 반환했습니다."
            );
        }

        try {
            /*
             * 의료기관과 약국의 XML 구조가 동일하므로
             * 기존 응답 DTO를 공통으로 사용한다.
             *
             * 약국 응답에 없는 jhHospital과 jpHospital은
             * null로 매핑되므로 XML 변환에는 문제가 없다.
             */
            return xmlParser.readValue(
                    responseXml,
                    MedicalInstitutionApiResponse.class
            );

        } catch (JsonProcessingException exception) {
            /*
             * XML 원문이나 인증키를 오류 메시지에 포함하지 않는다.
             */
            throw new IllegalStateException(
                    dataName + " API 응답 형식을 해석할 수 없습니다.",
                    exception
            );
        }
    }
}
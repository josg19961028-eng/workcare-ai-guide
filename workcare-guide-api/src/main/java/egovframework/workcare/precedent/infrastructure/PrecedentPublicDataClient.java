package egovframework.workcare.precedent.infrastructure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

import egovframework.workcare.common.config.PublicDataProperties;
import egovframework.workcare.common.xml.ExternalXmlParser;
import egovframework.workcare.precedent.infrastructure.dto.PrecedentApiResponse;

/**
 * 근로복지공단 산재보험 판례 공공데이터 API를 호출한다.
 *
 * <p>
 * 이 클래스는 HTTP 통신과 XML 변환만 담당한다.
 * 검색조건 검증과 화면용 필드 변환은 Service 계층에서 처리한다.
 * </p>
 */
@Component
public class PrecedentPublicDataClient {

    private static final String ACCIDENT_DISEASE_TYPE_PATH =
            "/getSjbSagoJilbyeongGubunPstate";

    private static final String RESULT_TYPE_PATH =
            "/getSjbPrecedentResultYuhyeongPstate";

    private static final String CASE_TYPE_PATH =
            "/getSjbSageonYuhyeongPstate";

    private static final String COUNT_PATH =
            "/getSjbYuhyeongByCountPstate";

    private static final String PRECEDENT_LIST_PATH =
            "/getSjbPrecedentNaeyongPstate";

    /*
     * 분류값은 총 100건보다 적으므로 한 번의 호출로 모두 가져온다.
     * 현재 확인된 개수는 결과 7종, 사건유형 12종, 사고·질병 13종이다.
     */
    private static final int CATEGORY_PAGE_SIZE = 100;

    private final RestClient restClient;
    private final PublicDataProperties properties;
    private final ExternalXmlParser xmlParser;

    /**
     * 판례 API 전용 RestClient를 생성자 주입으로 전달받는다.
     */
    public PrecedentPublicDataClient(
            @Qualifier("precedentRestClient")
            RestClient restClient,
            PublicDataProperties properties,
            ExternalXmlParser xmlParser
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.xmlParser = xmlParser;
    }

    /**
     * 사고·질병 구분 목록을 조회한다.
     */
    public PrecedentApiResponse fetchAccidentDiseaseTypes() {
        return fetch(
                ACCIDENT_DISEASE_TYPE_PATH,
                1,
                CATEGORY_PAGE_SIZE,
                null,
                null,
                null,
                "사고·질병 구분"
        );
    }

    /**
     * 판결결과 유형 목록을 조회한다.
     */
    public PrecedentApiResponse fetchResultTypes() {
        return fetch(
                RESULT_TYPE_PATH,
                1,
                CATEGORY_PAGE_SIZE,
                null,
                null,
                null,
                "판결결과 유형"
        );
    }

    /**
     * 사건유형 목록을 조회한다.
     */
    public PrecedentApiResponse fetchCaseTypes() {
        return fetch(
                CASE_TYPE_PATH,
                1,
                CATEGORY_PAGE_SIZE,
                null,
                null,
                null,
                "사건유형"
        );
    }

    /**
     * 검색조건에 해당하는 판결문 목록을 조회한다.
     */
    public PrecedentApiResponse fetchPrecedents(
            int pageNo,
            int numOfRows,
            String resultType,
            String caseType,
            String accidentDiseaseType
    ) {
        return fetch(
                PRECEDENT_LIST_PATH,
                pageNo,
                numOfRows,
                resultType,
                caseType,
                accidentDiseaseType,
                "산재보험 판례"
        );
    }

    /**
     * 검색조건에 해당하는 판례 개수를 조회한다.
     */
    public PrecedentApiResponse fetchPrecedentCount(
            String resultType,
            String caseType,
            String accidentDiseaseType
    ) {
        return fetch(
                COUNT_PATH,
                1,
                1,
                resultType,
                caseType,
                accidentDiseaseType,
                "판례 검색 결과 개수"
        );
    }

    /**
     * 판례 API의 공통 HTTP 호출과 XML 변환을 처리한다.
     */
    private PrecedentApiResponse fetch(
            String requestPath,
            int pageNo,
            int numOfRows,
            String resultType,
            String caseType,
            String accidentDiseaseType,
            String dataName
    ) {
        /*
         * URI 변수는 문자열 연결 대신 Map으로 관리한다.
         * 인증키와 한글 검색조건을 URI Builder가 안전하게 인코딩한다.
         */
        Map<String, Object> uriVariables = new HashMap<>();

        uriVariables.put("serviceKey", properties.serviceKey());
        uriVariables.put("pageNo", pageNo);
        uriVariables.put("numOfRows", numOfRows);

        String responseXml = restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder requestUri = uriBuilder
                            /*
                             * requestPath는 사용자 입력이 아니라
                             * 클래스 내부 상수만 전달하므로 SSRF 위험을 줄인다.
                             */
                            .path(requestPath)
                            .queryParam("ServiceKey", "{serviceKey}")
                            .queryParam("pageNo", "{pageNo}")
                            .queryParam("numOfRows", "{numOfRows}");

                    /*
                     * 값이 있는 조건만 외부 API 요청에 포함한다.
                     * 공백 조건을 전달하면 외부 API가 잘못 해석할 수 있다.
                     */
                    if (resultType != null && !resultType.isBlank()) {
                        uriVariables.put("kindA", resultType);
                        requestUri.queryParam("kindA", "{kindA}");
                    }

                    if (caseType != null && !caseType.isBlank()) {
                        uriVariables.put("kindB", caseType);
                        requestUri.queryParam("kindB", "{kindB}");
                    }

                    if (accidentDiseaseType != null
                            && !accidentDiseaseType.isBlank()) {

                        uriVariables.put(
                                "kindC",
                                accidentDiseaseType
                        );
                        requestUri.queryParam("kindC", "{kindC}");
                    }

                    return requestUri.build(uriVariables);
                })
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .body(String.class);

        /*
         * 빈 응답은 XML로 변환할 수 없으므로 파싱 전에 차단한다.
         */
        if (responseXml == null || responseXml.isBlank()) {
            throw new IllegalStateException(
                    dataName + " API가 빈 응답을 반환했습니다."
            );
        }

        try {
            /*
             * XXE 방어 설정이 적용된 공통 XML Parser를 사용한다.
             */
            return xmlParser.readValue(
                    responseXml,
                    PrecedentApiResponse.class
            );

        } catch (JsonProcessingException exception) {
            /*
             * 판결문 전문과 인증키가 로그나 응답에 노출되지 않도록
             * XML 원문을 예외 메시지에 포함하지 않는다.
             */
            throw new IllegalStateException(
                    dataName + " API 응답 형식을 해석할 수 없습니다.",
                    exception
            );
        }
    }
}
package egovframework.workcare.precedent.infrastructure.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 근로복지공단 산재보험 판례 API의 XML 응답 구조를 표현한다.
 *
 * <p>
 * 판결문, 판결결과 유형, 사건유형, 사고·질병 구분 및
 * 조건별 판례 개수 API가 공통으로 사용하는 외부 연동 DTO다.
 * </p>
 *
 * <p>
 * 이 DTO는 외부 기관의 필드명을 그대로 표현하므로
 * Vue 화면에 직접 반환하지 않는다. Service에서 우리 서비스가
 * 사용하는 의미 있는 이름으로 변환한다.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public record PrecedentApiResponse(

        // 외부 API의 처리 결과 코드와 메시지다.
        @JacksonXmlProperty(localName = "header")
        Header header,

        // 목록 및 페이지 정보가 들어 있는 영역이다.
        @JacksonXmlProperty(localName = "body")
        Body body
) {

    /**
     * 공공데이터 API 처리 결과다.
     *
     * HTTP 상태가 200이어도 resultCode에는 업무 오류가 들어올 수 있으므로
     * Service 계층에서 resultCode가 00인지 별도로 검사한다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    /**
     * 판례 데이터와 페이지 정보를 나타낸다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(

            @JacksonXmlProperty(localName = "items")
            Items items,

            int numOfRows,
            int pageNo,
            int totalCount
    ) {
    }

    /**
     * XML의 items 내부에서 반복되는 item을 목록으로 변환한다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(

            // items 안에서 item 태그가 반복되므로 별도 Wrapper를 만들지 않는다.
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "item")
            List<Item> item
    ) {
    }

    /**
     * 판례 API가 반환하는 원본 데이터 한 건이다.
     *
     * <p>
     * 호출한 상세기능에 따라 일부 필드만 값이 들어온다.
     * 예를 들어 판결결과 유형 API는 kinda만 반환하고,
     * 조건별 개수 API는 cnt만 반환한다.
     * </p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(

            // 사건번호
            String accnum,

            // 판결 법원명
            String courtname,

            // 판결결과: 기각, 취소, 각하 등
            String kinda,

            // 사건유형: 장해, 요양, 유족 등
            String kindb,

            // 사고·질병 구분: 업무상질병, 업무상사고 등
            String kindc,

            // 판결문 전문
            String noncontent,

            // 사건명
            String title,

            /*
             * 조건에 해당하는 판례 개수다.
             *
             * 다른 API 응답에는 cnt가 없으므로 기본형 int가 아닌
             * null을 표현할 수 있는 Integer로 선언한다.
             */
            Integer cnt
    ) {
    }
}
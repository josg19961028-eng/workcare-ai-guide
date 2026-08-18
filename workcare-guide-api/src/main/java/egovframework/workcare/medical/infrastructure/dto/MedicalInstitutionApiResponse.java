package egovframework.workcare.medical.infrastructure.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 근로복지공단 산재지정 의료기관 API의 XML 응답 구조를 표현한다.
 *
 * <p>
 * 이 객체는 근로복지공단이 반환하는 XML을 해석하기 위한
 * 외부 연동 전용 DTO다.
 * </p>
 *
 * <p>
 * Vue 화면으로 이 객체를 직접 반환하지 않는다.
 * Service 계층에서 우리 서비스가 사용할 이름으로 변환할 예정이다.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public record MedicalInstitutionApiResponse(

        /*
         * 외부 API의 처리 결과 코드와 메시지가 들어 있는 영역이다.
         */
        @JacksonXmlProperty(localName = "header")
        Header header,

        /*
         * 의료기관 목록과 페이지 정보가 들어 있는 영역이다.
         */
        @JacksonXmlProperty(localName = "body")
        Body body
) {

    /**
     * 외부 API의 처리 결과를 나타낸다.
     *
     * <p>
     * 공공데이터 API는 HTTP 상태가 200이어도
     * resultCode에 오류 코드를 담아 반환할 수 있다.
     * 따라서 Service에서 resultCode를 별도로 검사해야 한다.
     * </p>
     *
     * @param resultCode 외부 API 처리 결과 코드
     * @param resultMsg  외부 API 처리 결과 메시지
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    /**
     * 의료기관 목록과 페이지 정보를 나타낸다.
     *
     * @param items      의료기관 목록 영역
     * @param numOfRows  한 페이지 결과 수
     * @param pageNo     현재 페이지 번호
     * @param totalCount 전체 의료기관 수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(

            /*
             * 실제 XML의 <items> 영역을 Items 객체에 연결한다.
             */
            @JacksonXmlProperty(localName = "items")
            Items items,

            int numOfRows,
            int pageNo,
            int totalCount
    ) {
    }

    /**
     * XML의 items 영역을 나타낸다.
     *
     * <p>
     * 실제 XML 구조는 다음과 같이 items 안에서 item이 반복된다.
     * </p>
     *
     * <pre>
     * &lt;items&gt;
     *     &lt;item&gt;...&lt;/item&gt;
     *     &lt;item&gt;...&lt;/item&gt;
     * &lt;/items&gt;
     * </pre>
     *
     * @param item 반복되는 의료기관 목록
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(

            /*
             * 현재 객체 자체가 이미 <items> 영역을 나타내므로
             * 별도의 목록 Wrapper를 추가하지 않는다.
             *
             * 반복되는 각 <item>을 List 요소로 변환한다.
             */
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "item")
            List<InstitutionItem> item
    ) {
    }

    /**
     * 산재 의료정보 API가 반환하는 기관 한 건의 원본 정보다.
     *
     * <p>
     * 산재지정 의료기관, 지정 약국, 재활인증 의료기관이
     * 공통으로 사용하는 XML DTO다.
     * </p>
     *
     * <p>
     * 상세기능에 따라 일부 태그가 제공되지 않을 수 있으며,
     * 제공되지 않은 String 필드는 null로 매핑된다.
     * </p>
     *
     * @param addr         기관 주소
     * @param faxTel       팩스번호
     * @param gtCdNm1      공공데이터가 제공하는 의료기관 종별명
     * @param gwanriJisaCd 관리 지사 코드
     * @param hospitalNm   의료기관 또는 약국 이름
     * @param hospitalNo   의료기관 식별번호
     * @param jhHospital   외부 API가 제공하는 병원 구분값
     * @param jisaNm       관리 지사명
     * @param jpHospital   외부 API가 제공하는 병원 구분값
     * @param tel          전화번호
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstitutionItem(
            String addr,
            String faxTel,
            String gtCdNm1,
            String gwanriJisaCd,
            String hospitalNm,
            String hospitalNo,
            String jhHospital,
            String jisaNm,
            String jpHospital,
            String tel
    ) {
    }
}
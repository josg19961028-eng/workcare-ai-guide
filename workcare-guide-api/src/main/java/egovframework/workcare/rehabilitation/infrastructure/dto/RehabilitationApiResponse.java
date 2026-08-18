package egovframework.workcare.rehabilitation.infrastructure.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 근로복지공단 산재재활기관 API의 XML 응답 구조다.
 *
 * <p>이 DTO는 외부 API 형식에 맞춘 객체이며,
 * Vue에 직접 반환할 최종 응답 객체와는 분리할 예정이다.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public record RehabilitationApiResponse(

        @JacksonXmlProperty(localName = "header")
        Header header,

        @JacksonXmlProperty(localName = "body")
        Body body
) {

    /**
     * 공공데이터 API 처리 결과를 나타낸다.
     *
     * @param resultCode 성공 또는 오류 코드
     * @param resultMsg  처리 결과 메시지
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    /**
     * 재활기관 목록과 페이지 정보를 나타낸다.
     *
     * @param items      XML의 items 영역
     * @param numOfRows  한 페이지 결과 수
     * @param pageNo     현재 페이지 번호
     * @param totalCount 전체 데이터 수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(

            /*
             * XML의 <items> 영역 전체를 Items 객체로 받는다.
             *
             * <items>와 반복되는 <item>은 서로 다른 계층이므로
             * 하나의 List 필드에 동시에 매핑하지 않고 중간 객체를 둔다.
             */
            @JacksonXmlProperty(localName = "items")
            Items items,

            int numOfRows,
            int pageNo,
            int totalCount
    ) {
    }

    /**
     * XML의 items 영역을 나타내는 중간 DTO다.
     *
     * <p>실제 XML 구조는 다음과 같다.</p>
     *
     * <pre>
     * &lt;items&gt;
     *     &lt;item&gt;...&lt;/item&gt;
     *     &lt;item&gt;...&lt;/item&gt;
     * &lt;/items&gt;
     * </pre>
     *
     * @param item 반복되는 재활기관 데이터 목록
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(

            /*
             * 현재 Items 객체가 이미 <items> 영역을 나타내므로
             * 추가 목록 Wrapper는 사용하지 않는다.
             *
             * <item> 태그가 반복될 때마다 InstitutionItem 객체가
             * List에 하나씩 추가된다.
             */
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "item")
            List<InstitutionItem> item
    ) {
    }

    /**
     * XML의 item 한 건을 나타낸다.
     *
     * <p>전화번호와 코드는 계산 대상이 아니고 앞자리 0이
     * 유지되어야 하므로 숫자가 아닌 String으로 선언한다.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstitutionItem(
            String addr,
            String faxNo,
            String gigwanFg,
            String gigwanFgNm,
            String gigwanNm,
            String gwanriJisaCd,
            String jisaNm,
            String telNo,
            String urlAddr
    ) {
    }
}
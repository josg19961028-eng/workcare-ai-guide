package egovframework.workcare.rehabilitation.infrastructure;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import egovframework.workcare.common.xml.ExternalXmlParser;

import egovframework.workcare.common.config.PublicDataProperties;
import egovframework.workcare.rehabilitation.infrastructure.dto.RehabilitationApiResponse;

/**
 * 근로복지공단 산재재활기관 공공데이터 API를 호출하는 클래스다.
 *
 * <p>
 * 이 클래스는 외부 API와 통신하는 책임만 담당한다. 검색 조건 검증이나 화면용 응답 변환 같은 업무 로직은 이후 Service 계층에서
 * 처리한다.
 * </p>
 */
@Component
public class RehabilitationPublicDataClient {

	/*
	 * 공공데이터 XML을 Java 객체로 변환하는 보안 설정 적용 XmlMapper다.
	 */
	private final ExternalXmlParser xmlParser;

	/*
	 * 재활기관 목록을 조회하는 공공데이터 API 기능 경로다.
	 *
	 * 외부 API 명세가 변경되면 이 상수만 확인하면 되도록 문자열을 한곳에서 관리한다.
	 */
	private static final String INSTITUTION_LIST_PATH = "/getSjbWkGigwanInfoList";

	/*
	 * 근로복지공단 산재재활기관 API 전용 HTTP 클라이언트다.
	 */
	private final RestClient restClient;

	/*
	 * 공공데이터포털 인증키를 포함한 외부 API 설정이다.
	 */
	private final PublicDataProperties properties;

	/**
	 * 필요한 객체를 생성자 주입으로 전달받는다.
	 *
	 * <p>
	 * @Qualifier를 사용하지 않으면 앞으로 판례용 RestClient가 추가됐을 때 Spring이 어떤 RestClient를 주입해야
	 * 하는지 결정하지 못할 수 있다.
	 * </p>
	 *
	 * @param restClient 산재재활기관 API 전용 HTTP 클라이언트
	 * @param properties 공공데이터 API 설정
	 */
	public RehabilitationPublicDataClient(
	        @Qualifier("rehabilitationRestClient")
	        RestClient restClient,
	        PublicDataProperties properties,
	        ExternalXmlParser xmlParser) {

	    this.restClient = restClient;
	    this.properties = properties;
	    this.xmlParser = xmlParser;
	}

	/**
	 * 산재재활기관 목록의 XML 원문을 조회한다.
	 *
	 * @param pageNo    조회할 페이지 번호
	 * @param numOfRows 한 페이지에 요청할 데이터 수
	 * @return 공공데이터 API가 반환한 XML 문자열
	 */
	/**
	 * 산재재활기관 목록을 조회하고 XML을 Java 객체로 변환한다.
	 *
	 * @param pageNo    조회할 페이지 번호
	 * @param numOfRows 한 페이지 결과 수
	 * @return 공공데이터 API 응답 객체
	 */
	public RehabilitationApiResponse fetchInstitutionList(int pageNo, int numOfRows) {

		Map<String, Object> uriVariables = Map.of("serviceKey", properties.serviceKey(), "pageNo", pageNo, "numOfRows",
				numOfRows);

		/*
		 * 우선 외부 API의 XML 원문을 문자열로 받는다.
		 */
		String responseXml = restClient.get()
				.uri(uriBuilder -> uriBuilder.path(INSTITUTION_LIST_PATH).queryParam("ServiceKey", "{serviceKey}")
						.queryParam("pageNo", "{pageNo}").queryParam("numOfRows", "{numOfRows}").build(uriVariables))
				.accept(MediaType.APPLICATION_XML).retrieve().body(String.class);

		/*
		 * 응답이 비어 있으면 정상적인 XML 파싱을 진행할 수 없다.
		 */
		if (responseXml == null || responseXml.isBlank()) {
			throw new IllegalStateException("근로복지공단 API가 빈 응답을 반환했습니다.");
		}

		try {
			/*
			 * XML 태그를 RehabilitationApiResponse 구조에 맞춰 변환한다.
			 */
			return xmlParser.readValue(
			        responseXml,
			        RehabilitationApiResponse.class);
		} catch (JsonProcessingException exception) {
			/*
			 * XML 구조가 예상과 다르면 변환 실패로 처리한다.
			 *
			 * XML 원문은 개인정보나 외부 데이터가 포함될 수 있으므로 예외 메시지에 그대로 넣지 않는다.
			 */
			throw new IllegalStateException("근로복지공단 API 응답 형식을 해석할 수 없습니다.", exception);
		}
	}
}
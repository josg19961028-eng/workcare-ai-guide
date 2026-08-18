package egovframework.workcare.common.xml;

import javax.xml.stream.XMLInputFactory;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * 외부 기관에서 전달받은 XML 문자열을 Java 객체로 변환하는 전용 Parser다.
 */
@Component
public class ExternalXmlParser {
	private final XmlMapper xmlMapper;

	public ExternalXmlParser() {

		XMLInputFactory inputFactory = XMLInputFactory.newFactory();

		/*
		 * XML 내부의 DTD 선언을 허용하지 않는다.
		 *
		 * 악성 XML이 서버 파일이나 내부 자원을 참조하는 것을 차단하기 위한 보안 설정이다.
		 */
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

		/*
		 * 외부 엔티티 참조를 비활성화한다.
		 *
		 * 서버 내부 파일 조회나 내부망 요청으로 이어질 수 있는 XXE 공격을 방지한다.
		 */
		inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

		/*
		 * 위에서 보안 설정한 XMLInputFactory로 XmlMapper를 생성한다.
		 *
		 * 이 객체는 Spring Bean이 아니라 이 Parser 내부 객체이므로 일반 JSON 응답 직렬화에는 사용되지 않는다.
		 */
		this.xmlMapper = new XmlMapper(inputFactory);

		/*
		 * 공공데이터에 새로운 XML 태그가 추가되더라도 우리가 사용하지 않는 태그 때문에 전체 처리가 실패하지 않게 한다.
		 */
		this.xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	/**
	 * XML 문자열을 지정한 Java 타입으로 변환한다.
	 *
	 * @param xml        외부 기관이 반환한 XML 문자열
	 * @param targetType 변환할 Java 클래스
	 * @param <T>        변환 결과의 자료형
	 * @return XML에서 변환된 Java 객체
	 * @throws JsonProcessingException XML 구조를 해석하지 못한 경우
	 */
	public <T> T readValue(String xml, Class<T> targetType) throws JsonProcessingException {

		return xmlMapper.readValue(xml, targetType);
	}
}
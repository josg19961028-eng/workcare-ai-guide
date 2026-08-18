package egovframework.workcare.rehabilitation.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import egovframework.workcare.rehabilitation.infrastructure.RehabilitationPublicDataClient;
import egovframework.workcare.rehabilitation.infrastructure.dto.RehabilitationApiResponse;
import egovframework.workcare.rehabilitation.infrastructure.dto.RehabilitationApiResponse.InstitutionItem;
import egovframework.workcare.rehabilitation.service.RehabilitationInstitutionList;
import egovframework.workcare.rehabilitation.service.RehabilitationInstitutionService;
import egovframework.workcare.rehabilitation.service.RehabilitationPublicDataException;

/**
 * 근로복지공단 공공데이터를 이용해 재활기관 조회 업무를 처리하는 Service 구현체다.
 */
@Service
public class RehabilitationInstitutionServiceImpl implements RehabilitationInstitutionService {
	private static final Logger LOGGER = LoggerFactory.getLogger(RehabilitationInstitutionServiceImpl.class);
	 // 근로복지공단 API에서 정상 처리를 의미하는 결과 코드다.
	private static final String SUCCESS_RESULT_CODE = "00";
	 // 실제 공공데이터 API 통신을 담당하는 Client다.
	private final RehabilitationPublicDataClient publicDataClient;

	/**
	 * 생성자 주입을 사용한다.
	 * @param publicDataClient 근로복지공단 공공데이터 Client
	 */
	public RehabilitationInstitutionServiceImpl(RehabilitationPublicDataClient publicDataClient) {
		this.publicDataClient = publicDataClient;
	}

	/**
	 * 재활기관 목록을 조회한다.
	 */
	@Override
	public RehabilitationInstitutionList findInstitutions(int page, int size) {
		/*
		 * 1. Controller로부터 전달받은 페이지 번호를 방어적으로 검사한다.
		 *
		 * Controller의 입력값 검증을 우회해 Service가 직접 호출되더라도 비정상적인 요청이 외부 API까지 전달되지 않게 한다.
		 */
		if (page < 1) {
			throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
		}

		/*
		 * 한 번에 지나치게 많은 데이터를 요청하면 외부 API와 우리 서버 모두에 부하가 발생할 수 있다.
		 *
		 * 따라서 한 요청의 최대 조회 건수를 100건으로 제한한다. 이는 자원 고갈 공격과 과도한 트래픽을 줄이는 데도 도움이 된다.
		 */
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하이어야 합니다.");
		}

		/*
		 * 2. 공공데이터 API를 호출한다.
		 *
		 * Client는 HTTP 통신과 XML 변환을 담당하고, Service는 응답이 업무적으로 정상인지 판단한다.
		 */
		RehabilitationApiResponse apiResponse = publicDataClient.fetchInstitutionList(page, size);

		/*
		 * 3. 응답의 기본 구조를 확인한다.
		 *
		 * 외부 시스템의 응답은 항상 신뢰할 수 없으므로 null 여부와 필수 영역을 반드시 검사한다.
		 */
		if (apiResponse == null || apiResponse.header() == null) {
			throw new RehabilitationPublicDataException("재활기관 공공데이터 응답 정보가 올바르지 않습니다.");
		}

		/*
		 * HTTP 상태가 200이어도 공공데이터의 resultCode가 오류 코드일 수 있으므로 업무 결과 코드를 별도로 확인해야 한다.
		 */
		String resultCode = apiResponse.header().resultCode();

		if (!SUCCESS_RESULT_CODE.equals(resultCode)) {

			/*
			 * 인증키와 XML 원문은 기록하지 않는다. 문제 추적에 필요한 결과 코드만 로그에 남긴다.
			 */
			LOGGER.warn("재활기관 공공데이터 API가 오류 결과를 반환했습니다. resultCode={}", resultCode);

			throw new RehabilitationPublicDataException("재활기관 공공데이터를 조회하지 못했습니다.");
		}

		/*
		 * 정상 코드이지만 body가 없다면 정상적인 목록 응답으로 볼 수 없다.
		 */
		if (apiResponse.body() == null) {
			throw new RehabilitationPublicDataException("재활기관 공공데이터 본문이 존재하지 않습니다.");
		}

		RehabilitationApiResponse.Body body = apiResponse.body();

		/*
		 * XML에 <items> 영역이 없거나 그 안에 <item> 데이터가 없으면
		 * 빈 목록으로 처리한다.
		 *
		 * 외부 공공데이터는 조회 결과가 0건일 때
		 * <items> 태그 자체를 생략할 수 있으므로 두 단계 모두 검사한다.
		 */
		List<InstitutionItem> sourceItems =
		        body.items() == null || body.items().item() == null
		                ? List.of()
		                : body.items().item();

		/*
		 * 4. 공공데이터의 필드명을 우리 서비스에서 사용하는 이름으로 변환한다.
		 */
		List<RehabilitationInstitutionList.Institution> institutions = sourceItems.stream()
				.map(this::convertInstitution).toList();

		/*
		 * 5. 외부 API DTO가 아닌 서비스 내부 결과 모델을 반환한다.
		 */
		return new RehabilitationInstitutionList(body.pageNo(), body.numOfRows(), body.totalCount(), institutions);
	}

	/**
	 * 공공데이터 기관 한 건을 서비스 내부 기관 모델로 변환한다.
	 *
	 * @param source 공공데이터 원본 기관 정보
	 * @return 서비스에서 사용할 기관 정보
	 */
	private RehabilitationInstitutionList.Institution convertInstitution(InstitutionItem source) {

		return new RehabilitationInstitutionList.Institution(

				// gigwanNm: 기관명
				source.gigwanNm(),

				// gigwanFg: 기관 유형 코드
				source.gigwanFg(),

				// gigwanFgNm: 기관 유형명
				source.gigwanFgNm(),

				// jisaNm: 관리 지사명
				source.jisaNm(),

				// addr: 기관 주소
				source.addr(),

				// telNo: 전화번호
				source.telNo(),

				// faxNo: 팩스번호
				source.faxNo(),

				// urlAddr: 홈페이지 주소
				source.urlAddr());
	}
}
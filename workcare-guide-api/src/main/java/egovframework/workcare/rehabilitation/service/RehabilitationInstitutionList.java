package egovframework.workcare.rehabilitation.service;

import java.util.List;

/**
 * 재활기관 목록 조회 업무에서 사용하는 서비스 계층의 결과 모델이다.
 */
public record RehabilitationInstitutionList(
		// 현재 조회한 페이지 번호
		int page,
		// 한 번에 조회한 기관 수
		int size,
		// 공공데이터가 제공하는 전체 기관 수
		int totalCount,
		// 현재 페이지에서 조회된 기관 목록
		List<Institution> institutions) {
	/**
	 * 서비스 계층에서 사용하는 재활기관 한 건의 정보다.
	 */
	public record Institution(
			// 재활기관 이름
			String institutionName,
			// 공공데이터에서 사용하는 기관 유형 코드
			String institutionTypeCode,
			// 사용자에게 표시할 기관 유형명
			String institutionTypeName,
			// 해당 기관을 관리하는 근로복지공단 지사명
			String managingBranchName,
			// 재활기관 주소
			String address,
			// 재활기관 전화번호
			String telephoneNumber,
			// 재활기관 팩스번호
			String faxNumber,
			// 재활기관 홈페이지 주소
			String websiteUrl) {
	}
}
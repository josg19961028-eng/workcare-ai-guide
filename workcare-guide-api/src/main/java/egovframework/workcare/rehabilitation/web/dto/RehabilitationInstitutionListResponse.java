package egovframework.workcare.rehabilitation.web.dto;

import java.util.List;

/**
 * 재활기관 목록 조회 API가 클라이언트에게 반환하는 응답 DTO다.
 *
 * <p>공공데이터 원본 필드명(gigwanNm, jisaNm 등)을 그대로 노출하지 않고,
 * 프런트엔드에서 이해하기 쉬운 이름으로 변환해 제공한다.</p>
 *
 * <p>외부 API 형식이 바뀌더라도 우리 서비스의 응답 형식을 일정하게
 * 유지하기 위한 목적도 있다.</p>
 *
 * @param page        현재 페이지 번호
 * @param size        한 페이지에서 조회한 데이터 수
 * @param totalCount  전체 재활기관 수
 * @param institutions 재활기관 목록
 */
public record RehabilitationInstitutionListResponse(
        int page,
        int size,
        int totalCount,
        List<Institution> institutions
) {

    /**
     * 사용자 화면에 표시할 재활기관 한 건의 정보다.
     *
     * <p>전화번호와 기관 구분 코드는 숫자 계산 대상이 아니므로
     * String으로 선언한다. 숫자 타입을 사용하면 앞자리 0이 사라질 수 있다.</p>
     *
     * @param institutionName 기관명
     * @param institutionTypeCode 기관 유형 코드
     * @param institutionTypeName 기관 유형명
     * @param managingBranchName 관리 지사명
     * @param address 주소
     * @param telephoneNumber 전화번호
     * @param faxNumber 팩스번호
     * @param websiteUrl 홈페이지 주소
     */
    public record Institution(
            String institutionName,
            String institutionTypeCode,
            String institutionTypeName,
            String managingBranchName,
            String address,
            String telephoneNumber,
            String faxNumber,
            String websiteUrl
    ) {
    }
}
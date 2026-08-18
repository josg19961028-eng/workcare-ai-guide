package egovframework.workcare.medical.web.dto;

import java.util.List;

/**
 * 재활인증 의료기관 목록 API가 Vue에 반환하는 JSON 응답 DTO다.
 *
 * <p>
 * 외부 XML의 hospitalNo, hospitalNm, gtCdNm1 등을
 * 프런트엔드가 이해하기 쉬운 필드명으로 변환하여 제공한다.
 * </p>
 *
 * @param page         현재 페이지 번호
 * @param size         한 페이지 결과 수
 * @param totalCount   전체 재활인증 의료기관 수
 * @param institutions 현재 페이지의 재활인증 의료기관 목록
 */
public record CertifiedRehabilitationInstitutionListResponse(
        int page,
        int size,
        int totalCount,
        List<Institution> institutions
) {

    /**
     * Vue 화면에 반환할 재활인증 의료기관 한 건이다.
     *
     * @param hospitalNumber      의료기관 식별번호
     * @param hospitalName        의료기관명
     * @param institutionTypeName 의료기관 종별명
     * @param managingBranchCode  관리 지사 코드
     * @param managingBranchName  관리 지사명
     * @param address             의료기관 주소
     * @param telephoneNumber     의료기관 전화번호
     * @param faxNumber           의료기관 팩스번호
     */
    public record Institution(
            String hospitalNumber,
            String hospitalName,
            String institutionTypeName,
            String managingBranchCode,
            String managingBranchName,
            String address,
            String telephoneNumber,
            String faxNumber
    ) {
    }
}
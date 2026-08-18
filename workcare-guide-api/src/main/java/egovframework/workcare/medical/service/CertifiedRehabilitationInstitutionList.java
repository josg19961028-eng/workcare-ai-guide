package egovframework.workcare.medical.service;

import java.util.List;

/**
 * 재활인증 의료기관 조회 업무의 Service 결과 모델이다.
 *
 * <p>
 * 근로복지공단 XML의 hospitalNm, gtCdNm1 같은 원본 필드명을
 * 우리 서비스에서 이해하기 쉬운 이름으로 변환하여 관리한다.
 * </p>
 *
 * @param page         현재 페이지 번호
 * @param size         한 페이지 결과 수
 * @param totalCount   전체 재활인증 의료기관 수
 * @param institutions 현재 페이지의 재활인증 의료기관 목록
 */
public record CertifiedRehabilitationInstitutionList(
        int page,
        int size,
        int totalCount,
        List<Institution> institutions
) {

    /**
     * 재활인증 의료기관 한 건의 정보다.
     *
     * @param hospitalNumber       의료기관 식별번호
     * @param hospitalName         의료기관명
     * @param institutionTypeName  의료기관 종별명
     * @param managingBranchCode   관리 지사 코드
     * @param managingBranchName   관리 지사명
     * @param address              의료기관 주소
     * @param telephoneNumber      의료기관 전화번호
     * @param faxNumber            의료기관 팩스번호
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
package egovframework.workcare.medical.service;

import java.util.List;

/**
 * 산재지정 의료기관 조회 업무의 Service 결과 모델이다.
 *
 * <p>
 * 근로복지공단 원본 XML DTO를 Controller에 직접 전달하지 않고,
 * 우리 서비스에서 사용하는 의미 있는 필드명으로 변환해 관리한다.
 * </p>
 *
 * @param page         현재 페이지 번호
 * @param size         한 페이지 결과 수
 * @param totalCount   전체 의료기관 수
 * @param institutions 현재 페이지의 의료기관 목록
 */
public record MedicalInstitutionList(
        int page,
        int size,
        int totalCount,
        List<Institution> institutions
) {

    /**
     * Service 계층에서 사용하는 산재지정 의료기관 한 건이다.
     *
     * @param hospitalName         의료기관명
     * @param managingBranchCode   관리 지사 코드
     * @param managingBranchName   관리 지사명
     * @param address              의료기관 주소
     * @param telephoneNumber      의료기관 전화번호
     * @param faxNumber            의료기관 팩스번호
     */
    public record Institution(
            String hospitalName,
            String managingBranchCode,
            String managingBranchName,
            String address,
            String telephoneNumber,
            String faxNumber
    ) {
    }
}
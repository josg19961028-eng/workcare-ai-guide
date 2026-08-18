package egovframework.workcare.medical.web.dto;

import java.util.List;

/**
 * 산재지정 의료기관 목록 API가 Vue에 반환하는 JSON 응답 DTO다.
 *
 * <p>
 * 근로복지공단의 원본 필드명인 hospitalNm, jisaNm 등을
 * 프런트엔드 개발자가 이해하기 쉬운 이름으로 변환해 제공한다.
 * </p>
 *
 * @param page         현재 페이지 번호
 * @param size         한 페이지 결과 수
 * @param totalCount   전체 산재지정 의료기관 수
 * @param institutions 현재 페이지의 의료기관 목록
 */
public record MedicalInstitutionListResponse(
        int page,
        int size,
        int totalCount,
        List<Institution> institutions
) {

    /**
     * Vue 화면에 반환할 산재지정 의료기관 한 건이다.
     *
     * <p>
     * 원본 외부 API DTO를 그대로 반환하지 않고,
     * 사용자 화면에 공개하기로 결정한 필드만 명시적으로 제공한다.
     * </p>
     *
     * @param hospitalName       의료기관명
     * @param managingBranchCode 관리 지사 코드
     * @param managingBranchName 관리 지사명
     * @param address            의료기관 주소
     * @param telephoneNumber    의료기관 전화번호
     * @param faxNumber          의료기관 팩스번호
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
package egovframework.workcare.medical.web.dto;

import java.util.List;

/**
 * 산재지정 약국 목록 API가 Vue에 반환하는 JSON 응답 DTO다.
 *
 * <p>
 * 공공데이터 원본에서는 약국명도 hospitalNm으로 제공하지만,
 * Vue에는 의미가 분명한 pharmacyName으로 반환한다.
 * </p>
 *
 * @param page       현재 페이지 번호
 * @param size       한 페이지 결과 수
 * @param totalCount 전체 산재지정 약국 수
 * @param pharmacies 현재 페이지의 약국 목록
 */
public record PharmacyListResponse(
        int page,
        int size,
        int totalCount,
        List<Pharmacy> pharmacies
) {

    /**
     * Vue 화면에 반환할 산재지정 약국 한 건이다.
     *
     * @param pharmacyName        약국명
     * @param managingBranchCode  관리 지사 코드
     * @param managingBranchName  관리 지사명
     * @param address             약국 주소
     * @param telephoneNumber     약국 전화번호
     * @param faxNumber           약국 팩스번호
     */
    public record Pharmacy(
            String pharmacyName,
            String managingBranchCode,
            String managingBranchName,
            String address,
            String telephoneNumber,
            String faxNumber
    ) {
    }
}
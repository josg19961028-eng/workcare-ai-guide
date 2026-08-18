package egovframework.workcare.medical.service;

import java.util.List;

/**
 * 산재지정 약국 조회 업무의 Service 결과 모델이다.
 *
 * <p>
 * 공공데이터의 hospitalNm 같은 원본 필드명을
 * 우리 서비스에서 이해하기 쉬운 이름으로 변환해 관리한다.
 * </p>
 *
 * @param page       현재 페이지 번호
 * @param size       한 페이지 결과 수
 * @param totalCount 전체 산재지정 약국 수
 * @param pharmacies 현재 페이지의 약국 목록
 */
public record PharmacyList(
        int page,
        int size,
        int totalCount,
        List<Pharmacy> pharmacies
) {

    /**
     * 산재지정 약국 한 건의 정보다.
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
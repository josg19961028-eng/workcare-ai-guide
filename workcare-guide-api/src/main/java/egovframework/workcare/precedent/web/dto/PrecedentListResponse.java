package egovframework.workcare.precedent.web.dto;

import java.util.List;

/**
 * 판례 검색 API가 Vue에 반환하는 JSON 응답이다.
 */
public record PrecedentListResponse(
        int page,
        int size,
        int totalCount,
        List<Precedent> precedents
) {

    /**
     * 사용자 화면에 공개할 판례 한 건이다.
     */
    public record Precedent(
            String caseNumber,
            String courtName,
            String resultType,
            String caseType,
            String accidentDiseaseType,
            String title,

            /*
             * 판결문 원문이다.
             * Vue에서는 기본적으로 접어 두고 사용자가 상세보기를
             * 선택했을 때만 표시할 예정이다.
             */
            String content
    ) {
    }
}
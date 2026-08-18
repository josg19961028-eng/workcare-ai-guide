package egovframework.workcare.chat.service;

import java.util.List;

/**
 * 판례 검색형 챗봇의 Service 처리 결과다.
 *
 * @param answer     사용자에게 보여줄 안내 문장
 * @param precedents 관련 판례 목록
 */
public record PrecedentChatResult(
        String answer,
        List<Precedent> precedents
) {

    /**
     * 챗봇이 안내할 관련 판례 한 건이다.
     *
     * @param caseNumber          사건번호
     * @param courtName           법원명
     * @param resultType          판결결과
     * @param caseType            사건유형
     * @param accidentDiseaseType 사고·질병 구분
     * @param title               사건명
     * @param matchedExcerpt      질문과 가장 유사한 판결문 일부
     * @param similarityScore     0~1 유사도
     * @param similarityPercent   화면 표시용 유사도 백분율
     */
    public record Precedent(
            String caseNumber,
            String courtName,
            String resultType,
            String caseType,
            String accidentDiseaseType,
            String title,
            String matchedExcerpt,
            double similarityScore,
            int similarityPercent
    ) {
    }
}
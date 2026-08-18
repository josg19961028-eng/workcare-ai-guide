package egovframework.workcare.chat.web.dto;

import java.util.List;

/**
 * 판례 검색형 챗봇 응답이다.
 *
 * @param answer     챗봇 안내 문장
 * @param disclaimer 법률·산재 판단 관련 주의 문구
 * @param precedents 관련 판례 카드
 */
public record PrecedentChatResponse(
        String answer,
        String disclaimer,
        List<Precedent> precedents
) {

    /**
     * 관련 판례 카드 한 건이다.
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
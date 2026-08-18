package egovframework.workcare.precedent.infrastructure.persistence;

/**
 * DB에서 임베딩 대상을 조회한 결과다.
 *
 * @param precedentId        판례 내부 식별번호
 * @param caseNumber         사건번호
 * @param courtName          법원명
 * @param resultType         판결결과
 * @param caseType           사건유형
 * @param accidentDiseaseType 사고·질병 구분
 * @param title              사건명
 * @param content            판결문 전문
 * @param contentHash        판결문 SHA-256 해시
 */
public record PrecedentForEmbedding(
        long precedentId,
        String caseNumber,
        String courtName,
        String resultType,
        String caseType,
        String accidentDiseaseType,
        String title,
        String content,
        String contentHash
) {
}
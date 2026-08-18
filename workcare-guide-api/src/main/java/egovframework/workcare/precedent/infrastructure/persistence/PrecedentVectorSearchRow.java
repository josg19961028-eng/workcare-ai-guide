package egovframework.workcare.precedent.infrastructure.persistence;

/**
 * Oracle 벡터 유사도 검색 결과 한 건이다.
 *
 * @param precedentId        판례 내부 ID
 * @param caseNumber         사건번호
 * @param courtName          법원명
 * @param resultType         판결결과
 * @param caseType           사건유형
 * @param accidentDiseaseType 사고·질병 구분
 * @param title              사건명
 * @param chunkNo            가장 유사한 청크 번호
 * @param chunkText          가장 유사한 판결문 청크
 * @param distance           질문과 청크 사이의 코사인 거리
 */
public record PrecedentVectorSearchRow(
        long precedentId,
        String caseNumber,
        String courtName,
        String resultType,
        String caseType,
        String accidentDiseaseType,
        String title,
        int chunkNo,
        String chunkText,
        double distance
) {
}
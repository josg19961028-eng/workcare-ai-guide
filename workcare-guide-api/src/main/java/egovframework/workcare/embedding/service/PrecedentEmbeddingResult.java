package egovframework.workcare.embedding.service;

/**
 * 판례 임베딩 작업의 처리 결과다.
 *
 * @param selectedPrecedentCount 임베딩 대상으로 조회한 판례 수
 * @param completedPrecedentCount 임베딩을 완료한 판례 수
 * @param storedChunkCount Oracle에 저장한 전체 청크 수
 * @param skippedPrecedentCount 내용이 없어 제외한 판례 수
 */
public record PrecedentEmbeddingResult(
        int selectedPrecedentCount,
        int completedPrecedentCount,
        int storedChunkCount,
        int skippedPrecedentCount
) {
}
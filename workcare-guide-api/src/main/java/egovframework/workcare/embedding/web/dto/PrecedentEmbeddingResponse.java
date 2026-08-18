package egovframework.workcare.embedding.web.dto;

/**
 * 판례 임베딩 API 응답이다.
 *
 * @param selectedPrecedentCount 조회된 판례 수
 * @param completedPrecedentCount 완료한 판례 수
 * @param storedChunkCount 저장한 청크 수
 * @param skippedPrecedentCount 제외한 판례 수
 */
public record PrecedentEmbeddingResponse(
        int selectedPrecedentCount,
        int completedPrecedentCount,
        int storedChunkCount,
        int skippedPrecedentCount
) {
}
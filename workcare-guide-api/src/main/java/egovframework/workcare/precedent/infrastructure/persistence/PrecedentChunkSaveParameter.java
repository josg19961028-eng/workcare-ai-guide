package egovframework.workcare.precedent.infrastructure.persistence;

/**
 * 판결문 청크와 임베딩 벡터를 DB에 저장할 때 사용하는 데이터다.
 *
 * @param precedentId   원본 판례 ID
 * @param chunkNo       판례 안에서의 청크 순번
 * @param chunkText     분할된 판결문
 * @param chunkHash     청크 SHA-256 해시
 * @param embedding     1024차원 FLOAT32 벡터
 * @param embeddingModel 사용한 임베딩 모델명
 */
public record PrecedentChunkSaveParameter(
        long precedentId,
        int chunkNo,
        String chunkText,
        String chunkHash,
        float[] embedding,
        String embeddingModel
) {
}
package egovframework.workcare.embedding.web.dto;

import java.util.List;

/**
 * 로컬 임베딩 연결 확인 API의 응답 데이터다.
 *
 * <p>1024개 값을 모두 반환하지 않고 검증에 필요한
 * 앞의 5개 값만 반환한다.</p>
 *
 * @param status    임베딩 처리 상태
 * @param model     사용한 모델명
 * @param dimension 전체 벡터 차원
 * @param preview   벡터 앞부분 미리보기
 */
public record EmbeddingProbeResponse(
        String status,
        String model,
        int dimension,
        List<Double> preview
) {
}
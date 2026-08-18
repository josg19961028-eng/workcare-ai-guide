package egovframework.workcare.embedding.infrastructure.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ollama /api/embed API의 요청 및 응답 구조를 정의한다.
 *
 * <p>외부 API 전용 DTO이므로 Vue 화면에 직접 반환하지 않는다.</p>
 */
public final class OllamaEmbedApi {

    /**
     * 객체 생성을 막기 위한 private 생성자다.
     *
     * <p>이 클래스는 Request와 Response 타입을 묶는 용도로만 사용한다.</p>
     */
    private OllamaEmbedApi() {
    }

    /**
     * Ollama 임베딩 요청 데이터다.
     *
     * @param model 사용할 모델명
     * @param input 벡터로 변환할 문장
     */
    public record Request(
            String model,
            String input
    ) {
    }

    /**
     * Ollama 임베딩 응답 데이터다.
     *
     * <p>Ollama는 여러 문장을 한꺼번에 요청할 수 있으므로
     * embeddings가 2차원 배열 형태로 반환된다.</p>
     *
     * @param model      실제 사용한 모델명
     * @param embeddings 문장별 임베딩 벡터 목록
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            String model,
            List<List<Double>> embeddings
    ) {
    }
}
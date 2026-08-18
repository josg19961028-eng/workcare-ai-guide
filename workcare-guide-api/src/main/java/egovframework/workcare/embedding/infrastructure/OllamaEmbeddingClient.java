package egovframework.workcare.embedding.infrastructure;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import egovframework.workcare.common.config.OllamaProperties;
import egovframework.workcare.embedding.infrastructure.dto.OllamaEmbedApi;

/**
 * Ollama의 bge-m3 모델을 호출하여
 * 문자열을 임베딩 벡터로 변환하는 Client다.
 *
 * <p>이 클래스는 Ollama와의 HTTP 통신 및 응답 형식 검증만 담당한다.
 * 판결문 분할, DB 저장, 유사도 검색은 Service에서 처리한다.</p>
 */
@Component
public class OllamaEmbeddingClient {

    /*
     * 비정상적으로 큰 문자열이 한 번에 전달되어
     * CPU와 메모리를 과도하게 사용하는 것을 방지한다.
     *
     * 실제 판결문은 다음 단계에서 더 작은 청크로 분할한다.
     */
    private static final int MAX_INPUT_LENGTH = 8_000;

    private static final String EMBED_PATH = "/api/embed";

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaEmbeddingClient(
            @Qualifier("ollamaRestClient")
            RestClient restClient,
            OllamaProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * 문자열 한 건을 임베딩 벡터로 변환한다.
     *
     * @param input 임베딩할 문자열
     * @return bge-m3가 생성한 1024차원 벡터
     */
    public List<Double> embed(String input) {
        validateInput(input);

        OllamaEmbedApi.Request request =
                new OllamaEmbedApi.Request(
                        properties.embeddingModel(),
                        input
                );

        try {
            OllamaEmbedApi.Response response =
                    restClient.post()
                            .uri(EMBED_PATH)
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(request)
                            .retrieve()
                            .body(
                                    OllamaEmbedApi.Response.class
                            );

            return extractAndValidateVector(response);

        } catch (RestClientException exception) {
            /*
             * 임베딩할 원문은 예외 메시지나 로그에 포함하지 않는다.
             * 사용자 상담 내용이나 판결문에 민감정보가 있을 수 있기 때문이다.
             */
            throw new IllegalStateException(
                    "Ollama 임베딩 서버를 호출하지 못했습니다.",
                    exception
            );
        }
    }

    /**
     * Ollama에 전달할 문자열을 검증한다.
     */
    private void validateInput(String input) {
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException(
                    "임베딩할 문자열이 필요합니다."
            );
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException(
                    "임베딩할 문자열이 허용 길이를 초과했습니다."
            );
        }
    }

    /**
     * Ollama 응답에서 첫 번째 벡터를 꺼내고
     * 차원 및 숫자 형식을 검증한다.
     */
    private List<Double> extractAndValidateVector(
            OllamaEmbedApi.Response response
    ) {
        if (response == null
                || response.embeddings() == null
                || response.embeddings().isEmpty()) {

            throw new IllegalStateException(
                    "Ollama가 임베딩 벡터를 반환하지 않았습니다."
            );
        }

        List<Double> vector =
                response.embeddings().get(0);

        if (vector == null
                || vector.size()
                != properties.embeddingDimension()) {

            throw new IllegalStateException(
                    "Ollama 임베딩 벡터 차원이 올바르지 않습니다."
            );
        }

        /*
         * null, NaN, 무한대 값은 Oracle VECTOR에 저장할 수 없고
         * 유사도 계산 결과도 신뢰할 수 없으므로 차단한다.
         */
        boolean containsInvalidNumber =
                vector.stream()
                        .anyMatch(value ->
                                value == null
                                || !Double.isFinite(value)
                        );

        if (containsInvalidNumber) {
            throw new IllegalStateException(
                    "Ollama 임베딩 벡터에 유효하지 않은 값이 있습니다."
            );
        }

        /*
         * 외부 응답 목록이 이후 코드에서 변경되지 않도록
         * 불변 목록으로 복사하여 반환한다.
         */
        return List.copyOf(vector);
    }
}
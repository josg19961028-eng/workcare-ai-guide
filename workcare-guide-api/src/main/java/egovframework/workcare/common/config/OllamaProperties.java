package egovframework.workcare.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 로컬 Ollama 임베딩 서버 연동 설정을 관리한다.
 *
 * <p>URL, 모델명, 벡터 차원을 Java 코드에 직접 작성하지 않고
 * 환경설정으로 분리한다. 모델을 변경하거나 배포환경이 달라져도
 * Java 코드를 수정하지 않고 설정만 교체할 수 있다.</p>
 *
 * @param baseUrl            Ollama API 기본 주소
 * @param embeddingModel     사용할 임베딩 모델명
 * @param embeddingDimension 모델이 반환해야 하는 벡터 차원
 */
@Validated
@ConfigurationProperties(prefix = "workcare.ollama")
public record OllamaProperties(

        @NotBlank(
                message = "Ollama API 주소가 설정되지 않았습니다."
        )
        String baseUrl,

        @NotBlank(
                message = "Ollama 임베딩 모델이 설정되지 않았습니다."
        )
        String embeddingModel,

        @Min(
                value = 1,
                message = "임베딩 벡터 차원은 1 이상이어야 합니다."
        )
        int embeddingDimension
) {
}
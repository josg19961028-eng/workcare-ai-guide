package egovframework.workcare.precedent.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import egovframework.workcare.common.config.OllamaProperties;
import egovframework.workcare.embedding.infrastructure.OllamaEmbeddingClient;
import egovframework.workcare.precedent.infrastructure.persistence.PrecedentChunkMapper;
import egovframework.workcare.precedent.infrastructure.persistence.PrecedentVectorSearchRow;

/**
 * 사용자의 사고 설명과 의미적으로 가까운 판례를 검색한다.
 */
@Service
public class SemanticPrecedentSearchService {

    /*
     * 사용자가 요청하는 기본 검색 결과 수다.
     */
    private static final int SEARCH_LIMIT = 3;

    /*
     * 비정상적으로 긴 요청으로 Ollama CPU 자원을 과도하게
     * 사용하는 것을 방지한다.
     */
    private static final int MAX_QUESTION_LENGTH = 500;

    private final OllamaEmbeddingClient embeddingClient;
    private final PrecedentChunkMapper chunkMapper;
    private final OllamaProperties ollamaProperties;

    public SemanticPrecedentSearchService(
            OllamaEmbeddingClient embeddingClient,
            PrecedentChunkMapper chunkMapper,
            OllamaProperties ollamaProperties
    ) {
        this.embeddingClient = embeddingClient;
        this.chunkMapper = chunkMapper;
        this.ollamaProperties = ollamaProperties;
    }

    /**
     * 사용자 질문과 의미적으로 가까운 판례 3건을 검색한다.
     *
     * @param question 사용자의 사고 또는 질병 설명
     * @return 의미 검색 결과
     */
    public SemanticPrecedentSearchResult search(
            String question
    ) {
        String normalizedQuestion =
                validateAndNormalizeQuestion(question);

        /*
         * 판결문을 임베딩한 모델과 동일한 bge-m3 모델로
         * 사용자 질문도 임베딩해야 벡터를 비교할 수 있다.
         */
        List<Double> doubleVector =
                embeddingClient.embed(normalizedQuestion);

        float[] queryVector =
                convertToFloatArray(doubleVector);

        List<PrecedentVectorSearchRow> rows =
                chunkMapper.selectSimilarPrecedents(
                        queryVector,
                        SEARCH_LIMIT
                );

        List<SemanticPrecedentSearchResult.Match> matches =
                rows.stream()
                        .map(this::convertMatch)
                        .toList();

        return new SemanticPrecedentSearchResult(matches);
    }

    /**
     * 사용자 질문을 검증하고 앞뒤 공백을 제거한다.
     */
    private String validateAndNormalizeQuestion(
            String question
    ) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException(
                    "검색할 사고 또는 질병 내용을 입력해주세요."
            );
        }

        String normalized = question.strip();

        if (normalized.length() > MAX_QUESTION_LENGTH) {
            throw new IllegalArgumentException(
                    "검색 질문은 500자 이하여야 합니다."
            );
        }

        /*
         * 줄바꿈 등 제어문자를 이용한 로그 변조와
         * 비정상 요청을 차단한다.
         */
        boolean containsControlCharacter =
                normalized.chars()
                        .anyMatch(Character::isISOControl);

        if (containsControlCharacter) {
            throw new IllegalArgumentException(
                    "검색 질문에 허용되지 않은 문자가 있습니다."
            );
        }

        return normalized;
    }

    /**
     * Oracle 검색 결과를 Service 결과로 변환한다.
     */
    private SemanticPrecedentSearchResult.Match convertMatch(
            PrecedentVectorSearchRow source
    ) {
        double distance =
                source.distance();

        /*
         * 코사인 거리를 사용자에게 보여주기 쉬운
         * 0~1 사이 유사도 점수로 변환한다.
         *
         * 검색 정렬은 변환 점수가 아니라 DB가 계산한
         * 원래 코사인 거리를 사용한다.
         */
        double similarityScore =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                1.0 - distance
                        )
                );

        return new SemanticPrecedentSearchResult.Match(
                source.caseNumber(),
                source.courtName(),
                source.resultType(),
                source.caseType(),
                source.accidentDiseaseType(),
                source.title(),
                source.chunkText(),
                distance,
                similarityScore
        );
    }

    /**
     * Ollama의 Double 목록을 Oracle FLOAT32용 float[]로 변환한다.
     */
    private float[] convertToFloatArray(
            List<Double> vector
    ) {
        if (vector == null
                || vector.size()
                != ollamaProperties.embeddingDimension()) {

            throw new IllegalStateException(
                    "검색 질문 벡터 차원이 올바르지 않습니다."
            );
        }

        float[] result =
                new float[vector.size()];

        for (int index = 0; index < vector.size(); index++) {
            Double value = vector.get(index);

            if (value == null || !Double.isFinite(value)) {
                throw new IllegalStateException(
                        "검색 질문 벡터에 유효하지 않은 값이 있습니다."
                );
            }

            float convertedValue =
                    value.floatValue();

            if (!Float.isFinite(convertedValue)) {
                throw new IllegalStateException(
                        "검색 질문 벡터를 FLOAT32로 변환할 수 없습니다."
                );
            }

            result[index] = convertedValue;
        }

        return result;
    }
}
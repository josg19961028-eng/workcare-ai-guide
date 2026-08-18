package egovframework.workcare.embedding.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import egovframework.workcare.common.config.OllamaProperties;
import egovframework.workcare.embedding.infrastructure.OllamaEmbeddingClient;
import egovframework.workcare.precedent.infrastructure.persistence.PrecedentChunkMapper;
import egovframework.workcare.precedent.infrastructure.persistence.PrecedentChunkSaveParameter;
import egovframework.workcare.precedent.infrastructure.persistence.PrecedentForEmbedding;

/**
 * Oracle에 저장된 판결문을 청크로 분할하고,
 * Ollama를 이용해 임베딩한 후 VECTOR 컬럼에 저장한다.
 */
@Service
public class PrecedentEmbeddingService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    PrecedentEmbeddingService.class
            );

    /*
     * 판결문을 최대 1,000자 단위로 분리한다.
     *
     * 너무 큰 청크는 하나의 벡터에 여러 의미가 섞이고,
     * 너무 작은 청크는 판결의 문맥을 충분히 담지 못할 수 있다.
     */
    private static final int CHUNK_SIZE = 1_000;

    /*
     * 인접 청크 사이에 150자를 중복 포함한다.
     *
     * 중요한 문장이 청크 경계에서 잘려 의미가 유실되는 것을
     * 줄이기 위한 설정이다.
     */
    private static final int CHUNK_OVERLAP = 150;

    /*
     * 가능한 경우 청크 최대 길이의 70% 이후에서
     * 문장 끝이나 줄바꿈을 찾아 자연스럽게 분리한다.
     */
    private static final int MIN_BOUNDARY_PERCENT = 70;

    private final PrecedentChunkMapper chunkMapper;
    private final OllamaEmbeddingClient embeddingClient;
    private final OllamaProperties ollamaProperties;
    private final TransactionTemplate transactionTemplate;

    public PrecedentEmbeddingService(
            PrecedentChunkMapper chunkMapper,
            OllamaEmbeddingClient embeddingClient,
            OllamaProperties ollamaProperties,
            TransactionTemplate transactionTemplate
    ) {
        this.chunkMapper = chunkMapper;
        this.embeddingClient = embeddingClient;
        this.ollamaProperties = ollamaProperties;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 아직 청크가 없는 판례를 조회하여 임베딩한다.
     *
     * @param limit 한 번에 처리할 최대 판례 수
     * @return 임베딩 처리 결과
     */
    public PrecedentEmbeddingResult embedPendingPrecedents(
            int limit
    ) {
        /*
         * 아직 청크가 없는 판례만 제한된 수만큼 조회한다.
         */
        List<PrecedentForEmbedding> precedents =
                chunkMapper.selectPrecedentsWithoutChunks(
                        limit
                );

        int completedPrecedentCount = 0;
        int storedChunkCount = 0;
        int skippedPrecedentCount = 0;

        for (PrecedentForEmbedding precedent : precedents) {

            /*
             * 판결문을 먼저 청크로 분리한다.
             */
            List<String> chunks =
                    splitContent(precedent.content());

            if (chunks.isEmpty()) {
                /*
                 * 판결문 원문은 개인정보를 포함할 수 있으므로
                 * 로그에는 내부 판례 ID만 기록한다.
                 */
                LOGGER.warn(
                        "판결문 내용이 없어 임베딩을 제외했습니다. precedentId={}",
                        precedent.precedentId()
                );

                skippedPrecedentCount++;
                continue;
            }

            /*
             * 외부 Ollama 호출은 DB 트랜잭션 밖에서 모두 완료한다.
             *
             * Ollama 응답을 기다리는 동안 DB 연결과 트랜잭션을
             * 점유하지 않기 위한 설계다.
             */
            List<PrecedentChunkSaveParameter> parameters =
                    createChunkParameters(
                            precedent,
                            chunks
                    );

            /*
             * 모든 청크 임베딩에 성공한 뒤에만
             * 기존 청크 삭제와 신규 청크 저장을 실행한다.
             *
             * 임베딩 중간에 실패하면 기존 DB 데이터는 변경되지 않는다.
             */
            Integer affectedRows =
                    transactionTemplate.execute(
                            transactionStatus -> {
                                chunkMapper
                                        .deleteChunksByPrecedentId(
                                                precedent.precedentId()
                                        );

                                int insertedRows = 0;

                                for (PrecedentChunkSaveParameter
                                        parameter : parameters) {

                                    insertedRows +=
                                            chunkMapper.insertCompletedChunk(
                                                    parameter
                                            );
                                }

                                return insertedRows;
                            }
                    );

            storedChunkCount +=
                    affectedRows == null
                            ? 0
                            : affectedRows;

            completedPrecedentCount++;
        }

        return new PrecedentEmbeddingResult(
                precedents.size(),
                completedPrecedentCount,
                storedChunkCount,
                skippedPrecedentCount
        );
    }

    /**
     * 판결문 청크마다 Ollama 임베딩을 생성한다.
     */
    private List<PrecedentChunkSaveParameter>
            createChunkParameters(
                    PrecedentForEmbedding precedent,
                    List<String> chunks
            ) {
        List<PrecedentChunkSaveParameter> parameters =
                new ArrayList<>();

        for (int index = 0; index < chunks.size(); index++) {
            String chunkText = chunks.get(index);

            /*
             * 현재는 판결문 청크 본문만 임베딩한다.
             *
             * 판결결과·사건유형 등의 메타데이터는 향후
             * Oracle WHERE 조건을 이용한 구조화 검색에 사용한다.
             */
            List<Double> doubleVector =
                    embeddingClient.embed(chunkText);

            /*
             * Ollama의 Double 목록을 Oracle FLOAT32 컬럼에 맞는
             * float[]로 변환한다.
             */
            float[] floatVector =
                    convertToFloatArray(doubleVector);

            parameters.add(
                    new PrecedentChunkSaveParameter(
                            precedent.precedentId(),

                            /*
                             * 사람이 확인하기 편하도록
                             * 청크 번호는 0이 아니라 1부터 시작한다.
                             */
                            index + 1,

                            chunkText,
                            createSha256Hash(chunkText),
                            floatVector,
                            ollamaProperties.embeddingModel()
                    )
            );
        }

        return parameters;
    }

    /**
     * 판결문을 일정 크기의 중첩된 청크로 분할한다.
     */
    private List<String> splitContent(String content) {
        String normalizedContent =
                normalizeContent(content);

        if (!StringUtils.hasText(normalizedContent)) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();

        int contentLength = normalizedContent.length();
        int startIndex = 0;

        while (startIndex < contentLength) {
            int maximumEndIndex =
                    Math.min(
                            startIndex + CHUNK_SIZE,
                            contentLength
                    );

            int endIndex =
                    findNaturalBoundary(
                            normalizedContent,
                            startIndex,
                            maximumEndIndex
                    );

            /*
             * 비정상적인 경계 계산으로 무한 반복되는 것을
             * 방지하기 위한 방어 코드다.
             */
            if (endIndex <= startIndex) {
                endIndex = maximumEndIndex;
            }

            String chunk =
                    normalizedContent
                            .substring(
                                    startIndex,
                                    endIndex
                            )
                            .strip();

            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }

            if (endIndex >= contentLength) {
                break;
            }

            /*
             * 다음 청크를 현재 청크 끝보다 150자 앞에서 시작한다.
             * 최소 한 글자 이상 앞으로 이동하도록 보장한다.
             */
            startIndex = Math.max(
                    endIndex - CHUNK_OVERLAP,
                    startIndex + 1
            );
        }

        return chunks;
    }

    /**
     * 최대 청크 길이 안에서 문장 끝 또는 줄바꿈을 찾는다.
     */
    private int findNaturalBoundary(
            String content,
            int startIndex,
            int maximumEndIndex
    ) {
        /*
         * 마지막 청크라면 남은 내용을 모두 사용한다.
         */
        if (maximumEndIndex >= content.length()) {
            return content.length();
        }

        int minimumBoundaryIndex =
                startIndex
                + (CHUNK_SIZE * MIN_BOUNDARY_PERCENT / 100);

        /*
         * 최대 길이부터 뒤로 이동하면서 자연스러운 경계를 찾는다.
         */
        for (int index = maximumEndIndex - 1;
                index >= minimumBoundaryIndex;
                index--) {

            char character = content.charAt(index);

            if (character == '.'
                    || character == '?'
                    || character == '!'
                    || character == '。'
                    || character == '\n') {

                return index + 1;
            }
        }

        /*
         * 적당한 문장 경계를 찾지 못하면 최대 길이에서 분할한다.
         */
        return maximumEndIndex;
    }

    /**
     * 판결문 줄바꿈과 연속 공백을 정리한다.
     */
    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        return content
                /*
                 * Windows와 구형 Mac 줄바꿈을 Unix 형식으로 통일한다.
                 */
                .replace("\r\n", "\n")
                .replace('\r', '\n')

                /*
                 * 연속된 탭과 일반 공백을 한 칸으로 줄인다.
                 * 문단 구분을 위한 줄바꿈은 보존한다.
                 */
                .replaceAll("[\\t ]+", " ")

                /*
                 * 세 줄 이상의 연속 줄바꿈은 두 줄로 줄인다.
                 */
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    /**
     * Double 목록을 Oracle FLOAT32용 float[]로 변환한다.
     */
    private float[] convertToFloatArray(
            List<Double> vector
    ) {
        if (vector == null
                || vector.size()
                != ollamaProperties.embeddingDimension()) {

            throw new IllegalStateException(
                    "변환할 임베딩 벡터 차원이 올바르지 않습니다."
            );
        }

        float[] floatVector =
                new float[vector.size()];

        for (int index = 0; index < vector.size(); index++) {
            Double value = vector.get(index);

            if (value == null || !Double.isFinite(value)) {
                throw new IllegalStateException(
                        "임베딩 벡터에 유효하지 않은 값이 있습니다."
                );
            }

            float convertedValue =
                    value.floatValue();

            /*
             * Double에서는 유효했지만 float 변환 과정에서
             * 무한대로 변한 경우도 저장 전에 차단한다.
             */
            if (!Float.isFinite(convertedValue)) {
                throw new IllegalStateException(
                        "FLOAT32로 변환할 수 없는 벡터 값이 있습니다."
                );
            }

            floatVector[index] = convertedValue;
        }

        return floatVector;
    }

    /**
     * 청크 내용의 SHA-256 해시를 생성한다.
     */
    private String createSha256Hash(String text) {
        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes =
                    messageDigest.digest(
                            text.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 해시 기능을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}
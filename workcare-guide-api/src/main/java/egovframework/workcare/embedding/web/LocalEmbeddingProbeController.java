package egovframework.workcare.embedding.web;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.common.config.OllamaProperties;
import egovframework.workcare.embedding.infrastructure.OllamaEmbeddingClient;
import egovframework.workcare.embedding.web.dto.EmbeddingProbeResponse;

/**
 * Spring Boot와 로컬 Ollama의 연결 상태를 확인하는 Controller다.
 *
 * <p>개발용 진단 API이므로 local 프로필에서만 등록된다.</p>
 */
@Profile("local")
@RestController
@RequestMapping("/api/local/embedding/probe")
public class LocalEmbeddingProbeController {

    /*
     * 사용자 입력을 받기 전에 고정 문장으로 연동 상태만 확인한다.
     */
    private static final String PROBE_TEXT =
            "계단에서 무거운 짐을 들고 내려가다가 허리를 다쳤습니다.";

    private static final int PREVIEW_SIZE = 5;

    private final OllamaEmbeddingClient embeddingClient;
    private final OllamaProperties properties;

    public LocalEmbeddingProbeController(
            OllamaEmbeddingClient embeddingClient,
            OllamaProperties properties
    ) {
        this.embeddingClient = embeddingClient;
        this.properties = properties;
    }

    /**
     * 고정된 한국어 문장을 Ollama로 임베딩한다.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmbeddingProbeResponse> probeEmbedding() {

        List<Double> vector =
                embeddingClient.embed(PROBE_TEXT);

        int previewEndIndex =
                Math.min(PREVIEW_SIZE, vector.size());

        List<Double> preview =
                vector.subList(0, previewEndIndex);

        EmbeddingProbeResponse response =
                new EmbeddingProbeResponse(
                        "UP",
                        properties.embeddingModel(),
                        vector.size(),
                        preview
                );

        return ResponseEntity.ok(response);
    }
}
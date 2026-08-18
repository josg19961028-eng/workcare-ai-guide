package egovframework.workcare.embedding.web;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.embedding.service.PrecedentEmbeddingResult;
import egovframework.workcare.embedding.service.PrecedentEmbeddingService;
import egovframework.workcare.embedding.web.dto.PrecedentEmbeddingResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 개발환경에서 판례 임베딩 작업을 실행하는 Controller다.
 */
@Profile("local")
@Validated
@RestController
@RequestMapping("/api/local/embedding/precedents")
public class LocalPrecedentEmbeddingController {

    private final PrecedentEmbeddingService embeddingService;

    public LocalPrecedentEmbeddingController(
            PrecedentEmbeddingService embeddingService
    ) {
        this.embeddingService = embeddingService;
    }

    /**
     * 아직 임베딩되지 않은 판례를 제한된 수만큼 처리한다.
     *
     * @param limit 한 번에 처리할 최대 판례 수
     * @return 임베딩 처리 결과
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrecedentEmbeddingResponse>
            embedPrecedents(

            /*
             * 로컬 CPU 자원을 과도하게 사용하지 않도록
             * 한 요청에서 최대 3개 판례만 허용한다.
             */
            @RequestParam(defaultValue = "1")
            @Min(
                    value = 1,
                    message = "처리할 판례 수는 1 이상이어야 합니다."
            )
            @Max(
                    value = 3,
                    message = "한 번에 최대 3개 판례만 처리할 수 있습니다."
            )
            int limit
    ) {
        PrecedentEmbeddingResult result =
                embeddingService.embedPendingPrecedents(
                        limit
                );

        PrecedentEmbeddingResponse response =
                new PrecedentEmbeddingResponse(
                        result.selectedPrecedentCount(),
                        result.completedPrecedentCount(),
                        result.storedChunkCount(),
                        result.skippedPrecedentCount()
                );

        return ResponseEntity.ok(response);
    }
}
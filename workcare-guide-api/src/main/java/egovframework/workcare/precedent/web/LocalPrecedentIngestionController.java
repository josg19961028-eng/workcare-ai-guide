package egovframework.workcare.precedent.web;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.precedent.service.PrecedentIngestionResult;
import egovframework.workcare.precedent.service.PrecedentIngestionService;
import egovframework.workcare.precedent.web.dto.PrecedentIngestionResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 개발환경에서 판례 수집을 실행하는 REST API다.
 *
 * <p>DB 데이터를 변경하는 기능이므로 GET이 아닌 POST를 사용한다.</p>
 *
 * <p>현재는 관리자 인증이 구현되지 않았으므로 local 프로필에서만
 * Controller가 생성되도록 제한한다. 운영환경에서는 관리자 인증,
 * 권한 검사 및 실행 이력 기록이 필요하다.</p>
 */
@Profile("local")
@Validated
@RestController
@RequestMapping("/api/local/precedents/ingestion")
public class LocalPrecedentIngestionController {

    private final PrecedentIngestionService ingestionService;

    public LocalPrecedentIngestionController(
            PrecedentIngestionService ingestionService
    ) {
        this.ingestionService = ingestionService;
    }

    /**
     * 지정한 공공데이터 페이지의 판례를 Oracle에 저장한다.
     *
     * @param page 수집할 페이지 번호
     * @param size 한 번에 수집할 판례 수
     * @return 수집 및 저장 결과
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrecedentIngestionResponse> ingestPage(

            @RequestParam(defaultValue = "1")
            @Min(
                    value = 1,
                    message = "페이지 번호는 1 이상이어야 합니다."
            )
            int page,

            /*
             * 판결문 전문은 데이터 크기가 크므로
             * 한 번의 요청에서 최대 10건만 처리한다.
             */
            @RequestParam(defaultValue = "3")
            @Min(
                    value = 1,
                    message = "수집 건수는 1 이상이어야 합니다."
            )
            @Max(
                    value = 10,
                    message = "한 번에 최대 10건까지 수집할 수 있습니다."
            )
            int size
    ) {
        PrecedentIngestionResult result =
                ingestionService.ingestPage(page, size);

        PrecedentIngestionResponse response =
                new PrecedentIngestionResponse(
                        result.page(),
                        result.fetchedCount(),
                        result.storedCount(),
                        result.skippedCount()
                );

        return ResponseEntity.ok(response);
    }
}
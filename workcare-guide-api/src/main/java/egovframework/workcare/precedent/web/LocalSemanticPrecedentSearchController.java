package egovframework.workcare.precedent.web;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.precedent.service.SemanticPrecedentSearchResult;
import egovframework.workcare.precedent.service.SemanticPrecedentSearchService;
import egovframework.workcare.precedent.web.dto.SemanticPrecedentSearchRequest;
import egovframework.workcare.precedent.web.dto.SemanticPrecedentSearchResponse;
import jakarta.validation.Valid;

/**
 * 개발환경에서 의미 기반 판례 검색 API를 제공한다.
 *
 * <p>현재는 CPU 사용량 제한과 인증 기능이 완성되지 않았으므로
 * local 프로필에서만 활성화한다.</p>
 */
@Profile("local")
@RestController
@RequestMapping("/api/local/precedents/semantic-search")
public class LocalSemanticPrecedentSearchController {

    private static final String DISCLAIMER =
            "검색 결과는 참고 정보이며 산재 인정 여부나 법률 판단을 보장하지 않습니다.";

    private final SemanticPrecedentSearchService searchService;

    public LocalSemanticPrecedentSearchController(
            SemanticPrecedentSearchService searchService
    ) {
        this.searchService = searchService;
    }

    /**
     * 사용자의 사고 설명과 관련된 판례를 검색한다.
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SemanticPrecedentSearchResponse>
            searchPrecedents(

            @Valid
            @RequestBody
            SemanticPrecedentSearchRequest request
    ) {
        SemanticPrecedentSearchResult result =
                searchService.search(request.question());

        List<SemanticPrecedentSearchResponse.Result>
                responseResults =
                        result.matches()
                                .stream()
                                .map(this::convertResult)
                                .toList();

        SemanticPrecedentSearchResponse response =
                new SemanticPrecedentSearchResponse(
                        responseResults.size(),
                        DISCLAIMER,
                        responseResults
                );

        return ResponseEntity.ok(response);
    }

    private SemanticPrecedentSearchResponse.Result
            convertResult(
                    SemanticPrecedentSearchResult.Match source
            ) {
        return new SemanticPrecedentSearchResponse.Result(
                source.caseNumber(),
                source.courtName(),
                source.resultType(),
                source.caseType(),
                source.accidentDiseaseType(),
                source.title(),
                source.matchedChunk(),
                source.distance(),
                source.similarityScore()
        );
    }
}
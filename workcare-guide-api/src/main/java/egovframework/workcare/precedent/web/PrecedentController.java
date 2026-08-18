package egovframework.workcare.precedent.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.precedent.service.PrecedentList;
import egovframework.workcare.precedent.service.PrecedentSearchCondition;
import egovframework.workcare.precedent.service.PrecedentSearchOptions;
import egovframework.workcare.precedent.service.PrecedentService;
import egovframework.workcare.precedent.web.dto.PrecedentCountResponse;
import egovframework.workcare.precedent.web.dto.PrecedentListResponse;
import egovframework.workcare.precedent.web.dto.PrecedentSearchOptionsResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 산재보험 판례 검색 REST API를 제공한다.
 */
@Validated
@RestController
@RequestMapping("/api/precedents")
public class PrecedentController {

    private final PrecedentService precedentService;

    public PrecedentController(
            PrecedentService precedentService
    ) {
        this.precedentService = precedentService;
    }

    /**
     * 판례 검색 화면의 선택항목을 조회한다.
     */
    @GetMapping(
            value = "/options",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PrecedentSearchOptionsResponse>
            findSearchOptions() {

        PrecedentSearchOptions serviceResult =
                precedentService.findSearchOptions();

        PrecedentSearchOptionsResponse response =
                new PrecedentSearchOptionsResponse(
                        serviceResult.resultTypes(),
                        serviceResult.caseTypes(),
                        serviceResult.accidentDiseaseTypes()
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 검색조건에 해당하는 판례를 조회한다.
     *
     * <pre>
     * GET /api/precedents?page=1&amp;size=3
     *     &amp;resultType=기각
     *     &amp;caseType=장해
     *     &amp;accidentDiseaseType=업무상질병
     * </pre>
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrecedentListResponse> searchPrecedents(

            @RequestParam(defaultValue = "1")
            @Min(
                    value = 1,
                    message = "페이지 번호는 1 이상이어야 합니다."
            )
            int page,

            /*
             * 판결문 전문이 포함되므로 기본 3건, 최대 10건으로 제한한다.
             */
            @RequestParam(defaultValue = "3")
            @Min(
                    value = 1,
                    message = "페이지 크기는 1 이상이어야 합니다."
            )
            @Max(
                    value = 10,
                    message = "판례 페이지 크기는 10 이하여야 합니다."
            )
            int size,

            @RequestParam(defaultValue = "")
            @Size(
                    max = 50,
                    message = "판결결과는 50자 이하여야 합니다."
            )
            String resultType,

            @RequestParam(defaultValue = "")
            @Size(
                    max = 50,
                    message = "사건유형은 50자 이하여야 합니다."
            )
            String caseType,

            @RequestParam(defaultValue = "")
            @Size(
                    max = 50,
                    message = "사고·질병 구분은 50자 이하여야 합니다."
            )
            String accidentDiseaseType
    ) {
        PrecedentSearchCondition condition =
                new PrecedentSearchCondition(
                        page,
                        size,
                        resultType,
                        caseType,
                        accidentDiseaseType
                );

        PrecedentList serviceResult =
                precedentService.searchPrecedents(condition);

        return ResponseEntity.ok(
                convertListResponse(serviceResult)
        );
    }

    /**
     * 검색조건에 해당하는 판례 개수를 조회한다.
     */
    @GetMapping(
            value = "/count",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PrecedentCountResponse> countPrecedents(

            @RequestParam(defaultValue = "")
            @Size(max = 50)
            String resultType,

            @RequestParam(defaultValue = "")
            @Size(max = 50)
            String caseType,

            @RequestParam(defaultValue = "")
            @Size(max = 50)
            String accidentDiseaseType
    ) {
        int count = precedentService.countPrecedents(
                resultType,
                caseType,
                accidentDiseaseType
        );

        return ResponseEntity.ok(
                new PrecedentCountResponse(count)
        );
    }

    /**
     * Service 결과를 Vue에 공개할 API 응답 DTO로 변환한다.
     */
    private PrecedentListResponse convertListResponse(
            PrecedentList source
    ) {
        List<PrecedentListResponse.Precedent> precedents =
                source.precedents()
                        .stream()
                        .map(this::convertPrecedent)
                        .toList();

        return new PrecedentListResponse(
                source.page(),
                source.size(),
                source.totalCount(),
                precedents
        );
    }

    /**
     * 판례 한 건에서 공개할 필드만 명시적으로 선택한다.
     */
    private PrecedentListResponse.Precedent convertPrecedent(
            PrecedentList.Precedent source
    ) {
        return new PrecedentListResponse.Precedent(
                source.caseNumber(),
                source.courtName(),
                source.resultType(),
                source.caseType(),
                source.accidentDiseaseType(),
                source.title(),
                source.content()
        );
    }
}
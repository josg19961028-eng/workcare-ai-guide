package egovframework.workcare.rehabilitation.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.rehabilitation.service.RehabilitationInstitutionList;
import egovframework.workcare.rehabilitation.service.RehabilitationInstitutionService;
import egovframework.workcare.rehabilitation.web.dto.RehabilitationInstitutionListResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 사용자에게 재활기관 조회 기능을 제공하는 REST API Controller다.
 */
@Validated
@RestController
@RequestMapping("/api/rehabilitation-institutions")
public class RehabilitationInstitutionController {

    /**
     * 재활기관 조회 업무를 담당하는 Service다.
     */
    private final RehabilitationInstitutionService institutionService;

    /**
     * 생성자 주입을 이용해 Service 의존성을 전달받는다.
     */
    public RehabilitationInstitutionController(
            RehabilitationInstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    /**
     * 재활기관 목록을 페이지 단위로 조회한다.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RehabilitationInstitutionListResponse>
            findInstitutions(
                    @RequestParam(defaultValue = "1")
                    @Min(value = 1,
                            message = "페이지 번호는 1 이상이어야 합니다.")
                    int page,
                    @RequestParam(defaultValue = "10")
                    @Min(value = 1,
                            message = "페이지 크기는 1 이상이어야 합니다.")
                    @Max(value = 100,
                            message = "페이지 크기는 100 이하여야 합니다.")
                    int size) {

        /*
         * Controller는 HTTP 요청 처리를 담당하고,
         * 실제 조회 및 공공데이터 검증은 Service에 위임한다.
         */
        RehabilitationInstitutionList serviceResult =
                institutionService.findInstitutions(page, size);

        /*
         * Service 내부 모델을 외부에 공개할 API 응답 DTO로 변환한다.
         */
        RehabilitationInstitutionListResponse response =
                convertResponse(serviceResult);

        /*
         * 정상 처리되었으므로 HTTP 200 OK와 JSON 응답을 반환한다.
         */
        return ResponseEntity.ok(response);
    }

    /**
     * Service 계층의 결과를 사용자에게 반환할 API 응답 DTO로 변환한다.
     *
     * @param source Service 계층에서 반환한 재활기관 목록
     * @return API 응답 DTO
     */
    private RehabilitationInstitutionListResponse convertResponse(
            RehabilitationInstitutionList source) {

        /*
         * 서비스 계층의 기관 목록을 API 응답용 기관 목록으로 변환한다.
         */
        List<RehabilitationInstitutionListResponse.Institution>
                institutions =
                source.institutions()
                        .stream()
                        .map(this::convertInstitution)
                        .toList();

        /*
         * 페이지 정보와 변환된 기관 목록을 묶어서 반환한다.
         */
        return new RehabilitationInstitutionListResponse(
                source.page(),
                source.size(),
                source.totalCount(),
                institutions);
    }

    /**
     * Service 계층의 기관 한 건을 API 응답용 기관 한 건으로 변환한다.
     *
     * @param source Service 계층의 기관 정보
     * @return 사용자에게 공개할 기관 정보
     */
    private RehabilitationInstitutionListResponse.Institution
            convertInstitution(
                    RehabilitationInstitutionList.Institution source) {

        /*
         * API를 통해 공개하기로 결정한 필드만 명시적으로 복사한다.
         *
         * 외부 데이터 객체를 그대로 반환하지 않기 때문에
         * 불필요한 필드가 의도치 않게 노출되는 것을 방지할 수 있다.
         */
        return new RehabilitationInstitutionListResponse.Institution(
                source.institutionName(),
                source.institutionTypeCode(),
                source.institutionTypeName(),
                source.managingBranchName(),
                source.address(),
                source.telephoneNumber(),
                source.faxNumber(),
                source.websiteUrl());
    }
}
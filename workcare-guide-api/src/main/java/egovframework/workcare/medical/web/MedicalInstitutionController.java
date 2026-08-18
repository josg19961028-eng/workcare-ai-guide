package egovframework.workcare.medical.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.medical.service.MedicalInstitutionList;
import egovframework.workcare.medical.service.MedicalInstitutionService;
import egovframework.workcare.medical.web.dto.MedicalInstitutionListResponse;
import egovframework.workcare.medical.service.CertifiedRehabilitationInstitutionList;
import egovframework.workcare.medical.web.dto.CertifiedRehabilitationInstitutionListResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import egovframework.workcare.medical.service.PharmacyList;
import egovframework.workcare.medical.web.dto.PharmacyListResponse;

/**
 * 산재지정 의료기관 조회 기능을 제공하는 REST Controller다.
 *
 * <p>
 * Controller는 HTTP 요청값을 받고 Service를 호출한 후
 * 그 결과를 JSON 응답으로 변환하는 역할을 담당한다.
 * </p>
 */
@Validated
@RestController
@RequestMapping("/api/medical-institutions")
public class MedicalInstitutionController {

    /**
     * 산재지정 의료기관 조회 업무를 담당하는 Service다.
     */
    private final MedicalInstitutionService medicalInstitutionService;

    /**
     * 생성자 주입으로 Service를 전달받는다.
     *
     * @param medicalInstitutionService 의료기관 조회 Service
     */
    public MedicalInstitutionController(
            MedicalInstitutionService medicalInstitutionService
    ) {
        this.medicalInstitutionService = medicalInstitutionService;
    }

    /**
     * 산재지정 의료기관 목록을 페이지 단위로 조회한다.
     *
     * <p>
     * 호출 예시는 다음과 같다.
     * </p>
     *
     * <pre>
     * GET /api/medical-institutions?page=1&amp;size=10
     * </pre>
     *
     * @param page 조회할 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 산재지정 의료기관 목록 JSON
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MedicalInstitutionListResponse>
            findMedicalInstitutions(

                    /*
                     * page를 생략하면 첫 번째 페이지를 조회한다.
                     */
                    @RequestParam(defaultValue = "1")
                    @Min(
                            value = 1,
                            message = "페이지 번호는 1 이상이어야 합니다."
                    )
                    int page,

                    /*
                     * size를 생략하면 10건을 조회한다.
                     *
                     * 최대 100건으로 제한해 실수 또는 악의적인 대량 요청으로
                     * 서버와 외부 API 자원이 고갈되는 위험을 줄인다.
                     */
                    @RequestParam(defaultValue = "10")
                    @Min(
                            value = 1,
                            message = "페이지 크기는 1 이상이어야 합니다."
                    )
                    @Max(
                            value = 100,
                            message = "페이지 크기는 100 이하여야 합니다."
                    )
                    int size
            ) {

        /*
         * Controller는 외부 API를 직접 호출하지 않고,
         * 의료기관 조회 업무를 Service에 위임한다.
         */
        MedicalInstitutionList serviceResult =
                medicalInstitutionService.findMedicalInstitutions(
                        page,
                        size
                );

        /*
         * Service 결과를 Vue에 공개할 API 응답 DTO로 변환한다.
         */
        MedicalInstitutionListResponse response =
                convertResponse(serviceResult);

        /*
         * 정상 처리 결과이므로 HTTP 200 OK로 반환한다.
         */
        return ResponseEntity.ok(response);
    }

    /**
     * Service 결과를 API 응답 DTO로 변환한다.
     *
     * @param source Service의 의료기관 조회 결과
     * @return Vue에 반환할 API 응답
     */
    private MedicalInstitutionListResponse convertResponse(
            MedicalInstitutionList source
    ) {
        List<MedicalInstitutionListResponse.Institution> institutions =
                source.institutions()
                        .stream()
                        .map(this::convertInstitution)
                        .toList();

        return new MedicalInstitutionListResponse(
                source.page(),
                source.size(),
                source.totalCount(),
                institutions
        );
    }

    /**
     * 산재지정 약국 목록을 페이지 단위로 조회한다.
     *
     * <p>
     * 의료기관과 같은 Controller에서 처리하지만,
     * URL과 반환 DTO는 약국 업무에 맞게 구분한다.
     * </p>
     *
     * <pre>
     * GET /api/medical-institutions/pharmacies?page=1&amp;size=10
     * </pre>
     *
     * @param page 조회할 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 산재지정 약국 목록 JSON
     */
    @GetMapping(
            value = "/pharmacies",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PharmacyListResponse> findPharmacies(

            /*
             * page를 전달하지 않으면 첫 번째 페이지를 조회한다.
             */
            @RequestParam(defaultValue = "1")
            @Min(
                    value = 1,
                    message = "페이지 번호는 1 이상이어야 합니다."
            )
            int page,

            /*
             * 한 페이지 기본 조회 수는 10건이며
             * 최대 100건까지만 허용한다.
             */
            @RequestParam(defaultValue = "10")
            @Min(
                    value = 1,
                    message = "페이지 크기는 1 이상이어야 합니다."
            )
            @Max(
                    value = 100,
                    message = "페이지 크기는 100 이하여야 합니다."
            )
            int size
    ) {
        /*
         * 약국 조회 업무를 Service에 위임한다.
         */
        PharmacyList serviceResult =
                medicalInstitutionService.findPharmacies(
                        page,
                        size
                );

        /*
         * Service 결과를 Vue에 공개할 응답 DTO로 변환한다.
         */
        PharmacyListResponse response =
                convertPharmacyResponse(serviceResult);

        return ResponseEntity.ok(response);
    }

    /**
     * 재활인증 의료기관 목록을 페이지 단위로 조회한다.
     *
     * <p>
     * 재활인증 의료기관은 산재근로자에게 전문적인 재활치료를
     * 제공하는 의료기관 정보다.
     * </p>
     *
     * <pre>
     * GET /api/medical-institutions/certified-rehabilitation
     *     ?page=1&amp;size=10
     * </pre>
     *
     * @param page 조회할 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 재활인증 의료기관 목록 JSON
     */
    @GetMapping(
            value = "/certified-rehabilitation",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<
            CertifiedRehabilitationInstitutionListResponse>
            findCertifiedRehabilitationInstitutions(

                    /*
                     * page가 없으면 첫 번째 페이지를 조회한다.
                     */
                    @RequestParam(defaultValue = "1")
                    @Min(
                            value = 1,
                            message = "페이지 번호는 1 이상이어야 합니다."
                    )
                    int page,

                    /*
                     * 기본 10건, 최대 100건까지만 허용한다.
                     *
                     * 과도한 대량 요청으로 외부 API와 서버 자원이
                     * 소모되는 것을 줄이기 위한 제한이다.
                     */
                    @RequestParam(defaultValue = "10")
                    @Min(
                            value = 1,
                            message = "페이지 크기는 1 이상이어야 합니다."
                    )
                    @Max(
                            value = 100,
                            message = "페이지 크기는 100 이하여야 합니다."
                    )
                    int size
            ) {

        /*
         * Controller는 외부 API를 직접 호출하지 않고
         * 재활인증기관 조회 업무를 Service에 위임한다.
         */
        CertifiedRehabilitationInstitutionList serviceResult =
                medicalInstitutionService
                        .findCertifiedRehabilitationInstitutions(
                                page,
                                size
                        );

        /*
         * Service 결과를 외부 공개용 JSON 응답 DTO로 변환한다.
         */
        CertifiedRehabilitationInstitutionListResponse response =
                convertCertifiedRehabilitationResponse(
                        serviceResult
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Service 의료기관 한 건을 API 응답용 객체로 변환한다.
     *
     * @param source Service 계층의 의료기관 정보
     * @return API 응답용 의료기관 정보
     */
    private MedicalInstitutionListResponse.Institution
            convertInstitution(
                    MedicalInstitutionList.Institution source
            ) {

        /*
         * API로 공개할 필드만 명시적으로 선택한다.
         *
         * 외부 DTO를 그대로 반환하지 않으면 앞으로 외부 API에
         * 새로운 필드가 추가되어도 의도치 않게 사용자에게 노출되지 않는다.
         */
        return new MedicalInstitutionListResponse.Institution(
                source.hospitalName(),
                source.managingBranchCode(),
                source.managingBranchName(),
                source.address(),
                source.telephoneNumber(),
                source.faxNumber()
        );
    }
    /**
     * 약국 Service 결과를 API 응답 DTO로 변환한다.
     *
     * @param source Service의 약국 조회 결과
     * @return Vue에 반환할 약국 목록 응답
     */
    private PharmacyListResponse convertPharmacyResponse(
            PharmacyList source
    ) {
        /*
         * 약국 한 건씩 API 응답용 객체로 변환한다.
         */
        List<PharmacyListResponse.Pharmacy> pharmacies =
                source.pharmacies()
                        .stream()
                        .map(this::convertPharmacy)
                        .toList();

        return new PharmacyListResponse(
                source.page(),
                source.size(),
                source.totalCount(),
                pharmacies
        );
    }

    /**
     * Service의 약국 한 건을 API 응답용 약국으로 변환한다.
     *
     * @param source Service 계층의 약국 정보
     * @return API 응답용 약국 정보
     */
    private PharmacyListResponse.Pharmacy convertPharmacy(
            PharmacyList.Pharmacy source
    ) {
        /*
         * API로 공개하기로 결정한 필드만 명시적으로 복사한다.
         *
         * 외부 공공데이터의 필드가 추가되더라도 사용자에게
         * 의도하지 않은 정보가 자동 노출되지 않는다.
         */
        return new PharmacyListResponse.Pharmacy(
                source.pharmacyName(),
                source.managingBranchCode(),
                source.managingBranchName(),
                source.address(),
                source.telephoneNumber(),
                source.faxNumber()
        );
    }

    /**
     * 재활인증기관 Service 결과를 API 응답 DTO로 변환한다.
     *
     * @param source Service 계층의 재활인증기관 조회 결과
     * @return Vue에 반환할 재활인증기관 목록
     */
    private CertifiedRehabilitationInstitutionListResponse
            convertCertifiedRehabilitationResponse(
                    CertifiedRehabilitationInstitutionList source
            ) {

        /*
         * 기관 한 건씩 API 응답용 객체로 변환한다.
         */
        List<
                CertifiedRehabilitationInstitutionListResponse
                        .Institution
                > institutions =
                source.institutions()
                        .stream()
                        .map(
                                this::
                                convertCertifiedRehabilitationInstitution
                        )
                        .toList();

        return new CertifiedRehabilitationInstitutionListResponse(
                source.page(),
                source.size(),
                source.totalCount(),
                institutions
        );
    }

    /**
     * Service의 재활인증 의료기관 한 건을
     * 외부 공개용 API 응답으로 변환한다.
     *
     * @param source Service 계층의 재활인증 의료기관
     * @return API 응답용 재활인증 의료기관
     */
    private CertifiedRehabilitationInstitutionListResponse
            .Institution
            convertCertifiedRehabilitationInstitution(
                    CertifiedRehabilitationInstitutionList
                            .Institution source
            ) {

        /*
         * 공개하기로 결정한 필드만 명시적으로 복사한다.
         *
         * 외부 공공데이터에 새로운 필드가 추가되더라도
         * 검토되지 않은 정보가 자동 노출되는 것을 방지한다.
         */
        return new CertifiedRehabilitationInstitutionListResponse
                .Institution(
                        source.hospitalNumber(),
                        source.hospitalName(),
                        source.institutionTypeName(),
                        source.managingBranchCode(),
                        source.managingBranchName(),
                        source.address(),
                        source.telephoneNumber(),
                        source.faxNumber()
                );
    }
}
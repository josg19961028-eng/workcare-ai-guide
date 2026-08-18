package egovframework.workcare.medical.service;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import egovframework.workcare.medical.infrastructure.MedicalInstitutionPublicDataClient;
import egovframework.workcare.medical.infrastructure.dto.MedicalInstitutionApiResponse;
import egovframework.workcare.medical.infrastructure.dto.MedicalInstitutionApiResponse.Body;
import egovframework.workcare.medical.infrastructure.dto.MedicalInstitutionApiResponse.InstitutionItem;

/**
 * 산재지정 의료기관, 약국 및 재활인증 의료기관 조회 업무를 처리한다.
 *
 * <p>
 * 의료기관, 약국 및 재활인증 의료기관은 사용자 관점에서는
 * 서로 다른 정보이지만 공공데이터의 XML 계층과 검증 규칙은 동일하다.
 * </p>
 *
 * <p>
 * 따라서 공개 조회 메서드는 구분하고, 페이지 검증과
 * 외부 응답 검증은 private 메서드로 재사용한다.
 * </p>
 */
@Service
public class MedicalInstitutionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedicalInstitutionService.class);

    /**
     * 근로복지공단 Open API의 정상 결과 코드다.
     */
    private static final String SUCCESS_RESULT_CODE = "00";

    /**
     * 한 요청에서 허용하는 최대 조회 수다.
     *
     * <p>
     * 과도한 요청으로 인한 외부 API 호출량 증가와
     * 서버 자원 고갈 위험을 줄이기 위해 제한한다.
     * </p>
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 근로복지공단 산재 의료 공공데이터 호출 Client다.
     */
    private final MedicalInstitutionPublicDataClient publicDataClient;

    /**
     * 생성자 주입으로 Client를 전달받는다.
     *
     * @param publicDataClient 산재 의료 공공데이터 Client
     */
    public MedicalInstitutionService(
            MedicalInstitutionPublicDataClient publicDataClient
    ) {
        this.publicDataClient = publicDataClient;
    }

    /**
     * 산재지정 의료기관 목록을 조회한다.
     *
     * @param page 조회할 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 산재지정 의료기관 목록
     */
    public MedicalInstitutionList findMedicalInstitutions(
            int page,
            int size
    ) {
        /*
         * Controller 검증과 별도로 Service에서도 방어적으로 검사한다.
         */
        validatePagination(page, size);

        /*
         * 의료기관 API를 호출한 후 공통 응답 검증을 수행한다.
         */
        Body body = validateApiResponse(
                callMedicalInstitutionApi(page, size),
                "산재지정 의료기관"
        );

        /*
         * XML의 item 목록을 null에 안전한 목록으로 가져온다.
         */
        List<InstitutionItem> sourceItems = extractItems(body);

        /*
         * 외부 DTO를 의료기관 Service 모델로 변환한다.
         */
        List<MedicalInstitutionList.Institution> institutions =
                sourceItems.stream()
                        .filter(Objects::nonNull)
                        .map(this::convertMedicalInstitution)
                        .toList();

        return new MedicalInstitutionList(
                body.pageNo(),
                body.numOfRows(),
                body.totalCount(),
                institutions
        );
    }

    /**
     * 산재지정 약국 목록을 조회한다.
     *
     * @param page 조회할 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 산재지정 약국 목록
     */
    public PharmacyList findPharmacies(
            int page,
            int size
    ) {
        /*
         * 의료기관과 동일한 페이지 요청 규칙을 적용한다.
         */
        validatePagination(page, size);

        /*
         * 약국 API를 호출한 후 공통 응답 검증을 수행한다.
         */
        Body body = validateApiResponse(
                callPharmacyApi(page, size),
                "산재지정 약국"
        );

        /*
         * 약국 응답도 같은 XML DTO를 사용하므로
         * 기존 item 추출 메서드를 재사용할 수 있다.
         */
        List<InstitutionItem> sourceItems = extractItems(body);

        /*
         * 원본 hospitalNm 필드를 사용자 의미에 맞는
         * pharmacyName으로 변환한다.
         */
        List<PharmacyList.Pharmacy> pharmacies =
                sourceItems.stream()
                        .filter(Objects::nonNull)
                        .map(this::convertPharmacy)
                        .toList();

        return new PharmacyList(
                body.pageNo(),
                body.numOfRows(),
                body.totalCount(),
                pharmacies
        );
    }

    /**
     * 재활인증 의료기관 목록을 조회한다.
     *
     * <p>
     * 일반 의료기관과 달리 의료기관 식별번호와 종별명을
     * 함께 제공한다.
     * </p>
     *
     * @param page 조회할 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 재활인증 의료기관 목록
     */
    public CertifiedRehabilitationInstitutionList
            findCertifiedRehabilitationInstitutions(
                    int page,
                    int size
            ) {

        /*
         * Controller 검증을 우회하여 Service가 직접 호출되더라도
         * 잘못된 페이지 요청이 외부 API까지 전달되지 않게 한다.
         */
        validatePagination(page, size);

        /*
         * 재활인증 의료기관 API를 호출하고,
         * resultCode와 body를 기존 공통 메서드로 검증한다.
         */
        Body body = validateApiResponse(
                callCertifiedRehabilitationApi(page, size),
                "재활인증 의료기관"
        );

        /*
         * 검색 결과가 없어서 items 또는 item 태그가 누락된 경우에도
         * 빈 목록으로 안전하게 처리한다.
         */
        List<InstitutionItem> sourceItems =
                extractItems(body);

        /*
         * 외부 XML DTO를 재활인증 의료기관 Service 모델로 변환한다.
         */
        List<CertifiedRehabilitationInstitutionList.Institution>
                institutions =
                sourceItems.stream()
                        /*
                         * 외부 목록에 null 항목이 포함되어도
                         * 전체 응답 변환이 실패하지 않도록 제거한다.
                         */
                        .filter(Objects::nonNull)
                        .map(
                                this::
                                convertCertifiedRehabilitationInstitution
                        )
                        .toList();

        /*
         * 외부 API의 페이지 정보와 변환된 기관 목록을 묶어서 반환한다.
         */
        return new CertifiedRehabilitationInstitutionList(
                body.pageNo(),
                body.numOfRows(),
                body.totalCount(),
                institutions
        );
    }

    /**
     * 산재지정 의료기관 외부 API를 호출한다.
     *
     * @param page 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 외부 API 응답
     */
    private MedicalInstitutionApiResponse callMedicalInstitutionApi(
            int page,
            int size
    ) {
        try {
            return publicDataClient.fetchMedicalInstitutions(
                    page,
                    size
            );

        } catch (RuntimeException exception) {
            /*
             * 인증키, 요청 URL, XML 원문을 로그에 남기지 않고
             * 문제 분류에 필요한 예외 타입만 기록한다.
             */
            LOGGER.warn(
                    "산재지정 의료기관 외부 API 호출 실패. "
                            + "exceptionType={}",
                    exception.getClass().getName()
            );

            throw new MedicalPublicDataException(
                    "산재지정 의료기관 공공데이터를 조회하지 못했습니다.",
                    exception
            );
        }
    }

    /**
     * 산재지정 약국 외부 API를 호출한다.
     *
     * @param page 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 외부 API 응답
     */
    private MedicalInstitutionApiResponse callPharmacyApi(
            int page,
            int size
    ) {
        try {
            return publicDataClient.fetchPharmacies(
                    page,
                    size
            );

        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "산재지정 약국 외부 API 호출 실패. "
                            + "exceptionType={}",
                    exception.getClass().getName()
            );

            throw new MedicalPublicDataException(
                    "산재지정 약국 공공데이터를 조회하지 못했습니다.",
                    exception
            );
        }
    }

    /**
     * 재활인증 의료기관 외부 API를 호출한다.
     *
     * @param page 페이지 번호
     * @param size 한 페이지 결과 수
     * @return 근로복지공단 공공데이터 응답
     */
    private MedicalInstitutionApiResponse
            callCertifiedRehabilitationApi(
                    int page,
                    int size
            ) {

        try {
            /*
             * 실제 HTTP 통신과 XML 변환은 Client에 위임한다.
             */
            return publicDataClient
                    .fetchCertifiedRehabilitationInstitutions(
                            page,
                            size
                    );

        } catch (RuntimeException exception) {
            /*
             * 네트워크 연결 실패, 응답시간 초과, XML 변환 실패 등을
             * 우리 서비스에서 사용하는 공통 의료정보 예외로 변환한다.
             *
             * 인증키, 호출 URL 및 XML 원문은 로그에 남기지 않고
             * 예외의 타입만 기록한다.
             */
            LOGGER.warn(
                    "재활인증 의료기관 외부 API 호출 실패. "
                            + "exceptionType={}",
                    exception.getClass().getName()
            );

            throw new MedicalPublicDataException(
                    "재활인증 의료기관 공공데이터를 "
                            + "조회하지 못했습니다.",
                    exception
            );
        }
    }

    /**
     * 공공데이터 API 응답을 공통 검증하고 정상 body를 반환한다.
     *
     * <p>
     * 의료기관과 약국이 같은 검증 규칙을 사용하도록
     * 한 메서드에서 처리한다.
     * </p>
     *
     * @param apiResponse 외부 API 응답
     * @param dataName    오류 기록에 사용할 기능명
     * @return 검증이 완료된 응답 body
     */
    private Body validateApiResponse(
            MedicalInstitutionApiResponse apiResponse,
            String dataName
    ) {
        /*
         * 외부 시스템의 응답 구조는 항상 신뢰할 수 없으므로
         * 최상위 객체와 header의 존재 여부를 검사한다.
         */
        if (apiResponse == null || apiResponse.header() == null) {
            throw new MedicalPublicDataException(
                    dataName + " 공공데이터 응답 정보가 올바르지 않습니다."
            );
        }

        String resultCode = apiResponse.header().resultCode();

        /*
         * HTTP 200이어도 resultCode가 오류일 수 있으므로
         * 공공데이터의 업무 결과 코드를 따로 확인한다.
         */
        if (!SUCCESS_RESULT_CODE.equals(resultCode)) {
            /*
             * 외부 resultMsg는 로그에 기록하지 않는다.
             * 검토되지 않은 외부 문자열의 로그 삽입을 방지하기 위해
             * 결과 코드만 기록한다.
             */
            LOGGER.warn(
                    "{} API가 오류 결과를 반환했습니다. resultCode={}",
                    dataName,
                    resultCode
            );

            throw new MedicalPublicDataException(
                    dataName + " 공공데이터가 오류 결과를 반환했습니다."
            );
        }

        /*
         * 정상 코드인데 body가 없으면 정상 목록 응답으로 볼 수 없다.
         */
        if (apiResponse.body() == null) {
            throw new MedicalPublicDataException(
                    dataName + " 공공데이터 본문이 존재하지 않습니다."
            );
        }

        return apiResponse.body();
    }

    /**
     * XML body에서 목록을 null에 안전하게 추출한다.
     *
     * <p>
     * 조회 결과가 0건이면 공공데이터 API가 items 또는 item을
     * 생략할 수 있으므로 빈 목록으로 처리한다.
     * </p>
     *
     * @param body 외부 API 응답 본문
     * @return null이 아닌 item 목록
     */
    private List<InstitutionItem> extractItems(Body body) {
        if (body.items() == null || body.items().item() == null) {
            return List.of();
        }

        return body.items().item();
    }

    /**
     * 페이지 요청값을 검증한다.
     *
     * @param page 페이지 번호
     * @param size 한 페이지 결과 수
     */
    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 1) {
            throw new IllegalArgumentException(
                    "페이지 번호는 1 이상이어야 합니다."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "페이지 크기는 1 이상 100 이하이어야 합니다."
            );
        }
    }

    /**
     * 외부 의료기관 데이터를 의료기관 Service 모델로 변환한다.
     *
     * @param source 외부 API 의료기관 데이터
     * @return 산재지정 의료기관 정보
     */
    private MedicalInstitutionList.Institution
            convertMedicalInstitution(
                    InstitutionItem source
            ) {

        return new MedicalInstitutionList.Institution(
                source.hospitalNm(),
                source.gwanriJisaCd(),
                source.jisaNm(),
                source.addr(),
                source.tel(),
                source.faxTel()
        );
    }

    /**
     * 외부 약국 데이터를 약국 Service 모델로 변환한다.
     *
     * <p>
     * 외부 API에서는 약국명도 hospitalNm으로 제공하지만,
     * 우리 서비스에서는 의미가 명확한 pharmacyName으로 변환한다.
     * </p>
     *
     * @param source 외부 API 약국 데이터
     * @return 산재지정 약국 정보
     */
    private PharmacyList.Pharmacy convertPharmacy(
            InstitutionItem source
    ) {
        return new PharmacyList.Pharmacy(
                source.hospitalNm(),
                source.gwanriJisaCd(),
                source.jisaNm(),
                source.addr(),

                /*
                 * 약국 원본 데이터에는 tel 태그가 없는 항목이 있다.
                 * String 필드는 해당 경우 null로 안전하게 저장된다.
                 */
                source.tel(),

                source.faxTel()
        );
    }

    /**
     * 외부 재활인증 의료기관 데이터를 Service 모델로 변환한다.
     *
     * <p>
     * 외부 공공데이터 필드명을 우리 서비스에서 사용하는
     * 명확한 필드명으로 변경한다.
     * </p>
     *
     * @param source 외부 API의 재활인증 의료기관 원본 데이터
     * @return Service에서 사용할 재활인증 의료기관 정보
     */
    private CertifiedRehabilitationInstitutionList.Institution
            convertCertifiedRehabilitationInstitution(
                    InstitutionItem source
            ) {

        return new CertifiedRehabilitationInstitutionList.Institution(

                // hospitalNo: 의료기관 식별번호
                source.hospitalNo(),

                // hospitalNm: 의료기관명
                source.hospitalNm(),

                /*
                 * gtCdNm1: 응답 예시에서 요양병원 등
                 * 의료기관 종별값으로 제공되는 필드다.
                 */
                source.gtCdNm1(),

                // gwanriJisaCd: 관리 지사 코드
                source.gwanriJisaCd(),

                // jisaNm: 관리 지사명
                source.jisaNm(),

                // addr: 의료기관 주소
                source.addr(),

                // tel: 의료기관 전화번호
                source.tel(),

                // faxTel: 팩스번호
                source.faxTel()
        );
    }
}
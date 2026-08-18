package egovframework.workcare.precedent.service;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import egovframework.workcare.precedent.infrastructure.PrecedentPublicDataClient;
import egovframework.workcare.precedent.infrastructure.dto.PrecedentApiResponse;
import egovframework.workcare.precedent.infrastructure.dto.PrecedentApiResponse.Body;
import egovframework.workcare.precedent.infrastructure.dto.PrecedentApiResponse.Item;

/**
 * 산재보험 판례 검색 업무를 처리한다.
 */
@Service
public class PrecedentService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PrecedentService.class);

    private static final String SUCCESS_RESULT_CODE = "00";

    /*
     * 판결문 전문은 일반 목록보다 크기가 매우 크므로
     * 한 번에 최대 10건까지만 허용한다.
     */
    private static final int MAX_PAGE_SIZE = 10;

    /*
     * 비정상적으로 긴 검색조건이 외부 API와 로그 자원을
     * 불필요하게 소모하지 않도록 길이를 제한한다.
     */
    private static final int MAX_FILTER_LENGTH = 50;

    private final PrecedentPublicDataClient publicDataClient;

    public PrecedentService(
            PrecedentPublicDataClient publicDataClient
    ) {
        this.publicDataClient = publicDataClient;
    }

    /**
     * 판례 검색 화면에 필요한 세 종류의 선택값을 조회한다.
     */
    public PrecedentSearchOptions findSearchOptions() {
        Body resultTypeBody = callAndValidate(
                publicDataClient::fetchResultTypes,
                "판결결과 유형"
        );

        Body caseTypeBody = callAndValidate(
                publicDataClient::fetchCaseTypes,
                "사건유형"
        );

        Body accidentDiseaseBody = callAndValidate(
                publicDataClient::fetchAccidentDiseaseTypes,
                "사고·질병 구분"
        );

        return new PrecedentSearchOptions(
                extractCategories(resultTypeBody, Item::kinda),
                extractCategories(caseTypeBody, Item::kindb),
                extractCategories(
                        accidentDiseaseBody,
                        Item::kindc
                )
        );
    }

    /**
     * 검색조건에 맞는 판례 목록을 조회한다.
     */
    public PrecedentList searchPrecedents(
            PrecedentSearchCondition condition
    ) {
        if (condition == null) {
            throw new IllegalArgumentException(
                    "판례 검색조건이 필요합니다."
            );
        }

        validatePagination(condition.page(), condition.size());

        /*
         * 공백 제거와 길이 검증을 Service에서 수행한다.
         * Controller를 거치지 않고 호출되더라도 동일한 규칙이 적용된다.
         */
        String resultType = normalizeFilter(
                condition.resultType(),
                "판결결과"
        );

        String caseType = normalizeFilter(
                condition.caseType(),
                "사건유형"
        );

        String accidentDiseaseType = normalizeFilter(
                condition.accidentDiseaseType(),
                "사고·질병 구분"
        );

        Body body = callAndValidate(
                () -> publicDataClient.fetchPrecedents(
                        condition.page(),
                        condition.size(),
                        resultType,
                        caseType,
                        accidentDiseaseType
                ),
                "산재보험 판례"
        );

        List<PrecedentList.Precedent> precedents =
                extractItems(body).stream()
                        .filter(Objects::nonNull)
                        .map(this::convertPrecedent)
                        .toList();

        return new PrecedentList(
                body.pageNo(),
                body.numOfRows(),
                body.totalCount(),
                precedents
        );
    }

    /**
     * 검색조건에 해당하는 판례 개수를 조회한다.
     */
    public int countPrecedents(
            String resultTypeValue,
            String caseTypeValue,
            String accidentDiseaseTypeValue
    ) {
        String resultType = normalizeFilter(
                resultTypeValue,
                "판결결과"
        );

        String caseType = normalizeFilter(
                caseTypeValue,
                "사건유형"
        );

        String accidentDiseaseType = normalizeFilter(
                accidentDiseaseTypeValue,
                "사고·질병 구분"
        );

        Body body = callAndValidate(
                () -> publicDataClient.fetchPrecedentCount(
                        resultType,
                        caseType,
                        accidentDiseaseType
                ),
                "판례 검색 결과 개수"
        );

        /*
         * 개수 API는 item 한 건 안에 cnt를 반환한다.
         * 응답이 비어 있거나 cnt가 없으면 안전하게 0으로 처리한다.
         */
        return extractItems(body).stream()
                .filter(Objects::nonNull)
                .map(Item::cnt)
                .filter(Objects::nonNull)
                .findFirst()
                .map(count -> Math.max(count, 0))
                .orElse(0);
    }

    /**
     * 외부 호출과 공통 응답 검증을 한곳에서 처리한다.
     */
    private Body callAndValidate(
            Supplier<PrecedentApiResponse> apiCall,
            String dataName
    ) {
        try {
            return validateApiResponse(
                    apiCall.get(),
                    dataName
            );

        } catch (PrecedentPublicDataException exception) {
            // 이미 안전한 판례 업무 예외이면 그대로 전달한다.
            throw exception;

        } catch (RuntimeException exception) {
            /*
             * 검색조건, 인증키, 외부 URL, 판결문 전문은 로그에 남기지 않는다.
             * 문제 분류에 필요한 예외 타입만 기록한다.
             */
            LOGGER.warn(
                    "{} 공공데이터 호출 실패. exceptionType={}",
                    dataName,
                    exception.getClass().getName()
            );

            throw new PrecedentPublicDataException(
                    dataName + " 공공데이터를 조회하지 못했습니다.",
                    exception
            );
        }
    }

    /**
     * 공공데이터의 업무 결과 코드와 body를 검증한다.
     */
    private Body validateApiResponse(
            PrecedentApiResponse response,
            String dataName
    ) {
        if (response == null || response.header() == null) {
            throw new PrecedentPublicDataException(
                    dataName + " 응답 정보가 올바르지 않습니다."
            );
        }

        /*
         * HTTP 200만으로 성공을 판단하지 않고
         * 공공데이터 업무 결과 코드도 함께 검사한다.
         */
        if (!SUCCESS_RESULT_CODE.equals(
                response.header().resultCode()
        )) {
            throw new PrecedentPublicDataException(
                    dataName + " API가 오류 결과를 반환했습니다."
            );
        }

        if (response.body() == null) {
            throw new PrecedentPublicDataException(
                    dataName + " 응답 본문이 없습니다."
            );
        }

        return response.body();
    }

    /**
     * XML의 item 목록을 null에 안전하게 꺼낸다.
     */
    private List<Item> extractItems(Body body) {
        if (body.items() == null || body.items().item() == null) {
            return List.of();
        }

        return body.items().item();
    }

    /**
     * 종류별 API에서 필요한 문자열 필드만 추출한다.
     */
    private List<String> extractCategories(
            Body body,
            Function<Item, String> valueExtractor
    ) {
        return extractItems(body).stream()
                .filter(Objects::nonNull)
                .map(valueExtractor)
                .filter(StringUtils::hasText)
                .map(String::strip)
                .distinct()
                .toList();
    }

    /**
     * 외부 판례 DTO를 우리 Service 모델로 변환한다.
     */
    private PrecedentList.Precedent convertPrecedent(Item source) {
        return new PrecedentList.Precedent(
                source.accnum(),
                source.courtname(),
                source.kinda(),
                source.kindb(),
                source.kindc(),
                source.title(),
                source.noncontent()
        );
    }

    /**
     * 페이지 요청값을 방어적으로 검사한다.
     */
    private void validatePagination(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException(
                    "페이지 번호는 1 이상이어야 합니다."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "판례 페이지 크기는 1 이상 10 이하여야 합니다."
            );
        }
    }

    /**
     * 선택 검색조건을 정리하고 위험한 제어문자를 차단한다.
     */
    private String normalizeFilter(
            String value,
            String fieldName
    ) {
        /*
         * 선택하지 않은 조건은 null로 통일한다.
         */
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.length() > MAX_FILTER_LENGTH) {
            throw new IllegalArgumentException(
                    fieldName + " 검색조건이 너무 깁니다."
            );
        }

        /*
         * 줄바꿈 등 제어문자는 로그 변조나 비정상 요청에 이용될 수 있으므로
         * 외부 API로 전달하기 전에 차단한다.
         */
        boolean containsControlCharacter =
                normalized.chars()
                        .anyMatch(Character::isISOControl);

        if (containsControlCharacter) {
            throw new IllegalArgumentException(
                    fieldName + " 검색조건에 허용되지 않은 문자가 있습니다."
            );
        }

        return normalized;
    }
}
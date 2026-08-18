package egovframework.workcare.precedent.service;

import java.util.List;

/**
 * 판례 검색 화면의 선택 항목을 묶은 Service 결과 모델이다.
 */
public record PrecedentSearchOptions(
        List<String> resultTypes,
        List<String> caseTypes,
        List<String> accidentDiseaseTypes
) {
}
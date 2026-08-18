package egovframework.workcare.precedent.service;

/**
 * 판례 검색에 사용하는 Service 계층의 검색조건이다.
 *
 * @param page                페이지 번호
 * @param size                한 페이지 결과 수
 * @param resultType          판결결과
 * @param caseType            사건유형
 * @param accidentDiseaseType 사고·질병 구분
 */
public record PrecedentSearchCondition(
        int page,
        int size,
        String resultType,
        String caseType,
        String accidentDiseaseType
) {
}
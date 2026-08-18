package egovframework.workcare.precedent.web.dto;

/**
 * 선택한 검색조건에 해당하는 판례 개수 응답이다.
 */
public record PrecedentCountResponse(
        int count
) {
}
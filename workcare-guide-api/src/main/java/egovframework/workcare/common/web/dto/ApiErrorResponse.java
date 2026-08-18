package egovframework.workcare.common.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * WorkCare Guide의 모든 REST API가 공통으로 사용하는 오류 응답 DTO다.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<ErrorDetail> errors
) {

    /**
     * 요청값 하나에 대한 상세 검증 오류다.
     *
     * @param field 오류가 발생한 요청 필드명
     * @param message 해당 필드의 검증 메시지
     */
    public record ErrorDetail(
            String field,
            String message
    ) {
    }
}
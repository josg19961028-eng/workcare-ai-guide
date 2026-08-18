package egovframework.workcare.common.web.exception;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import egovframework.workcare.common.web.dto.ApiErrorResponse;
import egovframework.workcare.common.web.dto.ApiErrorResponse.ErrorDetail;
import egovframework.workcare.rehabilitation.service.RehabilitationPublicDataException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import egovframework.workcare.precedent.service.PrecedentPublicDataException;

import egovframework.workcare.medical.service.MedicalPublicDataException;

/**
 * REST API에서 발생한 예외를 공통 JSON 응답으로 변환한다.
 */
@RestControllerAdvice
public class GlobalApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

	/**
	 * URL 파라미터에 선언한 @Min, @Max 등의 검증 실패를 처리한다.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception,
			HttpServletRequest request) {

		/*
		 * 검증에 실패한 필드와 메시지를 사용자용 오류 정보로 변환한다.
		 */
		List<ErrorDetail> errors = exception.getConstraintViolations().stream().map(this::convertViolation).toList();

		return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값을 확인해주세요.", request, errors);
	}

	/**
	 * page=abc처럼 숫자 필드에 문자열이 전달된 경우를 처리한다.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {

		ErrorDetail error = new ErrorDetail(exception.getName(), "요청값의 형식이 올바르지 않습니다.");

		return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값을 확인해주세요.", request, List.of(error));
	}

	/**
	 * 향후 POST 요청 DTO에서 @Valid 검증이 실패하는 경우를 처리한다.
	 *
	 * <p>
	 * 현재 재활기관 조회는 URL 파라미터를 사용하지만, 이후 챗봇 질문이나 즐겨찾기 API의 JSON 요청 검증에서도 동일한 오류 형식을
	 * 재사용할 수 있다.
	 * </p>
	 *
	 * @param exception 요청 본문 검증 실패 예외
	 * @param request   현재 HTTP 요청
	 * @return HTTP 400 오류 응답
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpServletRequest request) {

		List<ErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage())).toList();

		return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "입력값을 확인해주세요.", request, errors);
	}

	/**
	 * Service의 방어적 입력 검증 실패를 처리한다.
	 *
	 * <p>
	 * 실제 예외 메시지를 그대로 반환하지 않고 사전에 정한 안전한 메시지만 사용자에게 제공한다.
	 * </p>
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값이 올바르지 않습니다.", request, List.of());
	}

	/**
	 * 근로복지공단 공공데이터가 비정상 응답을 반환한 경우를 처리한다.
	 *
	 * <p>
	 * 우리 서버는 요청을 받았지만 의존하고 있는 외부 서버에서 정상 데이터를 받지 못했으므로 502 Bad Gateway를 사용한다.
	 * </p>
	 */
	@ExceptionHandler(RehabilitationPublicDataException.class)
	public ResponseEntity<ApiErrorResponse> handleRehabilitationPublicData(RehabilitationPublicDataException exception,
			HttpServletRequest request) {

		/*
		 * 인증키나 XML 원문은 로그에 기록하지 않는다. 외부 API 오류 발생 사실과 요청 경로만 기록한다.
		 */
		LOGGER.warn("재활기관 공공데이터 처리에 실패했습니다. path={}", request.getRequestURI());

		return buildResponse(HttpStatus.BAD_GATEWAY, "PUBLIC_DATA_SERVICE_ERROR",
				"재활기관 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.", request, List.of());
	}

    /**
     * 산재 의료기관 공공데이터가 정상적으로 처리되지 않은 경우다.
     *
     * <p>
     * 우리 서버가 의존하는 근로복지공단 외부 API에서 정상 데이터를
     * 받지 못했으므로 HTTP 502 Bad Gateway를 반환한다.
     * </p>
     *
     * @param exception 의료기관 공공데이터 처리 예외
     * @param request   현재 HTTP 요청
     * @return HTTP 502 공통 JSON 오류 응답
     */
    @ExceptionHandler(MedicalPublicDataException.class)
    public ResponseEntity<ApiErrorResponse>
            handleMedicalPublicData(
                    MedicalPublicDataException exception,
                    HttpServletRequest request
            ) {

        /*
         * 인증키, 외부 호출 URL, XML 원문은 로그에 남기지 않는다.
         *
         * 오류 발생 기능과 내부 요청 경로만 기록하여
         * 문제 추적과 보안 사이의 균형을 유지한다.
         */
        LOGGER.warn(
                "산재 의료기관 공공데이터 처리에 실패했습니다. path={}",
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "PUBLIC_DATA_SERVICE_ERROR",
                "산재 의료기관 정보를 불러오지 못했습니다. "
                        + "잠시 후 다시 시도해주세요.",
                request,
                List.of()
        );
    }

    /**
     * 판례 공공데이터가 정상적으로 처리되지 않은 경우를 처리한다.
     */
    @ExceptionHandler(PrecedentPublicDataException.class)
    public ResponseEntity<ApiErrorResponse>
            handlePrecedentPublicData(
                    PrecedentPublicDataException exception,
                    HttpServletRequest request
            ) {

        /*
         * 판결문 전문, 검색조건, 인증키 및 외부 URL은 로그에 남기지 않는다.
         */
        LOGGER.warn(
                "산재보험 판례 공공데이터 처리에 실패했습니다. path={}",
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "PUBLIC_DATA_SERVICE_ERROR",
                "산재보험 판례 정보를 불러오지 못했습니다. "
                        + "잠시 후 다시 시도해주세요.",
                request,
                List.of()
        );
    }

	/**
	 * 위에서 예상하지 못한 예외를 최종적으로 처리한다.
	 *
	 * <p>
	 * 내부 예외 메시지를 응답에 넣으면 서버 구조나 라이브러리 정보가 노출될 수 있으므로 일반적인 메시지만 반환한다.
	 * </p>
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {

		/*
		 * 사용자 응답에는 예외 상세 내용을 넣지 않는다.
		 *
		 * 현재는 예외 클래스만 기록한다. 외부 요청 URL에 인증키가 포함될 수 있으므로 예외 메시지 전체를 무분별하게 기록하지 않는다.
		 */
		LOGGER.error("처리되지 않은 서버 오류가 발생했습니다. path={}, exceptionType={}", request.getRequestURI(),
				exception.getClass().getName());

		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 처리 중 오류가 발생했습니다.", request,
				List.of());
	}

	/**
	 * 검증 오류의 경로에서 실제 요청 필드명을 추출한다.
	 *
	 * <p>
	 * 예를 들어 findInstitutions.page가 전달되면 마지막 부분인 page만 반환한다.
	 * </p>
	 */
	private ErrorDetail convertViolation(ConstraintViolation<?> violation) {

		String propertyPath = violation.getPropertyPath().toString();

		int lastDotIndex = propertyPath.lastIndexOf('.');

		String field = lastDotIndex >= 0 ? propertyPath.substring(lastDotIndex + 1) : propertyPath;

		return new ErrorDetail(field, violation.getMessage());
	}

	/**
	 * 공통 오류 응답을 생성한다.
	 *
	 * @param status  HTTP 상태
	 * @param code    애플리케이션 오류 코드
	 * @param message 사용자용 오류 메시지
	 * @param request 현재 요청
	 * @param errors  필드별 검증 오류
	 * @return 완성된 오류 응답
	 */
	private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code, String message,
			HttpServletRequest request, List<ErrorDetail> errors) {

		ApiErrorResponse response = new ApiErrorResponse(Instant.now(), status.value(), code, message,

				/*
				 * Query String은 인증 정보나 검색어를 포함할 수 있으므로 오류 응답에는 순수 API 경로만 넣는다.
				 */
				request.getRequestURI(), errors);

		return ResponseEntity.status(status).body(response);
	}
}
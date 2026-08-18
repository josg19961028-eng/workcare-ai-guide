package egovframework.workcare.medical.service;

/**
 * 산재 의료기관 공공데이터를 정상적으로 처리할 수 없을 때 발생한다.
 *
 * <p>
 * 외부 API 통신 실패, XML 변환 실패, 비정상 결과 코드 등
 * 사용자의 요청 문제가 아닌 외부 시스템 문제를 표현한다.
 * </p>
 */
public class MedicalPublicDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 안전한 업무 오류 메시지만 전달하는 생성자다.
     *
     * @param message 내부에서 사용할 오류 메시지
     */
    public MedicalPublicDataException(String message) {
        super(message);
    }

    /**
     * 원인이 된 예외를 보존하는 생성자다.
     *
     * <p>
     * 원인 예외는 서버 내부 문제 분석에 사용하지만
     * 사용자 응답에는 그대로 노출하지 않는다.
     * </p>
     *
     * @param message 안전한 업무 오류 메시지
     * @param cause   원인이 된 하위 예외
     */
    public MedicalPublicDataException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
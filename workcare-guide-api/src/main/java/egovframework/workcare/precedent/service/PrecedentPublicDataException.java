package egovframework.workcare.precedent.service;

/**
 * 판례 공공데이터를 정상적으로 처리하지 못했을 때 발생한다.
 */
public class PrecedentPublicDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PrecedentPublicDataException(String message) {
        super(message);
    }

    /**
     * 원인 예외는 서버 내부 분석에 사용하지만 사용자에게 노출하지 않는다.
     */
    public PrecedentPublicDataException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
package egovframework.workcare.rehabilitation.service;

/**
 * 근로복지공단 공공데이터 API가 정상적인 결과를 반환하지 않았을 때 발생하는 예외다.
 */
public class RehabilitationPublicDataException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RehabilitationPublicDataException(String message) {
		super(message);
	}
}
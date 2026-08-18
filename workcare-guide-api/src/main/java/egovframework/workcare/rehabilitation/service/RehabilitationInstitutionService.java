package egovframework.workcare.rehabilitation.service;

/**
 * 재활기관 조회 업무 기능을 정의하는 Service 인터페이스다.
 */
public interface RehabilitationInstitutionService {
    RehabilitationInstitutionList findInstitutions(int page, int size);
}
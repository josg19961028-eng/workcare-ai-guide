package egovframework.workcare.precedent.infrastructure.persistence;

/**
 * 판례 한 건을 Oracle DB에 저장할 때 사용하는 입력 데이터다.
 *
 * <p>외부 공공데이터 DTO를 Mapper에 바로 전달하지 않고
 * 별도의 저장용 객체로 변환한다. 이렇게 분리하면 외부 API의
 * 필드명이 변경되어도 DB 저장 구조에 미치는 영향을 줄일 수 있다.</p>
 *
 * @param caseNumber          사건번호
 * @param courtName           판결 법원명
 * @param resultType          판결결과
 * @param caseType            사건유형
 * @param accidentDiseaseType 사고·질병 구분
 * @param title               사건명
 * @param content             판결문 전문
 * @param contentHash         판결문 전문의 SHA-256 해시
 */
public record PrecedentSaveParameter(
        String caseNumber,
        String courtName,
        String resultType,
        String caseType,
        String accidentDiseaseType,
        String title,
        String content,
        String contentHash
) {
}
package egovframework.workcare.precedent.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;

/**
 * 판례 데이터를 Oracle DB에 저장하고 조회하는 MyBatis Mapper다.
 *
 * <p>Mapper 인터페이스에는 SQL을 작성하지 않고
 * 실행할 DB 작업의 이름과 입출력 형식만 선언한다.
 * 실제 SQL은 PrecedentMapper.xml에서 관리한다.</p>
 */
@Mapper
public interface PrecedentMapper {

    /**
     * 판례 한 건을 사건번호와 법원명을 기준으로 저장한다.
     *
     * <p>같은 판례가 이미 존재하면 수정하고,
     * 존재하지 않으면 새로 등록한다.</p>
     *
     * @param parameter 저장할 판례 데이터
     * @return INSERT 또는 UPDATE 영향을 받은 행 개수
     */
    int mergePrecedent(PrecedentSaveParameter parameter);
}
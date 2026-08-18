package egovframework.workcare.common.infrastructure;

import org.apache.ibatis.annotations.Mapper;

/**
 * 애플리케이션에서 Oracle DB 접속이 가능한지 확인하는 Mapper다.
 *
 * <p>이 Mapper는 실제 업무 데이터를 처리하지 않고,
 * 개발환경에서 DataSource와 MyBatis 설정이 정상적으로 연결됐는지
 * 확인하기 위한 진단 용도로만 사용한다.</p>
 */
@Mapper
public interface DatabaseProbeMapper {

    /**
     * Oracle의 DUAL 테이블에서 숫자 1을 조회한다.
     *
     * <p>이 메서드가 1을 반환하면 다음 설정이 모두 정상이라는 뜻이다.</p>
     *
     * <ul>
     *   <li>Oracle 컨테이너와 Listener</li>
     *   <li>FREEPDB1 서비스</li>
     *   <li>WORKCARE_APP 계정과 비밀번호</li>
     *   <li>Oracle JDBC 드라이버</li>
     *   <li>HikariCP 커넥션 풀</li>
     *   <li>MyBatis Mapper XML 연결</li>
     * </ul>
     *
     * @return Oracle이 반환한 숫자 1
     */
    int selectConnectionCheck();
}
package egovframework.workcare.common.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import oracle.jdbc.OracleType;

/**
 * Java의 float[]와 Oracle의 VECTOR(..., FLOAT32)를
 * 서로 변환하는 MyBatis TypeHandler다.
 *
 * <p>JDBC 표준에는 VECTOR 타입이 정의되어 있지 않으므로
 * Oracle JDBC가 제공하는 OracleType.VECTOR_FLOAT32를
 * 명시적으로 사용한다.</p>
 */
@MappedTypes(float[].class)
public class OracleFloat32VectorTypeHandler
        extends BaseTypeHandler<float[]> {

    /**
     * Java float[]를 Oracle FLOAT32 VECTOR 매개변수로 바인딩한다.
     *
     * @param statement PreparedStatement
     * @param index     SQL 매개변수 위치
     * @param parameter 저장할 벡터
     * @param jdbcType  MyBatis JDBC 타입
     */
    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            float[] parameter,
            JdbcType jdbcType
    ) throws SQLException {

        /*
         * setObject(index, parameter)만 호출하면 JDBC가 해당 배열을
         * VECTOR로 변환해야 하는지 판단하지 못할 수 있다.
         *
         * OracleType.VECTOR_FLOAT32를 명시하여
         * float[]를 FLOAT32 벡터로 손실 없이 저장한다.
         */
        statement.setObject(
                index,
                parameter,
                OracleType.VECTOR_FLOAT32
        );
    }

    /**
     * 컬럼명으로 Oracle VECTOR를 조회한다.
     */
    @Override
    public float[] getNullableResult(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {

        return resultSet.getObject(
                columnName,
                float[].class
        );
    }

    /**
     * 컬럼 순서로 Oracle VECTOR를 조회한다.
     */
    @Override
    public float[] getNullableResult(
            ResultSet resultSet,
            int columnIndex
    ) throws SQLException {

        return resultSet.getObject(
                columnIndex,
                float[].class
        );
    }

    /**
     * 프로시저 또는 함수 결과에서 Oracle VECTOR를 조회한다.
     */
    @Override
    public float[] getNullableResult(
            CallableStatement statement,
            int columnIndex
    ) throws SQLException {

        return statement.getObject(
                columnIndex,
                float[].class
        );
    }
}
package egovframework.workcare.precedent.infrastructure.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 임베딩 대상 판례를 조회하고 판결문 청크 및 벡터를
 * Oracle에 저장하는 Mapper다.
 */
@Mapper
public interface PrecedentChunkMapper {

    /**
     * 아직 청크가 생성되지 않은 판례를 제한된 수만큼 조회한다.
     *
     * @param limit 한 번에 조회할 최대 판례 수
     * @return 임베딩 대상 판례
     */
    List<PrecedentForEmbedding>
            selectPrecedentsWithoutChunks(
                    @Param("limit") int limit
            );

    /**
     * 해당 판례의 기존 청크를 제거한다.
     *
     * <p>판결문이 변경된 경우 이전 벡터가 남는 것을 방지하고
     * 새로운 청크 전체로 교체하기 위해 사용한다.</p>
     *
     * @param precedentId 판례 ID
     * @return 삭제한 청크 수
     */
    int deleteChunksByPrecedentId(
            @Param("precedentId") long precedentId
    );

    /**
     * 임베딩을 완료한 청크 한 건을 저장한다.
     *
     * @param parameter 저장할 청크 데이터
     * @return 저장한 행 수
     */
    int insertCompletedChunk(
            PrecedentChunkSaveParameter parameter
    );

    /**
     * 질문 벡터와 의미적으로 가까운 판례를 조회한다.
     *
     * <p>한 판례에 여러 청크가 있어도 가장 가까운 청크 한 건만
     * 선택하여 동일 판례가 검색 결과에 반복되는 것을 방지한다.</p>
     *
     * @param queryEmbedding 사용자 질문의 1024차원 벡터
     * @param limit          반환할 최대 판례 수
     * @return 의미적으로 가까운 판례 목록
     */
    List<PrecedentVectorSearchRow> selectSimilarPrecedents(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("limit") int limit
    );
}
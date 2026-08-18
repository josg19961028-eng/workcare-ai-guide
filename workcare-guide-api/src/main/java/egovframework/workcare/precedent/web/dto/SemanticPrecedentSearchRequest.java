package egovframework.workcare.precedent.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 의미 기반 판례 검색 요청이다.
 *
 * @param question 사용자의 사고 또는 질병 설명
 */
public record SemanticPrecedentSearchRequest(

        @NotBlank(
                message = "검색할 사고 또는 질병 내용을 입력해주세요."
        )
        @Size(
                max = 500,
                message = "검색 질문은 500자 이하여야 합니다."
        )
        String question
) {
}
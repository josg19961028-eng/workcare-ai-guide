package egovframework.workcare.chat.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 판례 챗봇 질문 요청이다.
 *
 * @param message 사용자가 입력한 사고 또는 질병 내용
 */
public record PrecedentChatRequest(

        @NotBlank(
                message = "상담 내용을 입력해주세요."
        )
        @Size(
                max = 500,
                message = "상담 내용은 500자 이하여야 합니다."
        )
        String message
) {
}
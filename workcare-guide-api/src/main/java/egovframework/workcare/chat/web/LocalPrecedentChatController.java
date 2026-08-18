package egovframework.workcare.chat.web;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.chat.service.PrecedentChatResult;
import egovframework.workcare.chat.service.PrecedentChatService;
import egovframework.workcare.chat.web.dto.PrecedentChatRequest;
import egovframework.workcare.chat.web.dto.PrecedentChatResponse;
import jakarta.validation.Valid;

/**
 * 개발환경에서 판례 검색형 챗봇 API를 제공한다.
 */
@Profile("local")
@RestController
@RequestMapping("/api/local/chat/precedents")
public class LocalPrecedentChatController {

    private static final String DISCLAIMER =
            "이 서비스는 공개 판례 검색을 돕는 참고 도구이며, "
            + "산재 인정 가능성이나 법률적 결론을 제공하지 않습니다.";

    private final PrecedentChatService chatService;

    public LocalPrecedentChatController(
            PrecedentChatService chatService
    ) {
        this.chatService = chatService;
    }

    /**
     * 사용자의 사고 설명을 받아 관련 판례를 안내한다.
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PrecedentChatResponse> chat(

            @Valid
            @RequestBody
            PrecedentChatRequest request
    ) {
        PrecedentChatResult result =
                chatService.ask(request.message());

        List<PrecedentChatResponse.Precedent>
                responsePrecedents =
                        result.precedents()
                                .stream()
                                .map(this::convertPrecedent)
                                .toList();

        PrecedentChatResponse response =
                new PrecedentChatResponse(
                        result.answer(),
                        DISCLAIMER,
                        responsePrecedents
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Service 결과를 외부 API 응답 DTO로 변환한다.
     */
    private PrecedentChatResponse.Precedent convertPrecedent(
            PrecedentChatResult.Precedent source
    ) {
        return new PrecedentChatResponse.Precedent(
                source.caseNumber(),
                source.courtName(),
                source.resultType(),
                source.caseType(),
                source.accidentDiseaseType(),
                source.title(),
                source.matchedExcerpt(),
                source.similarityScore(),
                source.similarityPercent()
        );
    }
}
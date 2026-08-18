package egovframework.workcare.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import egovframework.workcare.precedent.service.SemanticPrecedentSearchResult;
import egovframework.workcare.precedent.service.SemanticPrecedentSearchService;

/**
 * 벡터 검색 결과를 사용자 안내형 챗봇 응답으로 변환한다.
 *
 * <p>생성형 AI가 임의로 답변을 생성하는 방식이 아니라
 * Oracle에서 검색한 판례의 사건번호, 유형 및 판결결과를
 * 정해진 문장 형식에 넣어 답변한다.</p>
 */
@Service
public class PrecedentChatService {

    /*
     * 유사도가 지나치게 낮은 판례는 관련 결과에서 제외한다.
     *
     * 현재는 소수의 실습 데이터로 설정한 초기 기준이며,
     * 판례 데이터가 충분히 쌓이면 평가 데이터를 이용해 조정해야 한다.
     */
    private static final double MIN_SIMILARITY_SCORE = 0.35;

    /*
     * 판결문 전체를 응답으로 반환하지 않고
     * 검색 근거가 되는 앞부분만 최대 400자로 제한한다.
     *
     * 과도하게 큰 응답과 불필요한 판결문 노출을 방지한다.
     */
    private static final int MAX_EXCERPT_LENGTH = 400;

    private final SemanticPrecedentSearchService searchService;

    public PrecedentChatService(
            SemanticPrecedentSearchService searchService
    ) {
        this.searchService = searchService;
    }

    /**
     * 사용자의 사고 설명을 바탕으로 관련 판례를 안내한다.
     *
     * @param question 사용자의 사고 또는 질병 설명
     * @return 챗봇 답변과 관련 판례
     */
    public PrecedentChatResult ask(String question) {
        /*
         * 기존 의미 검색 Service를 재사용한다.
         *
         * 질문 검증, Ollama 임베딩 및 Oracle VECTOR 검색을
         * 챗봇에서 중복 구현하지 않는다.
         */
        SemanticPrecedentSearchResult searchResult =
                searchService.search(question);

        /*
         * 최소 유사도 기준을 통과한 판례만 사용자에게 제공한다.
         */
        List<SemanticPrecedentSearchResult.Match>
                relevantMatches =
                        searchResult.matches()
                                .stream()
                                .filter(match ->
                                        match.similarityScore()
                                        >= MIN_SIMILARITY_SCORE
                                )
                                .toList();

        /*
         * 관련성이 충분한 판례가 없으면 억지로 답을 만들지 않는다.
         * 이는 잘못된 판례 안내를 줄이기 위한 안전장치다.
         */
        if (relevantMatches.isEmpty()) {
            return new PrecedentChatResult(
                    createNoResultAnswer(),
                    List.of()
            );
        }

        List<PrecedentChatResult.Precedent> precedents =
                relevantMatches.stream()
                        .map(this::convertPrecedent)
                        .toList();

        String answer =
                createAnswer(
                        relevantMatches.get(0),
                        precedents.size()
                );

        return new PrecedentChatResult(
                answer,
                precedents
        );
    }

    /**
     * 가장 관련성이 높은 판례를 기준으로 안내 문장을 만든다.
     */
    private String createAnswer(
            SemanticPrecedentSearchResult.Match topMatch,
            int resultCount
    ) {
        String caseNumber =
                defaultText(
                        topMatch.caseNumber(),
                        "사건번호 정보 없음"
                );

        String courtName =
                defaultText(
                        topMatch.courtName(),
                        "법원 정보 없음"
                );

        String title =
                defaultText(
                        topMatch.title(),
                        "사건명 정보 없음"
                );

        String caseType =
                defaultText(
                        topMatch.caseType(),
                        "사건유형 정보 없음"
                );

        String resultType =
                defaultText(
                        topMatch.resultType(),
                        "판결결과 정보 없음"
                );

        /*
         * 외부 텍스트를 HTML로 만들지 않고 일반 문자열로 반환한다.
         * Vue에서도 v-html이 아닌 {{ }} 보간법으로 출력해야
         * 저장형 XSS 위험을 줄일 수 있다.
         */
        return String.format(
                """
                입력하신 내용과 의미적으로 가까운 판례 %d건을 찾았습니다.

                가장 관련성이 높은 판례는 %s %s의 '%s' 사건입니다.
                이 판례의 사건유형은 %s이고 판결결과는 %s입니다.

                아래 관련 판례와 검색 근거 문장을 확인해 주세요.
                """,
                resultCount,
                courtName,
                caseNumber,
                title,
                caseType,
                resultType
        ).strip();
    }

    /**
     * 관련성이 충분한 판례가 없을 때 반환할 안내 문장이다.
     */
    private String createNoResultAnswer() {
        return """
                입력하신 내용과 충분히 유사한 판례를 찾지 못했습니다.

                사고 장소, 수행하던 업무, 다친 부위와 사고 원인을
                조금 더 구체적으로 입력해 주세요.
                """.strip();
    }

    /**
     * 의미 검색 결과를 챗봇 판례 카드 데이터로 변환한다.
     */
    private PrecedentChatResult.Precedent convertPrecedent(
            SemanticPrecedentSearchResult.Match source
    ) {
        double similarityScore =
                source.similarityScore();

        int similarityPercent =
                (int) Math.round(
                        similarityScore * 100
                );

        return new PrecedentChatResult.Precedent(
                source.caseNumber(),
                source.courtName(),
                source.resultType(),
                source.caseType(),
                source.accidentDiseaseType(),
                source.title(),
                createExcerpt(source.matchedChunk()),
                similarityScore,
                similarityPercent
        );
    }

    /**
     * 판결문 청크를 카드에 표시할 짧은 근거 문장으로 변환한다.
     */
    private String createExcerpt(String chunkText) {
        if (!StringUtils.hasText(chunkText)) {
            return "검색 근거 문장이 없습니다.";
        }

        /*
         * 여러 줄과 연속 공백을 한 칸으로 정리하여
         * 판례 카드에서 읽기 쉽게 표시한다.
         */
        String normalized =
                chunkText
                        .replaceAll("\\s+", " ")
                        .strip();

        if (normalized.length() <= MAX_EXCERPT_LENGTH) {
            return normalized;
        }

        return normalized.substring(
                0,
                MAX_EXCERPT_LENGTH
        ) + "...";
    }

    /**
     * null 또는 공백 문자열을 안전한 기본 문구로 변환한다.
     */
    private String defaultText(
            String value,
            String defaultValue
    ) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }

        return value.strip();
    }
}
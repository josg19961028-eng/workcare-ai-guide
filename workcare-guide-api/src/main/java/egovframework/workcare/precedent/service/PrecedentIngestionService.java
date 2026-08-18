package egovframework.workcare.precedent.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import egovframework.workcare.precedent.infrastructure.persistence.PrecedentMapper;
import egovframework.workcare.precedent.infrastructure.persistence.PrecedentSaveParameter;

/**
 * 근로복지공단 공공데이터에서 판례를 조회하여
 * Oracle DB에 저장하는 수집 업무를 처리한다.
 */
@Service
public class PrecedentIngestionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PrecedentIngestionService.class);

    private final PrecedentService precedentService;
    private final PrecedentMapper precedentMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 생성자 주입을 통해 필요한 협력 객체를 전달받는다.
     *
     * @param precedentService    공공데이터 판례 조회 Service
     * @param precedentMapper     Oracle 판례 저장 Mapper
     * @param transactionTemplate DB 트랜잭션 실행 도구
     */
    public PrecedentIngestionService(
            PrecedentService precedentService,
            PrecedentMapper precedentMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.precedentService = precedentService;
        this.precedentMapper = precedentMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 공공데이터 판례 한 페이지를 조회하여 Oracle에 저장한다.
     *
     * <p>외부 API 호출은 네트워크 지연이나 장애가 발생할 수 있으므로
     * DB 트랜잭션을 시작하기 전에 먼저 실행한다.</p>
     *
     * @param page 조회할 페이지 번호
     * @param size 페이지당 판례 수
     * @return 판례 수집 및 저장 결과
     */
    public PrecedentIngestionResult ingestPage(
            int page,
            int size
    ) {
        /*
         * 1단계: 근로복지공단 공공데이터를 조회한다.
         *
         * 이 시점에는 아직 DB 트랜잭션을 시작하지 않는다.
         * 외부 API 응답을 기다리는 동안 DB 연결을 점유하지 않기 위해서다.
         */
        PrecedentList publicDataResult =
                precedentService.searchPrecedents(
                        new PrecedentSearchCondition(
                                page,
                                size,
                                null,
                                null,
                                null
                        )
                );

        /*
         * 2단계: 외부 데이터를 DB 저장용 객체로 변환한다.
         */
        List<PrecedentSaveParameter> saveParameters =
                convertSaveParameters(
                        publicDataResult.precedents()
                );

        int fetchedCount =
                publicDataResult.precedents().size();

        int skippedCount =
                fetchedCount - saveParameters.size();

        /*
         * 3단계: 실제 Oracle 저장 부분만 하나의 트랜잭션으로 실행한다.
         *
         * 중간 판례 저장 중 오류가 발생하면 이 페이지에서 실행한
         * 모든 INSERT와 UPDATE가 함께 롤백된다.
         */
        Integer affectedRows =
                transactionTemplate.execute(
                        transactionStatus -> {
                            int totalAffectedRows = 0;

                            for (PrecedentSaveParameter parameter
                                    : saveParameters) {

                                totalAffectedRows +=
                                        precedentMapper.mergePrecedent(
                                                parameter
                                        );
                            }

                            return totalAffectedRows;
                        }
                );

        /*
         * TransactionTemplate의 반환값이 예외적으로 null이면
         * 안전하게 0으로 처리한다.
         */
        int storedCount =
                affectedRows == null ? 0 : affectedRows;

        return new PrecedentIngestionResult(
                page,
                fetchedCount,
                storedCount,
                skippedCount
        );
    }

    /**
     * 공공데이터 판례 목록을 DB 저장용 객체 목록으로 변환한다.
     */
    private List<PrecedentSaveParameter> convertSaveParameters(
            List<PrecedentList.Precedent> precedents
    ) {
        List<PrecedentSaveParameter> parameters =
                new ArrayList<>();

        if (precedents == null) {
            return parameters;
        }

        for (PrecedentList.Precedent precedent : precedents) {

            if (precedent == null) {
                continue;
            }

            String caseNumber =
                    normalize(precedent.caseNumber());

            String content =
                    normalize(precedent.content());

            /*
             * 사건번호는 판례를 식별하는 기준이고,
             * 판결문은 검색 및 임베딩에 필요한 핵심 데이터다.
             *
             * 두 값 중 하나라도 없으면 저장하지 않는다.
             */
            if (!StringUtils.hasText(caseNumber)
                    || !StringUtils.hasText(content)) {

                /*
                 * 판결문이나 사건번호 자체를 로그에 출력하지 않는다.
                 * 판결문에 개인정보 또는 민감정보가 포함될 수 있기 때문이다.
                 */
                LOGGER.warn(
                        "필수값이 없는 판례 한 건을 저장 대상에서 제외했습니다."
                );

                continue;
            }

            /*
             * 정리된 판결문을 기준으로 SHA-256 해시를 만든다.
             * 동일한 판결문은 항상 동일한 64자리 해시를 갖는다.
             */
            String contentHash =
                    createSha256Hash(content);

            parameters.add(
                    new PrecedentSaveParameter(
                            caseNumber,
                            normalize(precedent.courtName()),
                            normalize(precedent.resultType()),
                            normalize(precedent.caseType()),
                            normalize(
                                    precedent.accidentDiseaseType()
                            ),
                            normalize(precedent.title()),
                            content,
                            contentHash
                    )
            );
        }

        return parameters;
    }

    /**
     * 문자열의 앞뒤 공백을 제거한다.
     *
     * <p>null이거나 공백뿐인 값은 null로 통일하여
     * DB에 의미 없는 빈 문자열이 저장되는 것을 방지한다.</p>
     */
    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.strip();
    }

    /**
     * 판결문에 대한 SHA-256 해시값을 생성한다.
     *
     * @param content 해시를 생성할 판결문
     * @return 영문 소문자 64자리 SHA-256 해시
     */
    private String createSha256Hash(String content) {
        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes =
                    messageDigest.digest(
                            content.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            /*
             * Java 17 표준 실행환경에는 SHA-256이 반드시 제공된다.
             * 만약 사용할 수 없다면 정상적인 저장을 계속할 수 없는
             * 서버 설정 오류이므로 업무를 중단한다.
             */
            throw new IllegalStateException(
                    "SHA-256 해시 기능을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}
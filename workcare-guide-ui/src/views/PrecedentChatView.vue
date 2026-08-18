<script setup>
import {
  computed,
  nextTick,
  ref,
} from 'vue'

import { askPrecedentChat } from '@/api/chatApi'

/*
 * 사용자가 입력할 질문이다.
 */
const question = ref('')

/*
 * 사용자 입력값 검증 오류 또는 API 호출 오류 메시지다.
 */
const errorMessage = ref('')

/*
 * 챗봇 API를 호출하고 있는지 나타낸다.
 *
 * 요청 중에는 버튼과 입력창을 비활성화하여
 * 같은 질문이 중복 전송되는 것을 방지한다.
 *
 * 다만 프론트엔드 비활성화는 사용자 편의를 위한 장치다.
 * 실제 무차별 요청 방어는 서버의 Rate Limit 등으로 구현해야 한다.
 */
const isLoading = ref(false)

/*
 * 질문 입력창 DOM을 참조한다.
 *
 * API 요청이 끝난 후 다시 입력창에 포커스를 주기 위해 사용한다.
 */
const questionInput = ref(null)

/*
 * 채팅 영역의 마지막 위치를 참조한다.
 *
 * 새로운 메시지가 추가되면 화면을 아래쪽으로 이동시키는 데 사용한다.
 */
const chatEnd = ref(null)

/*
 * 메시지마다 고유한 번호를 부여한다.
 *
 * Vue의 v-for에서 안정적인 key 값으로 사용한다.
 */
let messageSequence = 0

/**
 * 새로운 메시지 식별번호를 만든다.
 *
 * @returns {number} 메시지 식별번호
 */
function createMessageId() {
  messageSequence += 1

  return messageSequence
}

/*
 * 현재 화면에 표시할 채팅 메시지 목록이다.
 *
 * 대화를 브라우저 localStorage에 저장하지 않는다.
 * 사용자가 입력한 사고·질병 내용에는 민감정보가 포함될 수 있기 때문이다.
 */
const chatMessages = ref([
  {
    id: createMessageId(),
    role: 'assistant',
    text:
      '사고 당시 수행하던 업무, 사고 원인, 다친 부위를 구체적으로 입력해 주세요.\n\n입력하신 내용과 의미적으로 가까운 산재보험 판례를 찾아드립니다.',
    disclaimer: '',
    precedents: [],
  },
])

/*
 * 질문의 최대 입력 길이다.
 *
 * 백엔드 PrecedentChatRequest의 @Size(max = 500)과
 * 동일한 값으로 맞춘다.
 */
const MAX_QUESTION_LENGTH = 500

/*
 * 사용자가 추가로 입력할 수 있는 글자 수다.
 */
const remainingLength = computed(() => {
  return MAX_QUESTION_LENGTH - question.value.length
})

/*
 * 질문을 전송할 수 있는 상태인지 계산한다.
 */
const canSubmit = computed(() => {
  return (
    !isLoading.value &&
    question.value.trim().length > 0 &&
    question.value.length <= MAX_QUESTION_LENGTH
  )
})

/**
 * 채팅 화면을 마지막 메시지 위치로 이동한다.
 */
async function scrollToChatEnd() {
  /*
   * Vue가 새 메시지를 실제 HTML에 반영할 때까지 기다린다.
   */
  await nextTick()

  chatEnd.value?.scrollIntoView({
    behavior: 'smooth',
    block: 'end',
  })
}

/**
 * 관련 판례 유사도를 화면 표시용 숫자로 변환한다.
 *
 * 서버 응답값이 비정상적이더라도 0~100 범위를 벗어나지 않게 한다.
 *
 * @param {unknown} value 서버가 반환한 유사도 백분율
 * @returns {number} 0~100 사이의 정수
 */
function normalizeSimilarityPercent(value) {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue)) {
    return 0
  }

  return Math.min(
    100,
    Math.max(0, Math.round(numericValue)),
  )
}

/**
 * API 응답의 관련 판례 목록을 안전하게 변환한다.
 *
 * @param {unknown} value API 응답의 precedents
 * @returns {Array<object>} 화면에 표시할 판례 목록
 */
function normalizePrecedents(value) {
  if (!Array.isArray(value)) {
    return []
  }

  return value.map((precedent) => {
    return {
      caseNumber:
        precedent.caseNumber || '사건번호 미제공',

      courtName:
        precedent.courtName || '법원 정보 미제공',

      resultType:
        precedent.resultType || '판결결과 미제공',

      caseType:
        precedent.caseType || '사건유형 미제공',

      accidentDiseaseType:
        precedent.accidentDiseaseType ||
        '사고·질병 구분 미제공',

      title:
        precedent.title || '사건명 미제공',

      matchedExcerpt:
        precedent.matchedExcerpt ||
        '검색 근거 문장이 제공되지 않았습니다.',

      similarityPercent:
        normalizeSimilarityPercent(
          precedent.similarityPercent,
        ),
    }
  })
}

/**
 * 사용자가 작성한 질문을 챗봇 API로 전송한다.
 */
async function submitQuestion() {
  /*
   * API 요청 중 다시 호출되는 것을 방지한다.
   */
  if (isLoading.value) {
    return
  }

  errorMessage.value = ''

  /*
   * 질문 앞뒤의 불필요한 공백을 제거한다.
   */
  const normalizedQuestion = question.value.trim()

  if (!normalizedQuestion) {
    errorMessage.value =
      '사고 또는 질병 내용을 입력해 주세요.'

    questionInput.value?.focus()

    return
  }

  if (
    normalizedQuestion.length >
    MAX_QUESTION_LENGTH
  ) {
    errorMessage.value =
      `질문은 ${MAX_QUESTION_LENGTH}자 이하로 입력해 주세요.`

    questionInput.value?.focus()

    return
  }

  /*
   * 사용자의 질문을 먼저 채팅 화면에 추가한다.
   */
  chatMessages.value.push({
    id: createMessageId(),
    role: 'user',
    text: normalizedQuestion,
    disclaimer: '',
    precedents: [],
  })

  /*
   * 전송이 완료된 입력창을 비운다.
   */
  question.value = ''

  isLoading.value = true

  await scrollToChatEnd()

  try {
    /*
     * 백엔드 판례 검색형 챗봇 API를 호출한다.
     *
     * 현재 API는 이전 대화 전체가 아니라
     * 지금 입력한 질문 한 건만 검색한다.
     */
    const response =
      await askPrecedentChat(normalizedQuestion)

    /*
     * 백엔드 응답을 그대로 HTML로 출력하지 않는다.
     *
     * 아래 template에서도 v-html이 아닌 {{ }}를 사용하므로
     * 판례 데이터에 HTML 또는 script 문자열이 포함되어도
     * 일반 텍스트로 처리된다.
     */
    const answer =
      typeof response.answer === 'string' &&
      response.answer.trim()
        ? response.answer
        : '관련 판례 검색을 완료했지만 안내 문장을 생성하지 못했습니다.'

    const disclaimer =
      typeof response.disclaimer === 'string'
        ? response.disclaimer
        : ''

    chatMessages.value.push({
      id: createMessageId(),
      role: 'assistant',
      text: answer,
      disclaimer,
      precedents: normalizePrecedents(
        response.precedents,
      ),
    })
  } catch (error) {
    /*
     * API 모듈에서 안전하게 가공한 오류 메시지만 출력한다.
     *
     * 사용자가 입력한 원문은 Console에 출력하지 않는다.
     * 사고·질병 내용에 개인정보가 포함될 수 있기 때문이다.
     */
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '판례 안내 중 오류가 발생했습니다.'

    console.error('판례 챗봇 API 호출 실패')
  } finally {
    isLoading.value = false

    await scrollToChatEnd()

    /*
     * 사용자가 다음 질문을 바로 입력할 수 있도록
     * 입력창에 포커스를 돌려준다.
     */
    questionInput.value?.focus()
  }
}

/**
 * 예시 질문을 입력창에 넣는다.
 *
 * 예시 질문은 바로 전송하지 않고 사용자가 확인한 뒤
 * 직접 전송 버튼을 누르게 한다.
 *
 * @param {string} exampleQuestion 예시 질문
 */
function applyExampleQuestion(exampleQuestion) {
  if (isLoading.value) {
    return
  }

  question.value = exampleQuestion
  errorMessage.value = ''

  questionInput.value?.focus()
}
</script>

<template>
  <!-- 챗봇 화면 상단 설명 영역이다. -->
  <section class="subpage-heading">
    <div class="page-container">
      <p class="eyebrow">
        VECTOR SEARCH · PRECEDENT GUIDE
      </p>

      <h1>산재 판례 안내 챗봇</h1>

      <p>
        사고 상황을 입력하면 Ollama 임베딩과 Oracle VECTOR 검색을
        이용하여 의미적으로 가까운 산재보험 판례를 찾아드립니다.
      </p>
    </div>
  </section>

  <section class="chat-section">
    <div class="page-container chat-layout">
      <!-- 실제 대화가 표시되는 영역이다. -->
      <div class="chat-panel">
        <header class="chat-panel__header">
          <div>
            <span
              class="chat-panel__status"
              aria-hidden="true"
            ></span>

            <strong>WorkCare 판례 안내</strong>
          </div>

          <span>검색형 챗봇</span>
        </header>

        <!--
          role="log"는 새로운 채팅 메시지가 추가되는 영역임을
          보조기기에 전달한다.
        -->
        <div
          class="chat-messages"
          role="log"
          aria-live="polite"
          aria-label="판례 챗봇 대화 내용"
        >
          <article
            v-for="message in chatMessages"
            :key="message.id"
            class="chat-message"
            :class="
              message.role === 'user'
                ? 'chat-message--user'
                : 'chat-message--assistant'
            "
          >
            <p class="chat-message__speaker">
              {{
                message.role === 'user'
                  ? '사용자'
                  : 'WorkCare 안내'
              }}
            </p>

            <!--
              v-html을 사용하지 않고 텍스트 보간법을 사용한다.
              외부 판례 데이터로 인한 XSS 실행을 방지한다.
            -->
            <div class="chat-message__bubble">
              {{ message.text }}
            </div>

            <!-- 챗봇 답변에 포함된 관련 판례 카드다. -->
            <ul
              v-if="message.precedents.length > 0"
              class="chat-precedents"
            >
              <li
                v-for="(
                  precedent,
                  precedentIndex
                ) in message.precedents"
                :key="
                  `${message.id}-${precedent.caseNumber}-${precedentIndex}`
                "
              >
                <article class="chat-precedent-card">
                  <div class="chat-precedent-card__top">
                    <span>
                      관련 판례
                      {{ precedentIndex + 1 }}
                    </span>

                    <strong>
                      유사도
                      {{ precedent.similarityPercent }}%
                    </strong>
                  </div>

                  <div
                    class="similarity-bar"
                    :aria-label="
                      `판례 유사도 ${precedent.similarityPercent}%`
                    "
                  >
                    <span
                      :style="{
                        width:
                          `${precedent.similarityPercent}%`,
                      }"
                    ></span>
                  </div>

                  <h3>{{ precedent.title }}</h3>

                  <dl class="chat-precedent-card__metadata">
                    <div>
                      <dt>사건번호</dt>
                      <dd>{{ precedent.caseNumber }}</dd>
                    </div>

                    <div>
                      <dt>법원</dt>
                      <dd>{{ precedent.courtName }}</dd>
                    </div>

                    <div>
                      <dt>사건유형</dt>
                      <dd>{{ precedent.caseType }}</dd>
                    </div>

                    <div>
                      <dt>판결결과</dt>
                      <dd>{{ precedent.resultType }}</dd>
                    </div>
                  </dl>

                  <details class="chat-precedent-card__excerpt">
                    <summary>검색 근거 문장 보기</summary>

                    <p>{{ precedent.matchedExcerpt }}</p>
                  </details>
                </article>
              </li>
            </ul>

            <p
              v-if="message.disclaimer"
              class="chat-message__disclaimer"
            >
              {{ message.disclaimer }}
            </p>
          </article>

          <!-- API 호출 중 표시되는 메시지다. -->
          <article
            v-if="isLoading"
            class="chat-message chat-message--assistant"
            role="status"
          >
            <p class="chat-message__speaker">
              WorkCare 안내
            </p>

            <div class="chat-message__bubble">
              <span
                class="chat-loading"
                aria-hidden="true"
              >
                <i></i>
                <i></i>
                <i></i>
              </span>

              질문과 관련된 판례를 검색하고 있습니다.
            </div>
          </article>

          <!-- 새 메시지가 추가되면 이 위치로 이동한다. -->
          <div ref="chatEnd"></div>
        </div>

        <!-- 질문 입력 영역이다. -->
        <form
          class="chat-form"
          aria-label="판례 챗봇 질문 입력"
          @submit.prevent="submitQuestion"
        >
          <label for="precedent-chat-question">
            사고 또는 질병 상황
          </label>

          <textarea
            id="precedent-chat-question"
            ref="questionInput"
            v-model="question"
            :maxlength="MAX_QUESTION_LENGTH"
            :disabled="isLoading"
            rows="4"
            placeholder="예: 공사장에서 철거 작업을 하던 중 쇠파이프가 무릎에 떨어져 다쳤습니다."
          ></textarea>

          <div class="chat-form__bottom">
            <span
              class="chat-form__length"
              :class="{
                'chat-form__length--warning':
                  remainingLength <= 50,
              }"
            >
              {{ remainingLength }}자 남음
            </span>

            <button
              type="submit"
              :disabled="!canSubmit"
            >
              {{
                isLoading
                  ? '판례 검색 중'
                  : '관련 판례 찾기'
              }}
            </button>
          </div>

          <p
            v-if="errorMessage"
            class="chat-form__error"
            role="alert"
          >
            {{ errorMessage }}
          </p>

          <p class="chat-form__privacy">
            이름, 주민등록번호, 정확한 주소 등 개인정보는
            입력하지 마세요.
          </p>
        </form>
      </div>

      <!-- 오른쪽 이용 안내 영역이다. -->
      <aside class="chat-guide">
        <h2>질문 작성 방법</h2>

        <ol>
          <li>
            <strong>수행하던 업무</strong>
            <p>사고 당시 어떤 일을 하고 있었는지 작성합니다.</p>
          </li>

          <li>
            <strong>사고 원인</strong>
            <p>넘어짐, 낙하물, 반복 작업 등 원인을 작성합니다.</p>
          </li>

          <li>
            <strong>다친 부위</strong>
            <p>허리, 무릎, 손목 등 다친 부위를 작성합니다.</p>
          </li>
        </ol>

        <div class="chat-examples">
          <h3>예시 질문</h3>

          <button
            type="button"
            :disabled="isLoading"
            @click="
              applyExampleQuestion(
                '공사장에서 철거 작업을 하던 중 쇠파이프가 무릎에 떨어져 다쳤습니다.',
              )
            "
          >
            공사장 낙하물 사고
          </button>

          <button
            type="button"
            :disabled="isLoading"
            @click="
              applyExampleQuestion(
                '무거운 짐을 반복해서 옮기는 업무를 하다가 허리를 다쳤습니다.',
              )
            "
          >
            반복 작업 중 허리 부상
          </button>
        </div>

        <div class="chat-guide__notice">
          <strong>주의사항</strong>

          <p>
            검색 결과는 유사 판례를 찾기 위한 참고자료이며
            산재 승인 가능성이나 법률적 결론을 의미하지 않습니다.
          </p>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.chat-section {
  min-height: 700px;
  padding: 64px 0 96px;
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  align-items: start;
  gap: 28px;
}

.chat-panel {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.chat-panel__header {
  display: flex;
  min-height: 68px;
  padding: 0 24px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border);
}

.chat-panel__header div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-panel__header > span {
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 700;
}

.chat-panel__status {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-accent);
  box-shadow: 0 0 0 5px rgba(0, 168, 126, 0.12);
}

.chat-messages {
  display: flex;
  height: 600px;
  overflow-y: auto;
  padding: 28px;
  flex-direction: column;
  gap: 24px;
  background: #f7f9fc;
}

.chat-message {
  width: min(100%, 720px);
}

.chat-message--user {
  margin-left: auto;
}

.chat-message__speaker {
  margin: 0 0 8px;
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.chat-message--user .chat-message__speaker {
  text-align: right;
}

.chat-message__bubble {
  padding: 17px 19px;
  border: 1px solid var(--color-border);
  border-radius: 4px 16px 16px;
  color: var(--color-text);
  background: #ffffff;
  line-height: 1.75;
  white-space: pre-line;
  word-break: keep-all;
}

.chat-message--user .chat-message__bubble {
  margin-left: auto;
  border-color: var(--color-primary);
  border-radius: 16px 4px 16px 16px;
  color: #ffffff;
  background: var(--color-primary);
}

.chat-precedents {
  display: grid;
  margin: 14px 0 0;
  padding: 0;
  gap: 12px;
  list-style: none;
}

.chat-precedent-card {
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: #ffffff;
}

.chat-precedent-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 800;
}

.similarity-bar {
  height: 6px;
  margin-top: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #e4e9f0;
}

.similarity-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-accent);
}

.chat-precedent-card h3 {
  margin: 18px 0 14px;
  font-size: 18px;
}

.chat-precedent-card__metadata {
  display: grid;
  margin: 0;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.chat-precedent-card__metadata div {
  min-width: 0;
}

.chat-precedent-card__metadata dt {
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.chat-precedent-card__metadata dd {
  margin: 4px 0 0;
  line-height: 1.5;
  word-break: break-word;
}

.chat-precedent-card__excerpt {
  margin-top: 16px;
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
}

.chat-precedent-card__excerpt summary {
  color: var(--color-primary);
  font-weight: 800;
  cursor: pointer;
}

.chat-precedent-card__excerpt p {
  margin: 12px 0 0;
  color: var(--color-text-muted);
  line-height: 1.75;
  word-break: keep-all;
}

.chat-message__disclaimer {
  margin: 12px 0 0;
  color: #7a5a13;
  font-size: 13px;
  line-height: 1.6;
}

.chat-loading {
  display: inline-flex;
  margin-right: 8px;
  gap: 4px;
}

.chat-loading i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: chat-bounce 1s infinite alternate;
}

.chat-loading i:nth-child(2) {
  animation-delay: 0.2s;
}

.chat-loading i:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes chat-bounce {
  to {
    transform: translateY(-5px);
    opacity: 0.45;
  }
}

.chat-form {
  padding: 22px 24px;
  border-top: 1px solid var(--color-border);
}

.chat-form label {
  display: block;
  margin-bottom: 9px;
  font-size: 14px;
  font-weight: 800;
}

.chat-form textarea {
  width: 100%;
  resize: vertical;
  padding: 14px;
  border: 1px solid #cbd5e2;
  border-radius: 10px;
  color: var(--color-text);
  line-height: 1.6;
}

.chat-form textarea:focus {
  border-color: var(--color-primary);
  outline: 3px solid rgba(22, 65, 148, 0.16);
}

.chat-form__bottom {
  display: flex;
  margin-top: 12px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.chat-form__length {
  color: var(--color-text-muted);
  font-size: 13px;
}

.chat-form__length--warning {
  color: #a35d00;
  font-weight: 800;
}

.chat-form button {
  min-height: 46px;
  padding: 0 22px;
  border: 0;
  border-radius: 9px;
  color: #ffffff;
  background: var(--color-primary);
  font-weight: 800;
  cursor: pointer;
}

.chat-form button:disabled {
  background: #9ca8ba;
  cursor: not-allowed;
}

.chat-form__error {
  margin: 12px 0 0;
  color: #a12828;
  font-size: 14px;
  font-weight: 700;
}

.chat-form__privacy {
  margin: 10px 0 0;
  color: var(--color-text-muted);
  font-size: 12px;
}

.chat-guide {
  position: sticky;
  top: 104px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: #ffffff;
  box-shadow: var(--shadow-card);
}

.chat-guide h2 {
  margin: 0;
  font-size: 20px;
}

.chat-guide ol {
  display: grid;
  margin: 22px 0 0;
  padding-left: 22px;
  gap: 18px;
}

.chat-guide li::marker {
  color: var(--color-primary);
  font-weight: 900;
}

.chat-guide li p {
  margin: 5px 0 0;
  color: var(--color-text-muted);
  font-size: 14px;
  line-height: 1.6;
}

.chat-examples {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
}

.chat-examples h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.chat-examples button {
  width: 100%;
  margin-top: 8px;
  padding: 11px 12px;
  border: 1px solid var(--color-border);
  border-radius: 9px;
  color: var(--color-primary);
  background: #ffffff;
  font-weight: 700;
  cursor: pointer;
}

.chat-guide__notice {
  margin-top: 24px;
  padding: 16px;
  border-radius: 10px;
  background: #fff8e8;
}

.chat-guide__notice strong {
  color: #76520b;
}

.chat-guide__notice p {
  margin: 7px 0 0;
  color: #745f31;
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 900px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .chat-guide {
    position: static;
  }
}

@media (max-width: 640px) {
  .chat-section {
    padding: 36px 0 64px;
  }

  .chat-messages {
    height: 540px;
    padding: 18px;
  }

  .chat-precedent-card__metadata {
    grid-template-columns: 1fr;
  }

  .chat-form__bottom {
    align-items: stretch;
    flex-direction: column;
  }

  .chat-form button {
    width: 100%;
  }
}
</style>
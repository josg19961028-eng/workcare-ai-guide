<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import {
  fetchPrecedents,
  fetchPrecedentSearchOptions,
} from '@/api/precedentApi'

/*
 * 검색 선택항목이다.
 *
 * 사용자가 임의로 값을 작성하는 대신 근로복지공단 API가 제공하는
 * 분류값 중 하나를 선택하게 한다.
 */
const searchOptions = reactive({
  resultTypes: [],
  caseTypes: [],
  accidentDiseaseTypes: [],
})

/*
 * 현재 검색 폼에서 사용자가 선택 중인 값이다.
 */
const formCondition = reactive({
  resultType: '',
  caseType: '',
  accidentDiseaseType: '',
})

/*
 * 실제 판례 조회에 적용된 검색조건이다.
 *
 * formCondition과 분리하지 않으면 사용자가 검색 버튼을 누르지 않고
 * 선택값만 변경한 상태에서 다음 페이지를 눌렀을 때
 * 의도하지 않은 새 조건으로 검색될 수 있다.
 */
const appliedCondition = reactive({
  resultType: '',
  caseType: '',
  accidentDiseaseType: '',
})

/*
 * 현재 페이지에 표시할 판례 목록이다.
 */
const precedents = ref([])

/*
 * 검색조건에 해당하는 전체 판례 수다.
 */
const totalCount = ref(0)

/*
 * 현재 페이지 번호다.
 */
const currentPage = ref(1)

/*
 * 판결문 전문은 데이터 크기가 크므로 한 페이지에 3건만 조회한다.
 */
const pageSize = 3

/*
 * 판례 목록 조회 상태다.
 */
const isLoading = ref(false)

/*
 * 검색 선택항목 조회 상태다.
 */
const isOptionsLoading = ref(false)

/*
 * 판례 목록 조회 오류 메시지다.
 */
const errorMessage = ref('')

/*
 * 검색 선택항목 조회 오류 메시지다.
 *
 * 선택항목 조회와 판례 목록 조회를 분리하면 한쪽이 실패해도
 * 다른 기능까지 모두 사용하지 못하는 상황을 줄일 수 있다.
 */
const optionsErrorMessage = ref('')

/*
 * 전체 판례 수와 페이지 크기로 마지막 페이지를 계산한다.
 */
const totalPages = computed(() => {
  return Math.max(
    1,
    Math.ceil(totalCount.value / pageSize),
  )
})

/*
 * 하나 이상의 검색조건이 적용됐는지 확인한다.
 */
const hasAppliedCondition = computed(() => {
  return Boolean(
    appliedCondition.resultType ||
      appliedCondition.caseType ||
      appliedCondition.accidentDiseaseType,
  )
})

/**
 * 판례 검색 선택항목을 조회한다.
 */
async function loadSearchOptions() {
  optionsErrorMessage.value = ''
  isOptionsLoading.value = true

  try {
    const responseData =
      await fetchPrecedentSearchOptions()

    /*
     * 예상한 배열이 아닌 값이 오더라도 select 출력이 실패하지 않도록
     * 각 필드를 방어적으로 검사한다.
     */
    searchOptions.resultTypes = Array.isArray(
      responseData.resultTypes,
    )
      ? responseData.resultTypes
      : []

    searchOptions.caseTypes = Array.isArray(
      responseData.caseTypes,
    )
      ? responseData.caseTypes
      : []

    searchOptions.accidentDiseaseTypes = Array.isArray(
      responseData.accidentDiseaseTypes,
    )
      ? responseData.accidentDiseaseTypes
      : []
  } catch (error) {
    optionsErrorMessage.value =
      error instanceof Error
        ? error.message
        : '판례 검색조건을 불러오는 중 오류가 발생했습니다.'

    /*
     * 인증키나 외부 XML은 Console에 기록하지 않는다.
     * 현재 Error에는 백엔드가 제공한 안전한 메시지만 들어 있다.
     */
    console.error('판례 검색조건 조회 실패', error)
  } finally {
    isOptionsLoading.value = false
  }
}

/**
 * 현재 적용된 검색조건으로 판례를 조회한다.
 */
async function loadPrecedents() {
  errorMessage.value = ''
  isLoading.value = true

  try {
    const responseData = await fetchPrecedents({
      page: currentPage.value,
      size: pageSize,
      resultType: appliedCondition.resultType,
      caseType: appliedCondition.caseType,
      accidentDiseaseType:
        appliedCondition.accidentDiseaseType,
    })

    precedents.value = Array.isArray(
      responseData.precedents,
    )
      ? responseData.precedents
      : []

    totalCount.value =
      Number(responseData.totalCount) || 0

    const responsePage = Number(responseData.page)

    if (
      Number.isInteger(responsePage) &&
      responsePage >= 1
    ) {
      currentPage.value = responsePage
    }
  } catch (error) {
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '판례를 불러오는 중 오류가 발생했습니다.'

    /*
     * 실패 전에 조회했던 판례가 현재 결과처럼 남지 않게 초기화한다.
     */
    precedents.value = []
    totalCount.value = 0

    console.error('산재보험 판례 조회 실패', error)
  } finally {
    isLoading.value = false
  }
}

/**
 * 검색 폼의 값을 실제 검색조건으로 적용한다.
 */
async function applySearch() {
  if (isLoading.value) {
    return
  }

  /*
   * 앞뒤 공백을 제거하여 의미가 같은 조건이 다르게 전달되지 않게 한다.
   */
  appliedCondition.resultType =
    formCondition.resultType.trim()

  appliedCondition.caseType =
    formCondition.caseType.trim()

  appliedCondition.accidentDiseaseType =
    formCondition.accidentDiseaseType.trim()

  /*
   * 새로운 검색은 항상 첫 페이지부터 조회한다.
   */
  currentPage.value = 1

  await loadPrecedents()
}

/**
 * 모든 검색조건을 초기화하고 전체 판례를 조회한다.
 */
async function resetSearch() {
  if (isLoading.value) {
    return
  }

  formCondition.resultType = ''
  formCondition.caseType = ''
  formCondition.accidentDiseaseType = ''

  appliedCondition.resultType = ''
  appliedCondition.caseType = ''
  appliedCondition.accidentDiseaseType = ''

  currentPage.value = 1

  await loadPrecedents()
}

/**
 * 지정한 페이지의 판례를 조회한다.
 *
 * @param {number} targetPage 이동할 페이지 번호
 */
async function movePage(targetPage) {
  if (
    isLoading.value ||
    targetPage < 1 ||
    targetPage > totalPages.value
  ) {
    return
  }

  currentPage.value = targetPage

  await loadPrecedents()

  document
    .getElementById('precedent-results')
    ?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
}

function goToPreviousPage() {
  movePage(currentPage.value - 1)
}

function goToNextPage() {
  movePage(currentPage.value + 1)
}

/*
 * 화면이 처음 열리면 검색조건과 전체 판례 첫 페이지를 함께 조회한다.
 *
 * 두 요청은 서로 의존하지 않으므로 병렬로 실행해 대기시간을 줄인다.
 */
onMounted(async () => {
  await Promise.allSettled([
    loadSearchOptions(),
    loadPrecedents(),
  ])
})
</script>

<template>
  <section class="subpage-heading">
    <div class="page-container">
      <p class="eyebrow">INDUSTRIAL ACCIDENT PRECEDENTS</p>

      <h1>산재보험 판례 검색</h1>

      <p>
        판결결과, 사건유형 및 사고·질병 구분을 선택하여
        근로복지공단이 제공하는 산재보험 판례를 검색할 수 있습니다.
      </p>
    </div>
  </section>

  <section
    id="precedent-results"
    class="precedent-section"
  >
    <div class="page-container">
      <!-- 판례 서비스 이용 시 주의사항이다. -->
      <aside class="precedent-notice">
        <strong>이용 안내</strong>

        <p>
          판례 정보는 일반적인 정보 제공을 위한 자료이며,
          개별 사건에 대한 법률 자문이나 판결 결과 예측을
          제공하지 않습니다.
        </p>
      </aside>

      <!-- 검색 선택항목을 가져오지 못한 경우다. -->
      <div
        v-if="optionsErrorMessage"
        class="option-error"
        role="alert"
      >
        <span>{{ optionsErrorMessage }}</span>

        <button
          type="button"
          :disabled="isOptionsLoading"
          @click="loadSearchOptions"
        >
          검색조건 다시 불러오기
        </button>
      </div>

      <!-- 판례 검색조건 입력 영역이다. -->
      <form
        class="precedent-search-form"
        aria-label="산재보험 판례 검색조건"
        @submit.prevent="applySearch"
      >
        <div class="precedent-search-form__grid">
          <label>
            <span>판결결과</span>

            <select
              v-model="formCondition.resultType"
              :disabled="isOptionsLoading || isLoading"
            >
              <option value="">전체</option>

              <option
                v-for="resultType in searchOptions.resultTypes"
                :key="resultType"
                :value="resultType"
              >
                {{ resultType }}
              </option>
            </select>
          </label>

          <label>
            <span>사건유형</span>

            <select
              v-model="formCondition.caseType"
              :disabled="isOptionsLoading || isLoading"
            >
              <option value="">전체</option>

              <option
                v-for="caseType in searchOptions.caseTypes"
                :key="caseType"
                :value="caseType"
              >
                {{ caseType }}
              </option>
            </select>
          </label>

          <label>
            <span>사고·질병 구분</span>

            <select
              v-model="formCondition.accidentDiseaseType"
              :disabled="isOptionsLoading || isLoading"
            >
              <option value="">전체</option>

              <option
                v-for="
                  accidentDiseaseType in
                  searchOptions.accidentDiseaseTypes
                "
                :key="accidentDiseaseType"
                :value="accidentDiseaseType"
              >
                {{ accidentDiseaseType }}
              </option>
            </select>
          </label>
        </div>

        <div class="precedent-search-form__actions">
          <button
            class="precedent-search-form__submit"
            type="submit"
            :disabled="isLoading || isOptionsLoading"
          >
            {{ isLoading ? '검색 중' : '판례 검색' }}
          </button>

          <button
            class="precedent-search-form__reset"
            type="button"
            :disabled="isLoading"
            @click="resetSearch"
          >
            조건 초기화
          </button>
        </div>
      </form>

      <div class="precedent-section__header">
        <div>
          <h2>
            {{
              hasAppliedCondition
                ? '조건별 판례 검색 결과'
                : '전체 판례 조회 결과'
            }}
          </h2>

          <p>
            근로복지공단 산재보험 판례 판결문 조회
            Open API 제공
          </p>
        </div>

        <span
          v-if="!isLoading && !errorMessage"
          class="result-count"
          aria-live="polite"
        >
          전체 {{ totalCount.toLocaleString() }}건
        </span>
      </div>

      <!-- 판례 조회 중 상태다. -->
      <div
        v-if="isLoading"
        class="state-panel"
        role="status"
        aria-live="polite"
      >
        <span
          class="loading-spinner"
          aria-hidden="true"
        ></span>

        <h3>산재보험 판례를 검색하고 있습니다</h3>

        <p>
          판결문 데이터를 조회하는 동안 잠시 기다려주세요.
        </p>
      </div>

      <!-- 판례 조회 실패 상태다. -->
      <div
        v-else-if="errorMessage"
        class="state-panel state-panel--error"
        role="alert"
      >
        <span
          class="state-panel__symbol"
          aria-hidden="true"
        >
          !
        </span>

        <h3>판례를 불러오지 못했습니다</h3>

        <p>{{ errorMessage }}</p>

        <button
          class="secondary-button"
          type="button"
          @click="loadPrecedents"
        >
          다시 시도
        </button>
      </div>

      <!-- 검색 결과가 없는 경우다. -->
      <div
        v-else-if="precedents.length === 0"
        class="state-panel"
      >
        <span
          class="state-panel__symbol"
          aria-hidden="true"
        >
          0
        </span>

        <h3>검색조건에 해당하는 판례가 없습니다</h3>

        <p>
          다른 판결결과, 사건유형 또는 사고·질병 구분을
          선택해보세요.
        </p>
      </div>

      <!-- 정상 판례 검색 결과다. -->
      <template v-else>
        <ul class="precedent-list">
          <li
            v-for="precedent in precedents"
            :key="
              `${precedent.caseNumber}-${precedent.title}`
            "
          >
            <article class="precedent-card">
              <div class="precedent-card__badges">
                <span>
                  {{ precedent.resultType || '판결결과 미제공' }}
                </span>

                <span>
                  {{ precedent.caseType || '사건유형 미제공' }}
                </span>

                <span>
                  {{
                    precedent.accidentDiseaseType ||
                    '사고·질병 구분 미제공'
                  }}
                </span>
              </div>

              <h3>
                {{ precedent.title || '사건명 미제공' }}
              </h3>

              <dl class="precedent-card__metadata">
                <div>
                  <dt>사건번호</dt>
                  <dd>
                    {{ precedent.caseNumber || '미제공' }}
                  </dd>
                </div>

                <div>
                  <dt>법원</dt>
                  <dd>
                    {{ precedent.courtName || '미제공' }}
                  </dd>
                </div>
              </dl>

              <!--
                details는 JavaScript 없이도 열고 닫을 수 있는
                접근성 있는 HTML 요소다.
              -->
              <details class="precedent-content">
                <summary>판결문 전문 보기</summary>

                <!--
                  v-html을 사용하지 않고 텍스트 보간을 사용한다.

                  외부 판결문에 HTML이나 script 문자열이 들어 있더라도
                  Vue가 일반 텍스트로 처리하므로 XSS 실행을 방지한다.
                -->
                <pre>{{
                  precedent.content ||
                  '판결문 내용이 제공되지 않았습니다.'
                }}</pre>
              </details>
            </article>
          </li>
        </ul>

        <nav
          class="pagination"
          aria-label="산재보험 판례 페이지 이동"
        >
          <button
            class="pagination__button"
            type="button"
            :disabled="isLoading || currentPage <= 1"
            @click="goToPreviousPage"
          >
            이전
          </button>

          <span
            class="pagination__status"
            aria-live="polite"
          >
            {{ currentPage.toLocaleString() }} /
            {{ totalPages.toLocaleString() }} 페이지
          </span>

          <button
            class="pagination__button"
            type="button"
            :disabled="
              isLoading || currentPage >= totalPages
            "
            @click="goToNextPage"
          >
            다음
          </button>
        </nav>
      </template>

      <p class="precedent-source">
        출처:
        <a
          href="https://www.data.go.kr/data/15041878/openapi.do"
          target="_blank"
          rel="noopener noreferrer"
        >
          근로복지공단 산재보험 판례 판결문 조회 서비스
        </a>
      </p>
    </div>
  </section>
</template>

<style scoped>
.precedent-section {
  min-height: 600px;
  padding: 64px 0 96px;
}

.precedent-notice {
  margin-bottom: 24px;
  padding: 20px 22px;
  border-left: 4px solid var(--color-primary);
  border-radius: 0 12px 12px 0;
  background: var(--color-primary-light);
}

.precedent-notice strong {
  color: var(--color-primary-dark);
}

.precedent-notice p {
  margin: 8px 0 0;
  color: var(--color-text-muted);
  line-height: 1.7;
  word-break: keep-all;
}

.option-error {
  display: flex;
  margin-bottom: 16px;
  padding: 14px 16px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #efc4c4;
  border-radius: 10px;
  color: #8c2929;
  background: #fffafa;
}

.option-error button {
  border: 0;
  color: var(--color-primary);
  background: transparent;
  font-weight: 800;
  cursor: pointer;
}

.precedent-search-form {
  margin-bottom: 44px;
  padding: 26px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.precedent-search-form__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.precedent-search-form label {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 9px;
  color: var(--color-text);
  font-size: 14px;
  font-weight: 800;
}

.precedent-search-form select {
  width: 100%;
  min-height: 48px;
  padding: 0 38px 0 13px;
  border: 1px solid #cbd5e2;
  border-radius: 9px;
  color: var(--color-text);
  background: #ffffff;
}

.precedent-search-form select:focus {
  border-color: var(--color-primary);
  outline: 3px solid rgba(22, 65, 148, 0.16);
}

.precedent-search-form__actions {
  display: flex;
  margin-top: 22px;
  justify-content: flex-end;
  gap: 10px;
}

.precedent-search-form__submit,
.precedent-search-form__reset {
  min-height: 46px;
  padding: 0 20px;
  border-radius: 9px;
  font-weight: 800;
  cursor: pointer;
}

.precedent-search-form__submit {
  border: 1px solid var(--color-primary);
  color: #ffffff;
  background: var(--color-primary);
}

.precedent-search-form__reset {
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  background: #ffffff;
}

.precedent-search-form__submit:disabled,
.precedent-search-form__reset:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.precedent-section__header {
  display: flex;
  margin-bottom: 28px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.precedent-section__header h2 {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.03em;
}

.precedent-section__header p {
  margin: 10px 0 0;
  color: var(--color-text-muted);
  font-size: 14px;
}

.precedent-list {
  display: grid;
  margin: 0;
  padding: 0;
  gap: 20px;
  list-style: none;
}

.precedent-card {
  padding: 28px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: var(--color-surface);
  box-shadow: 0 10px 26px rgba(23, 32, 51, 0.05);
}

.precedent-card__badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.precedent-card__badges span {
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--color-primary-dark);
  background: var(--color-primary-light);
  font-size: 12px;
  font-weight: 800;
}

.precedent-card h3 {
  margin: 20px 0 0;
  font-size: 22px;
  line-height: 1.45;
  word-break: keep-all;
}

.precedent-card__metadata {
  display: grid;
  margin: 22px 0 0;
  padding: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  border-radius: 12px;
  background: #f7f9fc;
}

.precedent-card__metadata div {
  min-width: 0;
}

.precedent-card__metadata dt {
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.precedent-card__metadata dd {
  margin: 6px 0 0;
  overflow-wrap: anywhere;
  font-weight: 700;
}

.precedent-content {
  margin-top: 22px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
}

.precedent-content summary {
  padding: 16px 18px;
  color: var(--color-primary);
  background: #ffffff;
  font-weight: 800;
  cursor: pointer;
}

.precedent-content[open] summary {
  border-bottom: 1px solid var(--color-border);
  background: var(--color-primary-light);
}

.precedent-content pre {
  max-height: 520px;
  margin: 0;
  padding: 22px;
  overflow: auto;
  color: #273247;
  background: #fbfcfe;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.precedent-source {
  margin: 38px 0 0;
  color: var(--color-text-muted);
  font-size: 13px;
  text-align: right;
}

.precedent-source a {
  color: var(--color-primary);
  font-weight: 800;
}

@media (max-width: 760px) {
  .precedent-search-form__grid {
    grid-template-columns: 1fr;
  }

  .precedent-section__header,
  .option-error {
    align-items: stretch;
    flex-direction: column;
  }

  .precedent-card__metadata {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .precedent-section {
    padding: 48px 0 72px;
  }

  .precedent-search-form,
  .precedent-card {
    padding: 20px;
  }

  .precedent-search-form__actions {
    flex-direction: column;
  }

  .precedent-search-form__submit,
  .precedent-search-form__reset {
    width: 100%;
  }
}
</style>
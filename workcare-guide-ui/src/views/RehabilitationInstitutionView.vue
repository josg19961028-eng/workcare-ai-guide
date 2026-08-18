<script setup>
/*
 * onMounted:
 * 현재 Vue 화면이 브라우저에 표시된 직후 실행할 작업을 등록한다.
 *
 * ref:
 * 화면의 변경 가능한 상태를 반응형 변수로 만든다.
 * ref 값이 변경되면 해당 값을 사용하는 화면도 자동으로 갱신된다.
 */
import { computed, onMounted, ref } from 'vue'
/*
 * Spring API에서 조회한 재활기관 목록을 저장한다.
 *
 * 처음에는 데이터가 없으므로 빈 배열로 시작한다.
 */
const institutions = ref([])

/*
 * 공공데이터가 제공하는 전체 재활기관 수를 저장한다.
 */
const totalCount = ref(0)

/*
 * API 요청이 진행 중인지 나타낸다.
 *
 * true일 때는 로딩 화면을 표시하고,
 * false일 때는 로딩 화면을 숨긴다.
 */
const isLoading = ref(false)

/*
 * API 조회에 실패했을 때 사용자에게 보여줄 메시지를 저장한다.
 *
 * 오류가 없을 때는 빈 문자열을 사용한다.
 */
const errorMessage = ref('')

/*
 * 현재 조회 중인 사회복귀 지원기관 페이지 번호다.
 *
 * 이전·다음 버튼을 누르면 값이 변경되고
 * 변경된 페이지 번호로 API를 다시 호출한다.
 */
const currentPage = ref(1)

/*
 * 한 번에 조회할 기관 수다.
 */
const pageSize = 6

/*
 * 전체 사회복귀 지원기관 수와 한 페이지 결과 수를 이용해
 * 전체 페이지 수를 계산한다.
 *
 * computed 내부에서 사용하는 totalCount가 변경되면
 * totalPages도 자동으로 다시 계산된다.
 */
const totalPages = computed(() => {
  /*
   * 조회 전에도 최소 1페이지로 처리한다.
   */
  return Math.max(
    1,
    Math.ceil(totalCount.value / pageSize),
  )
})

/*
 * Spring Boot API에서 재활기관 목록을 조회한다.
 */
async function loadInstitutions() {
  /*
   * 새로운 조회를 시작할 때 이전 오류 메시지를 제거한다.
   */
  errorMessage.value = ''

  /*
   * 화면에 로딩 상태를 표시한다.
   */
  isLoading.value = true

  try {
    /*
     * Vue에서는 8080 전체 주소를 직접 사용하지 않고
     * Vite Proxy가 처리할 /api 주소로 요청한다.
     *
     * encodeURIComponent는 페이지 값을 URL에서 안전하게 사용할 수 있게
     * 인코딩한다. 현재는 숫자지만 검색어가 추가될 때도 같은 원칙을 사용한다.
     */
    const requestUrl =
      `/api/rehabilitation-institutions` +
      `?page=${encodeURIComponent(currentPage.value)}` +
      `&size=${encodeURIComponent(pageSize)}`

    /*
     * 브라우저 기본 fetch API로 Spring Boot에 GET 요청을 보낸다.
     *
     * Accept 헤더를 지정하여 JSON 응답을 원한다는 것을 명시한다.
     */
    const response = await fetch(requestUrl, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    })

    /*
     * fetch는 HTTP 400이나 500이 발생해도 자동으로 예외를 발생시키지 않는다.
     *
     * 따라서 response.ok를 직접 확인해야 한다.
     * response.ok는 HTTP 상태가 200~299일 때 true다.
     */
    if (!response.ok) {
      /*
       * 기본 오류 메시지다.
       *
       * 서버가 JSON 오류 응답을 제공하지 못하는 상황에서도
       * 사용자에게 최소한의 안내를 제공한다.
       */
      let message =
        '재활기관 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'

      try {
        /*
         * Spring의 GlobalApiExceptionHandler가 반환한
         * 공통 JSON 오류 응답을 읽는다.
         */
        const errorResponse = await response.json()

        /*
         * 서버가 안전한 사용자용 메시지를 제공했다면 그 값을 사용한다.
         */
        if (errorResponse.message) {
          message = errorResponse.message
        }
      } catch {
        /*
         * 오류 응답이 JSON이 아니어도 기본 메시지를 사용할 수 있으므로
         * 여기서는 별도의 처리를 하지 않는다.
         *
         * HTML 오류 페이지나 빈 응답 때문에 화면 코드까지
         * 다시 실패하지 않게 하는 방어적 처리다.
         */
      }

      /*
       * catch 영역에서 공통 처리할 수 있도록 오류를 발생시킨다.
       */
      throw new Error(message)
    }

    /*
     * 정상 JSON 응답을 JavaScript 객체로 변환한다.
     */
    const responseData = await response.json()

    /*
     * API의 institutions 값이 실제 배열일 때만 저장한다.
     *
     * 외부 응답이나 서버 코드가 예상과 다르게 변경됐을 때
     * Vue의 반복 출력에서 오류가 발생하는 것을 방지한다.
     */
    institutions.value = Array.isArray(responseData.institutions)
      ? responseData.institutions
      : []

    /*
     * 전체 기관 수를 숫자로 저장한다.
     *
     * 숫자로 변환할 수 없으면 0을 사용한다.
     */
    totalCount.value = Number(responseData.totalCount) || 0
  } catch (error) {
    /*
     * 네트워크 단절, Spring 서버 미실행 또는 API 오류를 처리한다.
     */
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '재활기관 정보를 불러오는 중 오류가 발생했습니다.'

    /*
     * 이전 조회 데이터가 남아 잘못된 정보처럼 보이지 않게 비운다.
     */
    institutions.value = []
    totalCount.value = 0

    /*
     * 개발 중 원인을 확인할 수 있게 브라우저 Console에 기록한다.
     *
     * 인증키나 외부 XML 원문은 포함하지 않는다.
     */
    console.error('재활기관 목록 조회 실패', error)
  } finally {
    /*
     * 성공과 실패 여부와 관계없이 로딩 상태를 종료한다.
     */
    isLoading.value = false
  }
}

/*
 * 외부 공공데이터가 제공한 홈페이지 주소가
 * 안전한 HTTP 또는 HTTPS 주소인지 검사한다.
 *
 * 외부 데이터는 신뢰할 수 없는 입력이므로 그대로 href에 넣지 않는다.
 * javascript: 같은 위험한 스킴이 링크로 사용되는 것을 방지한다.
 */
function getSafeWebsiteUrl(websiteUrl) {
  /*
   * 홈페이지 주소가 없으면 링크를 만들지 않는다.
   */
  if (!websiteUrl) {
    return null
  }

  try {
    /*
     * 문자열을 URL 객체로 변환하여 프로토콜을 검사한다.
     */
    const parsedUrl = new URL(websiteUrl)

    /*
     * 일반 웹사이트에서 사용하는 HTTP와 HTTPS만 허용한다.
     */
    if (parsedUrl.protocol === 'http:' || parsedUrl.protocol === 'https:') {
      return parsedUrl.href
    }

    return null
  } catch {
    /*
     * 올바른 URL 형식이 아니면 링크를 제공하지 않는다.
     */
    return null
  }
}

/**
 * 지정한 페이지의 사회복귀 지원기관을 조회한다.
 *
 * @param {number} targetPage 이동할 페이지 번호
 */
async function movePage(targetPage) {
  /*
   * 다음 상황에서는 페이지를 변경하지 않는다.
   *
   * 1. API 요청이 이미 진행 중인 경우
   * 2. 1보다 작은 페이지를 요청한 경우
   * 3. 전체 페이지보다 큰 페이지를 요청한 경우
   *
   * 로딩 중 버튼을 반복해서 누르는 동작도 차단하므로
   * 불필요한 중복 API 호출을 줄일 수 있다.
   */
  if (
    isLoading.value ||
    targetPage < 1 ||
    targetPage > totalPages.value
  ) {
    return
  }

  /*
   * 조회할 페이지 번호를 변경한다.
   */
  currentPage.value = targetPage

  /*
   * 변경된 페이지 번호를 이용해 API를 다시 호출한다.
   */
  await loadInstitutions()

  /*
   * 조회가 완료되면 결과 영역의 위쪽으로 이동한다.
   */
  document
    .getElementById('rehabilitation-institution-results')
    ?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
}

/*
 * 현재 페이지보다 하나 앞의 페이지를 조회한다.
 */
function goToPreviousPage() {
  movePage(currentPage.value - 1)
}

/*
 * 현재 페이지보다 하나 뒤의 페이지를 조회한다.
 */
function goToNextPage() {
  movePage(currentPage.value + 1)
}

/*
 * 컴포넌트가 화면에 표시되면 재활기관 목록을 자동으로 조회한다.
 */
onMounted(() => {
  loadInstitutions()
})
</script>

<template>
  <!-- 재활기관 페이지의 제목과 목적을 설명하는 상단 영역이다. -->
  <section class="subpage-heading">
    <div class="page-container">
		<p class="eyebrow">RETURN-TO-WORK SUPPORT</p>

		<h1>사회복귀 지원기관 찾기</h1>

		<p>
		  근로복지공단이 관리하는 직업훈련기관, 재활스포츠 및
		  심리재활 프로그램 위탁기관의 정보를 제공합니다.
		</p>
    </div>
  </section>

  <!-- 실제 재활기관 조회 결과를 출력하는 영역이다. -->
  <section id="rehabilitation-institution-results" class="institution-section">
    <div class="page-container">
      <div class="institution-section__header">
        <div>
			<h2>사회복귀 지원기관 조회 결과</h2>

			<p>
			  근로복지공단 산재재활기관관리정보 Open API 제공
			</p>
        </div>

        <!--
          API 조회가 끝난 경우에만 전체 기관 수를 표시한다.
          숫자가 긴 경우 읽기 쉽도록 쉼표를 넣는다.
        -->
        <span
          v-if="!isLoading && !errorMessage"
          class="result-count"
          aria-live="polite"
        >
          전체 {{ totalCount.toLocaleString() }}개 기관
        </span>
      </div>

      <!--
        API 요청 중 표시하는 상태다.
        aria-live를 사용해 보조기기가 상태 변경을 인식하게 한다.
      -->
      <div
        v-if="isLoading"
        class="state-panel"
        role="status"
        aria-live="polite"
      >
        <span class="loading-spinner" aria-hidden="true"></span>

        <h3>사회복귀 지원기관 정보를 불러오고 있습니다</h3>

        <p>공공데이터를 조회하는 동안 잠시 기다려주세요.</p>
      </div>

      <!-- API 요청 실패 상태다. -->
      <div
        v-else-if="errorMessage"
        class="state-panel state-panel--error"
        role="alert"
      >
        <span class="state-panel__symbol" aria-hidden="true">!</span>

        <h3>지원기관 정보를 불러오지 못했습니다</h3>

        <p>{{ errorMessage }}</p>

        <!-- 사용자가 페이지를 새로고침하지 않고 다시 요청할 수 있다. -->
        <button
          class="secondary-button"
          type="button"
          @click="loadInstitutions"
        >
          다시 시도
        </button>
      </div>

      <!-- 정상 응답이지만 조회된 기관이 없는 상태다. -->
      <div
        v-else-if="institutions.length === 0"
        class="state-panel"
      >
        <span class="state-panel__symbol" aria-hidden="true">0</span>

        <h3>조회된 사회복귀 지원기관이 없습니다</h3>

        <p>다른 검색 조건을 이용하거나 잠시 후 다시 확인해주세요.</p>
      </div>

      <!-- 조회된 재활기관 목록을 카드 형태로 출력한다. -->
      <ul v-else class="institution-grid">
        <!--
          v-for는 배열의 각 기관을 반복해서 출력한다.

          현재 공공데이터에는 고유 ID가 없으므로
          기관명, 주소, 전화번호를 조합해 임시 key로 사용한다.
        -->
        <li
          v-for="institution in institutions"
          :key="
            `${institution.institutionName}-${institution.address}-${institution.telephoneNumber}`
          "
          class="institution-card"
        >
          <article>
            <div class="institution-card__top">
              <!-- 기관 유형을 표시한다. -->
              <span class="institution-type">
                {{ institution.institutionTypeName || '기관 유형 미제공' }}
              </span>

              <!-- 관리 지사를 표시한다. -->
              <span class="managing-branch">
                {{ institution.managingBranchName || '관리 지사 미제공' }}
              </span>
            </div>

            <!-- 기관명이다. -->
            <h3>{{ institution.institutionName }}</h3>

            <!--
              기관의 세부 정보를 의미에 맞는 dt와 dd 구조로 표시한다.
            -->
            <dl class="institution-details">
              <div>
                <dt>주소</dt>
                <dd>{{ institution.address || '주소 정보 없음' }}</dd>
              </div>

              <div>
                <dt>전화번호</dt>
                <dd>
                  {{
                    institution.telephoneNumber ||
                    '전화번호 정보 없음'
                  }}
                </dd>
              </div>

              <div>
                <dt>팩스번호</dt>
                <dd>
                  {{ institution.faxNumber || '팩스번호 정보 없음' }}
                </dd>
              </div>
            </dl>

            <!--
              검증된 HTTP 또는 HTTPS 홈페이지 주소가 있을 때만
              외부 사이트 링크를 표시한다.
            -->
            <a
              v-if="getSafeWebsiteUrl(institution.websiteUrl)"
              class="institution-website"
              :href="getSafeWebsiteUrl(institution.websiteUrl)"
              target="_blank"
              rel="noopener noreferrer"
            >
              기관 홈페이지
              <span aria-hidden="true">↗</span>
            </a>

            <span
              v-else
              class="institution-website institution-website--disabled"
            >
              홈페이지 정보 없음
            </span>
          </article>
        </li>
      </ul>
	  <!--
	    정상적으로 기관이 조회되고 전체 페이지가 2페이지 이상일 때
	    이전·다음 페이지 버튼을 표시한다.
	  -->
	  <nav
	    v-if="
	      !isLoading &&
	      !errorMessage &&
	      institutions.length > 0 &&
	      totalPages > 1
	    "
	    class="pagination"
	    aria-label="사회복귀 지원기관 페이지 이동"
	  >
	    <!-- 첫 페이지에서는 이전 버튼을 사용할 수 없다. -->
	    <button
	      class="pagination__button"
	      type="button"
	      :disabled="isLoading || currentPage <= 1"
	      @click="goToPreviousPage"
	    >
	      이전
	    </button>

	    <!-- 현재 페이지와 전체 페이지를 사용자에게 알려준다. -->
	    <span
	      class="pagination__status"
	      aria-live="polite"
	    >
	      {{ currentPage.toLocaleString() }} /
	      {{ totalPages.toLocaleString() }} 페이지
	    </span>

	    <!-- 마지막 페이지에서는 다음 버튼을 사용할 수 없다. -->
	    <button
	      class="pagination__button"
	      type="button"
	      :disabled="
	        isLoading ||
	        currentPage >= totalPages
	      "
	      @click="goToNextPage"
	    >
	      다음
	    </button>
	  </nav>
    </div>
  </section>
</template>
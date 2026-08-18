/*
 * 판례 화면에서 사용하는 Spring Boot API 호출을 한곳에서 관리한다.
 *
 * View에서 fetch 코드를 분리하면 화면은 사용자 상태와 출력에 집중하고,
 * API 주소와 오류 처리는 이 파일에서 일관되게 관리할 수 있다.
 */

const JSON_HEADERS = Object.freeze({
  Accept: 'application/json',
})

/**
 * 요청 파라미터를 안전한 Query String으로 변환한다.
 *
 * URLSearchParams를 사용하면 한글과 특수문자가 자동으로 인코딩된다.
 *
 * @param {object} parameters 요청 파라미터
 * @returns {string} 완성된 Query String
 */
function createQueryString(parameters) {
  const query = new URLSearchParams()

  Object.entries(parameters).forEach(([name, value]) => {
    /*
     * 선택하지 않은 검색조건은 요청에서 제외한다.
     */
    if (value === null || value === undefined || value === '') {
      return
    }

    query.append(name, String(value))
  })

  return query.toString()
}

/**
 * Spring Boot API를 호출하고 JSON 응답을 반환한다.
 *
 * @param {string} path Spring Boot API 경로
 * @param {object} parameters 요청 파라미터
 * @param {string} fallbackMessage 서버 메시지를 읽지 못할 때 사용할 문구
 * @returns {Promise<object>} JSON 응답
 */
async function requestJson(path, parameters, fallbackMessage) {
  const queryString = createQueryString(parameters)

  const requestUrl = queryString
    ? `${path}?${queryString}`
    : path

  /*
   * 공공데이터 인증키는 Vue가 아니라 Spring Boot에서 관리한다.
   * 브라우저에는 우리 백엔드 API 경로만 노출된다.
   */
  const response = await fetch(requestUrl, {
    method: 'GET',
    headers: JSON_HEADERS,
  })

  /*
   * fetch는 HTTP 400이나 500을 자동으로 예외 처리하지 않으므로
   * response.ok를 반드시 확인한다.
   */
  if (!response.ok) {
    let message = fallbackMessage

    try {
      const errorResponse = await response.json()

      /*
       * 백엔드가 제공하는 안전한 사용자용 메시지만 화면에 전달한다.
       */
      if (
        typeof errorResponse.message === 'string' &&
        errorResponse.message.trim()
      ) {
        message = errorResponse.message
      }
    } catch {
      /*
       * HTML 또는 빈 오류 응답이 오면 기본 메시지를 사용한다.
       */
    }

    throw new Error(message)
  }

  return response.json()
}

/**
 * 판결결과, 사건유형, 사고·질병 구분 목록을 조회한다.
 */
export function fetchPrecedentSearchOptions() {
  return requestJson(
    '/api/precedents/options',
    {},
    '판례 검색조건을 불러오지 못했습니다.',
  )
}

/**
 * 검색조건에 해당하는 판례 목록을 조회한다.
 *
 * @param {object} condition 판례 검색조건
 */
export function fetchPrecedents(condition) {
  return requestJson(
    '/api/precedents',
    condition,
    '산재보험 판례를 불러오지 못했습니다.',
  )
}

/**
 * 검색조건에 해당하는 판례 개수를 조회한다.
 *
 * 현재 검색 화면에서는 목록 응답의 totalCount를 사용하지만,
 * 이후 통계 또는 챗봇 기능에서 재사용할 수 있다.
 */
export function fetchPrecedentCount(condition) {
  return requestJson(
    '/api/precedents/count',
    condition,
    '판례 개수를 불러오지 못했습니다.',
  )
}
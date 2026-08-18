/*
 * 판례 검색형 챗봇에서 사용하는 백엔드 API 호출을 관리한다.
 *
 * View 컴포넌트에 fetch 코드를 직접 작성하지 않고 별도 파일로 분리하면
 * 화면은 입력값과 출력 상태 관리에 집중할 수 있다.
 */

/*
 * 백엔드에 JSON 형식으로 요청하고,
 * JSON 형식의 응답을 받겠다는 HTTP Header다.
 */
const JSON_HEADERS = Object.freeze({
  Accept: 'application/json',
  'Content-Type': 'application/json',
})

/**
 * 사용자의 사고 내용을 판례 검색형 챗봇 API로 전송한다.
 *
 * 처리 흐름:
 * 1. 사용자가 Vue 화면에 사고 내용을 입력한다.
 * 2. Vue가 Spring Boot에 JSON 요청을 보낸다.
 * 3. Spring Boot가 Ollama로 질문 벡터를 생성한다.
 * 4. Oracle VECTOR 검색으로 유사 판례를 찾는다.
 * 5. 챗봇 안내 문장과 관련 판례를 JSON으로 반환한다.
 *
 * @param {string} message 사용자가 입력한 사고 또는 질병 내용
 * @returns {Promise<object>} 챗봇 답변과 관련 판례 목록
 */
export async function askPrecedentChat(message) {
  /*
   * Vite 개발 서버가 /api 요청을
   * http://localhost:8080으로 전달한다.
   *
   * 브라우저에서는 5173 포트만 사용하므로
   * 별도의 CORS 설정 없이 백엔드 API를 호출할 수 있다.
   */
  const response = await fetch(
    '/api/local/chat/precedents',
    {
      method: 'POST',
      headers: JSON_HEADERS,

      /*
       * JavaScript 객체는 그대로 HTTP 요청 본문에 넣을 수 없다.
       * JSON.stringify를 사용하여 JSON 문자열로 변환한다.
       *
       * 변환 결과 예:
       * {
       *   "message": "공사장에서 쇠파이프에 무릎을 다쳤습니다."
       * }
       */
      body: JSON.stringify({
        message,
      }),
    },
  )

  /*
   * fetch는 HTTP 상태가 400 또는 500이어도
   * 자동으로 예외를 발생시키지 않는다.
   *
   * 따라서 response.ok를 직접 확인해야 한다.
   */
  if (!response.ok) {
    let errorMessage =
      '판례 안내 답변을 불러오지 못했습니다.'

    try {
      const errorResponse = await response.json()

      /*
       * 백엔드가 제공한 안전한 사용자용 오류 메시지가 있으면
       * 기본 메시지 대신 사용한다.
       */
      if (
        typeof errorResponse.message === 'string' &&
        errorResponse.message.trim()
      ) {
        errorMessage = errorResponse.message
      }

      /*
       * Bean Validation 오류처럼 필드별 오류가 있는 경우
       * 첫 번째 검증 메시지를 사용자에게 보여준다.
       */
      if (
        Array.isArray(errorResponse.errors) &&
        errorResponse.errors.length > 0 &&
        typeof errorResponse.errors[0]?.message === 'string'
      ) {
        errorMessage = errorResponse.errors[0].message
      }
    } catch {
      /*
       * 백엔드가 JSON이 아닌 빈 응답 또는 HTML을 반환하면
       * 위에서 설정한 기본 오류 문구를 사용한다.
       */
    }

    throw new Error(errorMessage)
  }

  /*
   * 성공 응답의 JSON을 JavaScript 객체로 변환해서 반환한다.
   */
  return response.json()
}
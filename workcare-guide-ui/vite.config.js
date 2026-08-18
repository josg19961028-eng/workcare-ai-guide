import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

/*
 * Vite 개발 서버 설정이다.
 *
 * 개발 중 Vue는 5173 포트, Spring Boot는 8080 포트에서 실행된다.
 * 브라우저가 /api로 요청하면 Vite가 Spring Boot로 대신 전달한다.
 */
export default defineConfig({
  plugins: [
    /*
     * Vue의 .vue 단일 파일 컴포넌트를 해석한다.
     */
    vue(),

    /*
     * 개발 중 Vue 컴포넌트와 상태를 확인하는 도구다.
     */
    vueDevTools(),
  ],

  resolve: {
    alias: {
      /*
       * 앞으로 '@/views/...'처럼 src 폴더를 @로 표현할 수 있다.
       */
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  server: {
    /*
     * Vue 개발 서버 포트를 5173으로 고정한다.
     */
    port: 5173,

    /*
     * 5173 포트가 이미 사용 중이면 임의의 다른 포트로 실행하지 않고
     * 오류를 발생시킨다. 접속 주소가 달라지는 혼란을 방지한다.
     */
    strictPort: true,

    proxy: {
      /*
       * 브라우저에서 /api로 시작하는 모든 요청을
       * Spring Boot 서버로 전달한다.
       *
       * 예:
       * 브라우저 요청
       * http://localhost:5173/api/rehabilitation-institutions
       *
       * 실제 전달 주소
       * http://localhost:8080/api/rehabilitation-institutions
       */
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
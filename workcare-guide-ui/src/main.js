/*
 * Vue 애플리케이션을 생성하는 함수다.
 */
import { createApp } from 'vue'

/*
 * 모든 화면의 공통 틀 역할을 하는 최상위 컴포넌트다.
 */
import App from './App.vue'

/*
 * URL에 따라 표시할 화면을 결정하는 Vue Router 설정이다.
 */
import router from './router'

/*
 * 글꼴, 색상, 여백 등 애플리케이션 전체에 적용할 공통 CSS다.
 */
import './assets/main.css'

/*
 * App 컴포넌트를 기준으로 Vue 애플리케이션을 생성한다.
 */
const app = createApp(App)

/*
 * Vue 애플리케이션에 Router 기능을 등록한다.
 *
 * 이 설정이 있어야 RouterLink와 RouterView를 사용할 수 있다.
 */
app.use(router)

/*
 * index.html 안의 id="app" 요소에 Vue 애플리케이션을 연결한다.
 */
app.mount('#app')
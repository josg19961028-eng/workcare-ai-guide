/*
 * createRouter:
 * Vue 애플리케이션의 Router 객체를 생성한다.
 *
 * createWebHistory:
 * 주소에 #을 사용하지 않는 일반적인 URL 방식을 사용한다.
 */
import { createRouter, createWebHistory } from 'vue-router'

/*
 * 메인 주소에서 보여줄 화면 컴포넌트다.
 */
import HomeView from '../views/HomeView.vue'
import RehabilitationInstitutionView from '@/views/RehabilitationInstitutionView.vue'
import MedicalInstitutionView from '@/views/MedicalInstitutionView.vue'
import PrecedentSearchView from '@/views/PrecedentSearchView.vue'
/*
 * 산재 판례 안내 챗봇 화면이다.
 */
import PrecedentChatView from '@/views/PrecedentChatView.vue'
/*
 * Vue Router를 생성한다.
 */
const router = createRouter({
  /*
   * Vite의 기본 배포 경로를 기준으로 브라우저 주소를 관리한다.
   */
  history: createWebHistory(import.meta.env.BASE_URL),

  /*
   * URL과 Vue 화면을 연결한다.
   *
   * Spring의 @GetMapping과 비슷하게 생각할 수 있지만,
   * Router는 서버 API가 아니라 브라우저 화면 이동을 담당한다.
   */
  routes: [
    {
      /*
       * 사용자가 http://localhost:5173/으로 접속했을 때 적용된다.
       */
      path: '/',

      /*
       * 코드에서 이 화면을 식별할 때 사용할 이름이다.
       */
      name: 'home',

      /*
       * 해당 주소에서 HomeView 컴포넌트를 화면에 출력한다.
       */
      component: HomeView,

      /*
       * 페이지 제목처럼 화면 부가정보를 저장할 수 있다.
       */
      meta: {
        title: 'WorkCare Guide',
      },
    },

	{
		path: '/rehabilitation-institutions',
		name: 'rehabilitation-institutions',
		component: RehabilitationInstitutionView,
		meta: {
			title:'사회복귀 지원기관 찾기 | WorkCare Guide',
		},
	},
	{
		/*
		 * 산재지정 의료기관 조회 화면 주소다.
		 */
		path: '/medical-institutions',
		name: 'medical-institutions',
		component: MedicalInstitutionView,
		meta: {
		  title: '산재지정 의료기관 찾기 | WorkCare Guide',
		},
	},
	{
	  /*
	   * 산재보험 판례 검색 화면 주소다.
	   */
	  path: '/precedents',
	  name: 'precedents',
	  component: PrecedentSearchView,
	  meta: {
	    title: '산재보험 판례 검색 | WorkCare Guide',
	  },
	},
	{
	  /*
	   * 벡터 검색 기반 산재 판례 안내 챗봇 화면 주소다.
	   */
	  path: '/precedent-chat',

	  /*
	   * 코드에서 해당 화면을 식별할 때 사용하는 이름이다.
	   */
	  name: 'precedent-chat',

	  /*
	   * 이 주소로 접근하면 PrecedentChatView를 출력한다.
	   */
	  component: PrecedentChatView,

	  /*
	   * 브라우저 탭에 표시할 제목이다.
	   */
	  meta: {
	    title: '산재 판례 안내 챗봇 | WorkCare Guide',
	  },
	},
  ],
})

/*
 * 화면 이동이 완료될 때마다 브라우저 탭 제목을 변경한다.
 */
router.afterEach((to) => {
  /*
   * 해당 Route에 제목이 있으면 그 값을 사용하고,
   * 없으면 기본 서비스명을 사용한다.
   */
  document.title = to.meta.title || 'WorkCare Guide'
})

export default router
<script setup>
/*
 * 현재 메인 화면은 고정된 안내 내용을 출력하므로
 * 별도의 반응형 변수나 API 호출 코드가 필요하지 않다.
 *
 * 다음 단계의 재활기관 목록 화면에서는 ref, onMounted 등을 사용해
 * Spring API의 데이터를 조회할 예정이다.
 */
import { RouterLink } from 'vue-router';
</script>

<template>
  <!-- 서비스의 목적을 첫 화면에서 설명하는 대표 영역이다. -->
  <section class="hero-section">
    <div class="page-container hero-section__inner">
      <div class="hero-section__content">
        <p class="eyebrow">PUBLIC DATA · WORKCARE INFORMATION</p>

        <h1>
          흩어진 산재 지원정보를
          <span>한곳에서 쉽게 확인하세요</span>
        </h1>

		<p class="hero-section__description">
		  근로복지공단 공공데이터를 활용하여 산재 지정 의료기관부터
		  재활·직업복귀 지원정보까지 사용자가 이해하기 쉬운 형태로
		  제공하는 통합 안내 서비스입니다.
		</p>

        <div class="hero-section__actions">
          <!-- 아직 별도 화면이 없으므로 현재 페이지의 주요 기능 영역으로 이동한다. -->
          <a class="primary-button" href="#services">서비스 살펴보기</a>

          <span class="data-status">
            <span class="data-status__indicator" aria-hidden="true"></span>
            공공데이터 연동 정상
          </span>
        </div>
      </div>

      <!-- 서비스 처리 흐름을 시각적으로 요약한다. -->
      <aside class="hero-summary" aria-label="서비스 데이터 처리 흐름">
        <p class="hero-summary__label">서비스 처리 흐름</p>

        <ol class="process-list">
          <li>
            <span>01</span>
            <div>
              <strong>공공데이터 조회</strong>
              <p>근로복지공단 Open API 연동</p>
            </div>
          </li>

          <li>
            <span>02</span>
            <div>
              <strong>안전한 데이터 변환</strong>
              <p>외부 XML을 서비스 JSON으로 변환</p>
            </div>
          </li>

          <li>
            <span>03</span>
            <div>
              <strong>사용자 맞춤 안내</strong>
              <p>Vue 화면과 챗봇을 통한 정보 제공</p>
            </div>
          </li>
        </ol>
      </aside>
    </div>
  </section>

  <!-- 프로젝트에서 제공할 주요 기능을 소개한다. -->
  <section id="services" class="service-section">
    <div class="page-container">
      <div class="section-heading">
        <p class="eyebrow">MAIN SERVICES</p>
        <h2>주요 서비스</h2>
        <p>공공데이터를 사용자가 활용하기 쉬운 서비스로 변환합니다.</p>
      </div>

      <div class="service-grid">
        <!-- 현재 백엔드 API 구현이 완료된 기능이다. -->
        <article class="service-card service-card--active">
          <div class="service-card__top">
            <span class="service-card__number">01</span>
            <span class="status-badge status-badge--ready">API 연동 완료</span>
          </div>

		  <h3>사회복귀 지원기관 찾기</h3>

		  <p>
		    직업훈련, 재활스포츠 및 심리재활 프로그램을 제공하는
		    산재재활 위탁기관의 주소와 연락처를 조회합니다.
		  </p>
		  <RouterLink
		    class="service-card__action service-card__link"
		    to="/rehabilitation-institutions"
		  >
		    사회복귀 지원기관 찾기
		    <span aria-hidden="true">→</span>
		  </RouterLink>

        </article>

		<!-- 산재지정 의료기관 API 연동이 완료된 기능이다. -->
		<article class="service-card service-card--active">
		  <div class="service-card__top">
		    <span class="service-card__number">02</span>

		    <span class="status-badge status-badge--ready">
		      API 연동 완료
		    </span>
		  </div>

		  <h3>산재 지정 의료기관 찾기</h3>

		  <p>
		    산재보험 지정 병원·의원의 주소, 연락처 및 관할 지사 정보를
		    근로복지공단 공공데이터를 통해 조회합니다.
		  </p>

		  <RouterLink
		    class="service-card__action service-card__link"
		    to="/medical-institutions"
		  >
		    산재 지정 의료기관 찾기
		    <span aria-hidden="true">→</span>
		  </RouterLink>
		</article>

		<!-- 산재보험 판례 API 연동이 완료된 기능이다. -->
		<article class="service-card service-card--active">
		  <div class="service-card__top">
		    <span class="service-card__number">03</span>

		    <span class="status-badge status-badge--ready">
		      API 연동 완료
		    </span>
		  </div>

		  <h3>산재보험 판례 검색</h3>

		  <p>
		    판결결과, 사건유형 및 사고·질병 구분을 선택하여
		    근로복지공단이 제공하는 산재보험 판례와 판결문을 조회합니다.
		  </p>

		  <RouterLink
		    class="service-card__action service-card__link"
		    to="/precedents"
		  >
		    산재보험 판례 검색
		    <span aria-hidden="true">→</span>
		  </RouterLink>
		</article>

		<!--
		  Oracle VECTOR 검색과 Ollama 임베딩을 이용한
		  산재 판례 안내 챗봇 기능이다.
		-->
		<article class="service-card service-card--active">
		  <div class="service-card__top">
		    <span class="service-card__number">04</span>

		    <span class="status-badge status-badge--ready">
		      벡터 검색 구현 완료
		    </span>
		  </div>

		  <h3>산재 판례 안내 챗봇</h3>

		  <p>
		    사용자가 입력한 사고 상황을 벡터로 변환하고,
		    Oracle에 저장된 판결문 중 의미적으로 가까운
		    산재보험 판례와 검색 근거를 안내합니다.
		  </p>

		  <RouterLink
		    class="service-card__action service-card__link"
		    to="/precedent-chat"
		  >
		    판례 챗봇 이용하기
		    <span aria-hidden="true">→</span>
		  </RouterLink>
		</article>
      </div>
    </div>
  </section>

  <!-- 포트폴리오에서 기술 선택 이유를 보여주는 영역이다. -->
  <section class="architecture-section">
    <div class="page-container architecture-section__inner">
      <div>
        <p class="eyebrow">ARCHITECTURE</p>
        <h2>화면과 업무 로직을 분리했습니다</h2>
      </div>

      <div class="architecture-flow" aria-label="프로젝트 구성">
        <span>Vue 3</span>
        <strong aria-hidden="true">→</strong>
        <span>eGovFrame Boot</span>
        <strong aria-hidden="true">→</strong>
        <span>공공데이터 API</span>
      </div>
    </div>
  </section>
</template>
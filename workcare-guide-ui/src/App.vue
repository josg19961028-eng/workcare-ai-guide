<script setup>
/*
 * RouterLink:
 * 페이지 전체를 새로 불러오지 않고 Vue 화면을 이동시킨다.
 *
 * RouterView:
 * 현재 URL에 해당하는 화면 컴포넌트가 표시되는 위치다.
 */
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <!--
    키보드 사용자가 반복되는 메뉴를 건너뛰고
    본문으로 바로 이동할 수 있게 하는 접근성 링크다.

    평소에는 화면 밖에 있다가 Tab 키로 선택하면 표시된다.
  -->
  <a class="skip-link" href="#main-content">본문 바로가기</a>

  <div class="app-shell">
    <!-- 모든 페이지에서 공통으로 표시되는 상단 영역이다. -->
    <header class="site-header">
      <div class="site-header__inner">
        <!--
          서비스명이나 로고를 누르면 메인 화면으로 이동한다.
          RouterLink는 일반적인 a 태그와 달리 화면 전체를 새로고침하지 않는다.
        -->
        <RouterLink class="brand" to="/" aria-label="WorkCare Guide 메인으로 이동">
          <span class="brand__mark" aria-hidden="true">W</span>

          <span class="brand__text">
            <strong>WorkCare Guide</strong>
            <small>산재 지원정보 안내 서비스</small>
          </span>
        </RouterLink>

        <!-- 현재 구현된 화면으로 이동하는 주 메뉴다. -->
        <nav class="main-navigation" aria-label="주요 메뉴">
          <RouterLink to="/">서비스 소개</RouterLink>
          <RouterLink to="/rehabilitation-institutions">사회복귀 지원기관</RouterLink>
		  <RouterLink to="/medical-institutions">산재 지정 의료기관</RouterLink>
		  <RouterLink to="/precedents">산재 판례</RouterLink>
		  <RouterLink to="/precedent-chat">판례 챗봇</RouterLink>
        </nav>
      </div>
    </header>

    <!--
      Router가 현재 주소에 해당하는 View를 이 위치에 출력한다.
      현재 / 주소에서는 HomeView가 표시된다.
    -->
    <main id="main-content" class="site-main">
      <RouterView />
    </main>

    <!-- 모든 페이지에서 공통으로 표시되는 하단 영역이다. -->
    <footer class="site-footer">
      <div class="site-footer__inner">
        <strong>WorkCare Guide</strong>

        <p>
          공공데이터를 활용한 학습용 프로젝트이며, 근로복지공단의 공식 서비스가 아닙니다.
        </p>

        <p class="site-footer__copyright">
          공공데이터 기반 산재 지원정보 탐색 포트폴리오
        </p>
      </div>
    </footer>
  </div>
</template>
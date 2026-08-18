<script setup>
import { computed, onMounted, ref } from 'vue'

/*
 * 조회 유형별 설정이다.
 *
 * API 주소, 응답 목록 필드, 이름 필드처럼 유형마다 다른 값을
 * 한곳에서 관리한다.
 *
 * Object.freeze를 사용해 실행 중 설정이 의도치 않게 변경되는 것을 막는다.
 */
const FACILITY_TYPES = Object.freeze({
	institution: Object.freeze({
		displayName: '산재지정 의료기관',
		countUnit: '의료기관',
		apiPath: '/api/medical-institutions',
		responseListField: 'institutions',
		responseNameField: 'hospitalName',
	}),

	pharmacy: Object.freeze({
		displayName: '산재지정 약국',
		countUnit: '약국',
		apiPath: '/api/medical-institutions/pharmacies',
		responseListField: 'pharmacies',
		responseNameField: 'pharmacyName',
	}),

	certified: Object.freeze({
		displayName: '재활인증 의료기관',
		countUnit: '의료기관',
		apiPath:
			'/api/medical-institutions/certified-rehabilitation',
		responseListField: 'institutions',
		responseNameField: 'hospitalName',
	}),
})

/*
 * 현재 선택한 조회 유형이다.
 *
 * institution: 산재지정 의료기관
 * pharmacy:    산재지정 약국
 *
 * 서버로부터 받은 값을 그대로 사용하지 않고
 * 코드 내부에서 정한 두 값만 사용하므로 임의의 API 경로가
 * 만들어지는 것을 방지할 수 있다.
 */
const activeType = ref('institution')

/*
 * 현재 선택한 유형의 조회 결과 목록이다.
 *
 * 의료기관과 약국의 필드명을 화면용 공통 구조로 변환해 저장한다.
 */
const facilities = ref([])

/*
 * 현재 선택한 유형의 전체 데이터 수다.
 */
const totalCount = ref(0)

/*
 * 현재 조회 중인 페이지 번호다.
 *
 * 탭을 변경하면 첫 페이지로 초기화한다.
 */
const currentPage = ref(1)

/*
 * API 요청 진행 여부다.
 *
 * 조회 중 탭과 페이지 버튼의 중복 클릭을 차단한다.
 */
const isLoading = ref(false)

/*
 * 사용자에게 표시할 안전한 오류 메시지다.
 */
const errorMessage = ref('')

/*
 * 한 페이지에서 조회할 데이터 수다.
 */
const pageSize = 6

/*
 * 현재 선택된 유형의 설정이다.
 *
 * activeType이 변경되면 해당 설정도 자동으로 변경된다.
 */
const activeTypeConfig = computed(() => {
	return FACILITY_TYPES[activeType.value]
})

/*
 * 현재 재활인증 의료기관 탭인지 확인한다.
 *
 * 재활인증기관에서만 기관번호와 종별명을 표시할 때 사용한다.
 */
const isCertified = computed(() => {
	return activeType.value === 'certified'
})

/*
 * 현재 선택된 유형의 화면 표시 이름이다.
 */
const activeTypeName = computed(() => {
	return activeTypeConfig.value.displayName
})

/*
 * 전체 건수 뒤에 표시할 단위다.
 */
const activeCountUnit = computed(() => {
	return activeTypeConfig.value.countUnit
})

/*
 * 현재 선택된 탭의 HTML id다.
 *
 * 탭과 결과 패널을 접근성 속성으로 연결할 때 사용한다.
 */
const activeTabId = computed(() => {
	return `${activeType.value}-tab`
})

/*
 * 전체 데이터 수와 한 페이지 결과 수로 전체 페이지를 계산한다.
 */
const totalPages = computed(() => {
	return Math.max(
		1,
		Math.ceil(totalCount.value / pageSize),
	)
})

/**
 * 현재 선택한 탭에 맞는 Spring Boot API 주소를 만든다.
 *
 * @returns {string} 백엔드 API 요청 주소
 */
function createRequestUrl() {
	/*
	 * 사용자 입력값으로 경로를 조합하지 않고
	 * FACILITY_TYPES에 미리 등록한 API 주소만 사용한다.
	 *
	 * 예상하지 않은 내부 또는 외부 주소가 만들어지는 것을 방지한다.
	 */
	const apiPath = activeTypeConfig.value.apiPath

	return (
		apiPath +
		`?page=${encodeURIComponent(currentPage.value)}` +
		`&size=${encodeURIComponent(pageSize)}`
	)
}

/**
 * 백엔드 응답을 화면 공통 구조로 변환한다.
 *
 * @param {object} responseData 백엔드 JSON 응답
 * @returns {Array<object>} 화면에서 사용할 시설 목록
 */
function convertFacilities(responseData) {
	/*
	 * 현재 유형에 맞는 응답 배열 필드명을 설정에서 가져온다.
	 *
	 * 의료기관·재활인증기관: institutions
	 * 약국: pharmacies
	 */
	const listField =
		activeTypeConfig.value.responseListField

	/*
	 * 현재 유형에 맞는 이름 필드명을 가져온다.
	 *
	 * 의료기관·재활인증기관: hospitalName
	 * 약국: pharmacyName
	 */
	const nameField =
		activeTypeConfig.value.responseNameField

	const sourceItems = responseData[listField]

	/*
	 * 예상한 배열이 아니면 Vue 반복 출력이 실패하지 않도록
	 * 빈 배열로 처리한다.
	 */
	if (!Array.isArray(sourceItems)) {
		return []
	}

	/*
	 * 세 API의 응답을 화면에서 사용할 공통 구조로 변환한다.
	 *
	 * hospitalNumber와 institutionTypeName은
	 * 재활인증 의료기관에만 존재하며 나머지 유형에서는 null이 된다.
	 */
	return sourceItems.map((item) => ({
		name: item[nameField],
		hospitalNumber: item.hospitalNumber ?? null,
		institutionTypeName:
			item.institutionTypeName ?? null,
		managingBranchCode:
			item.managingBranchCode,
		managingBranchName:
			item.managingBranchName,
		address: item.address,
		telephoneNumber: item.telephoneNumber,
		faxNumber: item.faxNumber,
	}))
}

/**
 * 현재 탭의 목록을 조회한다.
 */
async function loadFacilities() {
	errorMessage.value = ''
	isLoading.value = true

	try {
		/*
		 * Vue는 공공데이터 인증키를 보유하지 않는다.
		 *
		 * Vue → Spring Boot → 근로복지공단 순서로 요청하여
		 * 브라우저 개발자 도구에 인증키가 노출되지 않게 한다.
		 */
		const response = await fetch(createRequestUrl(), {
			method: 'GET',
			headers: {
				Accept: 'application/json',
			},
		})

		/*
		 * fetch는 HTTP 400·500·502를 자동으로 예외 처리하지 않으므로
		 * response.ok를 직접 확인한다.
		 */
		if (!response.ok) {
			let message =
				`${activeTypeName.value} 정보를 불러오지 못했습니다. ` +
				'잠시 후 다시 시도해주세요.'

			try {
				/*
				 * 백엔드의 공통 JSON 오류 응답을 읽는다.
				 */
				const errorResponse = await response.json()

				if (errorResponse.message) {
					message = errorResponse.message
				}
			} catch {
				/*
				 * JSON이 아닌 오류 응답이 와도 기본 메시지를 사용한다.
				 */
			}

			throw new Error(message)
		}

		const responseData = await response.json()

		/*
		 * 의료기관과 약국 응답을 화면용 공통 구조로 변환한다.
		 */
		facilities.value = convertFacilities(responseData)

		totalCount.value =
			Number(responseData.totalCount) || 0

		/*
		 * 백엔드에서 반환한 페이지 번호가 정상일 때만 반영한다.
		 */
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
				: `${activeTypeName.value} 정보를 불러오는 중 오류가 발생했습니다.`

		/*
		 * 실패한 조회의 이전 결과가 화면에 남지 않도록 초기화한다.
		 */
		facilities.value = []
		totalCount.value = 0

		/*
		 * 인증키나 외부 XML 원문은 Console에 기록하지 않는다.
		 */
		console.error(
			`${activeTypeName.value} 목록 조회 실패`,
			error,
		)
	} finally {
		isLoading.value = false
	}
}

/**
 * 의료기관, 약국 또는 재활인증기관 탭으로 전환한다.
 *
 * @param {'institution'|'pharmacy'|'certified'} targetType
 * 선택할 조회 유형
 */
async function changeType(targetType) {
	/*
	 * FACILITY_TYPES에 등록된 유형만 허용한다.
	 *
	 * 허용 목록 방식으로 검증하면 새로운 임의 문자열이
	 * API 요청 경로로 사용되는 것을 방지할 수 있다.
	 */
	if (!Object.hasOwn(FACILITY_TYPES, targetType)) {
		return
	}

	/*
	 * 로딩 중이거나 이미 선택한 탭이면 다시 요청하지 않는다.
	 */
	if (
		isLoading.value ||
		activeType.value === targetType
	) {
		return
	}

	activeType.value = targetType

	/*
	 * 다른 유형으로 전환할 때는 첫 페이지부터 조회한다.
	 */
	currentPage.value = 1
	facilities.value = []
	totalCount.value = 0
	errorMessage.value = ''

	await loadFacilities()
}

/**
 * 지정한 페이지로 이동한다.
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

	await loadFacilities()

	document
		.getElementById('medical-facility-results')
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
 * 화면이 처음 표시되면 의료기관 첫 페이지를 조회한다.
 */
onMounted(() => {
	loadFacilities()
})
</script>

<template>
	<section class="subpage-heading">
		<div class="page-container">
			<p class="eyebrow">
				INDUSTRIAL ACCIDENT MEDICAL CARE
			</p>

			<h1>산재 의료기관 찾기</h1>

			<p>
				근로복지공단 공공데이터를 이용하여 산재보험 지정
				병원·의원과 약국, 재활인증 의료기관의 주소,
				연락처 및 관할 지사 정보를 제공합니다.
			</p>
		</div>
	</section>

	<section id="medical-facility-results" class="institution-section">
		<div class="page-container">
			<!--
        의료기관과 약국 조회 유형을 선택하는 탭이다.
      -->
			<div class="lookup-tabs" role="tablist" aria-label="산재 의료기관 조회 유형">
				<button id="institution-tab" class="lookup-tabs__button" :class="{
					'lookup-tabs__button--active':
						activeType === 'institution',
				}" type="button" role="tab" :aria-selected="activeType === 'institution'
					" aria-controls="medical-facility-panel" :disabled="isLoading" @click="changeType('institution')">
					산재지정 의료기관
				</button>

				<button id="pharmacy-tab" class="lookup-tabs__button" :class="{
					'lookup-tabs__button--active':
						activeType === 'pharmacy',
				}" type="button" role="tab" :aria-selected="activeType === 'pharmacy'
					" aria-controls="medical-facility-panel" :disabled="isLoading" @click="changeType('pharmacy')">
					산재지정 약국
				</button>
				<!-- 재활치료 역량을 인증받은 의료기관 조회 탭이다. -->
				<button id="certified-tab" class="lookup-tabs__button" :class="{
					'lookup-tabs__button--active':
						activeType === 'certified',
				}" type="button" role="tab" :aria-selected="activeType === 'certified'
					" aria-controls="medical-facility-panel" :disabled="isLoading" @click="changeType('certified')">
					재활인증 의료기관
				</button>
			</div>

			<!-- 선택한 탭의 조회 결과 영역이다. -->
			<div id="medical-facility-panel" role="tabpanel" :aria-labelledby="activeTabId">
				<div class="institution-section__header">
					<div>
						<h2>{{ activeTypeName }} 조회 결과</h2>

						<p>
							근로복지공단 산재병원 의료 현황정보
							Open API 제공
						</p>
					</div>

					<span v-if="!isLoading && !errorMessage" class="result-count" aria-live="polite">
						전체 {{ totalCount.toLocaleString() }}개
						{{ activeCountUnit }}
					</span>
				</div>

				<!-- 조회 중 상태다. -->
				<div v-if="isLoading" class="state-panel" role="status" aria-live="polite">
					<span class="loading-spinner" aria-hidden="true"></span>

					<h3>
						{{ activeTypeName }} 정보를 불러오고 있습니다
					</h3>

					<p>
						공공데이터를 조회하는 동안 잠시 기다려주세요.
					</p>
				</div>

				<!-- 조회 실패 상태다. -->
				<div v-else-if="errorMessage" class="state-panel state-panel--error" role="alert">
					<span class="state-panel__symbol" aria-hidden="true">
						!
					</span>

					<h3>{{ activeTypeName }} 정보를 불러오지 못했습니다</h3>

					<p>{{ errorMessage }}</p>

					<button class="secondary-button" type="button" @click="loadFacilities">
						다시 시도
					</button>
				</div>

				<!-- 정상 응답이지만 결과가 없는 상태다. -->
				<div v-else-if="facilities.length === 0" class="state-panel">
					<span class="state-panel__symbol" aria-hidden="true">
						0
					</span>

					<h3>조회된 {{ activeTypeName }}이 없습니다</h3>

					<p>
						다른 페이지를 조회하거나 잠시 후 다시 확인해주세요.
					</p>
				</div>

				<!-- 정상 조회 결과다. -->
				<template v-else>
					<ul class="institution-grid">
						<li v-for="facility in facilities" :key="`${activeType}-${facility.hospitalNumber || ''}-${facility.name}-${facility.address}`
							" class="institution-card">
							<article>
								<div class="institution-card__top">
									<span class="institution-type">
										{{
											isCertified
												? facility.institutionTypeName ||
												'의료기관 종별 미제공'
												: activeTypeName
										}}
									</span>

									<span class="managing-branch">
										{{
											facility.managingBranchName ||
											'관할 지사 미제공'
										}}
									</span>
								</div>

								<h3>
									{{ facility.name || '기관명 미제공' }}
								</h3>

								<dl class="institution-details">
									<!--
					                    의료기관 식별번호는 재활인증기관 응답에서만 제공된다.
					                  -->
									<div v-if="isCertified">
										<dt>기관번호</dt>

										<dd>
											{{
												facility.hospitalNumber ||
												'기관번호 정보 없음'
											}}
										</dd>
									</div>
									<div>
										<dt>주소</dt>
										<dd>
											{{
												facility.address ||
												'주소 정보 없음'
											}}
										</dd>
									</div>

									<div>
										<dt>전화번호</dt>
										<dd>
											{{
												facility.telephoneNumber ||
												'전화번호 정보 없음'
											}}
										</dd>
									</div>

									<div>
										<dt>팩스번호</dt>
										<dd>
											{{
												facility.faxNumber ||
												'팩스번호 정보 없음'
											}}
										</dd>
									</div>

									<div>
										<dt>관할지사</dt>
										<dd>
											{{
												facility.managingBranchName ||
												'관할 지사 정보 없음'
											}}

											<span v-if="facility.managingBranchCode">
												({{ facility.managingBranchCode }})
											</span>
										</dd>
									</div>
								</dl>
							</article>
						</li>
					</ul>

					<nav class="pagination" :aria-label="`${activeTypeName} 페이지 이동`
						">
						<button class="pagination__button" type="button" :disabled="isLoading || currentPage <= 1
							" @click="goToPreviousPage">
							이전
						</button>

						<span class="pagination__status" aria-live="polite">
							{{ currentPage.toLocaleString() }} /
							{{ totalPages.toLocaleString() }} 페이지
						</span>

						<button class="pagination__button" type="button" :disabled="isLoading ||
							currentPage >= totalPages
							" @click="goToNextPage">
							다음
						</button>
					</nav>
				</template>
			</div>
		</div>
	</section>
</template>
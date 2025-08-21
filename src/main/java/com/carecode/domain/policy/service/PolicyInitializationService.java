package com.carecode.domain.policy.service;

import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.entity.PolicyCategory;
import com.carecode.domain.policy.repository.PolicyCategoryRepository;
import com.carecode.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 정책 관련 초기 데이터를 생성하는 서비스
 * 서버 시작 시 실제 대한민국 육아 정책 데이터를 자동으로 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Order(3) // CommunityInitializationService 다음에 실행
public class PolicyInitializationService implements CommandLineRunner {

    private final PolicyRepository policyRepository;
    private final PolicyCategoryRepository policyCategoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("정책 초기 데이터 생성 시작");
        
        try {
            // 카테고리 생성
            List<PolicyCategory> categories = createPolicyCategories();
            
            // 정책 생성 
            createPolicies(categories);
            
            // 최종 개수 확인
            long totalPolicies = policyRepository.count();
            long totalCategories = policyCategoryRepository.count();
            log.info("정책 초기 데이터 생성 완료 - 카테고리: {}개, 정책: {}개", totalCategories, totalPolicies);
        } catch (Exception e) {
            log.error("정책 초기 데이터 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 정책 카테고리 생성
     */
    private List<PolicyCategory> createPolicyCategories() {
        if (policyCategoryRepository.count() > 0) {
            log.info("정책 카테고리가 이미 존재하므로 생성을 건너뜁니다.");
            return policyCategoryRepository.findAll();
        }

        List<String> categoryData = Arrays.asList(
            "출산・육아휴직:출산 및 육아를 위한 휴직 관련 정책",
            "양육수당・보육료:자녀 양육을 위한 경제적 지원",
            "돌봄서비스:아이돌봄 및 육아 지원 서비스",
            "의료・건강:임신, 출산, 영유아 건강 관련 지원",
            "교육지원:영유아 교육 및 보육시설 이용 지원",
            "주거지원:육아가정을 위한 주거 관련 혜택",
            "다자녀혜택:다자녀 가정을 위한 특별 지원"
        );

        List<PolicyCategory> categories = categoryData.stream()
                .map(data -> {
                    String[] parts = data.split(":");
                    return PolicyCategory.builder()
                            .name(parts[0])
                            .description(parts[1])
                            .displayOrder(categoryData.indexOf(data) + 1)
                            .build();
                })
                .toList();

        List<PolicyCategory> savedCategories = policyCategoryRepository.saveAll(categories);
        log.info("{}개의 정책 카테고리 생성 완료", savedCategories.size());
        return savedCategories;
    }

    /**
     * 실제 대한민국 육아 정책 생성
     */
    private void createPolicies(List<PolicyCategory> categories) {
        if (policyRepository.count() > 0) {
            log.info("정책이 이미 존재하므로 생성을 건너뜁니다.");
            return;
        }

        // 카테고리별 정책 생성
        createMaternityAndChildcareLeave(categories.get(0)); // 출산・육아휴직
        createChildcareAllowances(categories.get(1)); // 양육수당・보육료
        createCareServices(categories.get(2)); // 돌봄서비스
        createHealthcareSupport(categories.get(3)); // 의료・건강
        createEducationSupport(categories.get(4)); // 교육지원
        createHousingSupport(categories.get(5)); // 주거지원
        createMultiChildBenefits(categories.get(6)); // 다자녀혜택

        log.info("정책 데이터 생성 완료");
    }

    /**
     * 출산・육아휴직 정책
     */
    private void createMaternityAndChildcareLeave(PolicyCategory category) {
        List<Policy> policies = Arrays.asList(
            Policy.builder()
                .policyCode("ML001")
                .title("출산전후휴가급여")
                .description("출산을 전후하여 90일의 휴가를 제공하며, 이 기간 동안 월 통상임금의 100%를 지급합니다. 출산 전 45일, 출산 후 45일로 나누어 사용할 수 있습니다.")
                .policyType("급여지원")
                .targetAgeMin(0)
                .targetAgeMax(0)
                .targetRegion("전국")
                .benefitType("월급여")
                .applicationUrl("https://www.ei.go.kr")
                .contactInfo("고용보험 고객상담센터 1350")
                .requiredDocuments("출산전후휴가 신청서, 의사소견서, 가족관계증명서")
                .isActive(true)
                .priority(1)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("CL001")
                .title("육아휴직급여")
                .description("만 8세 이하 또는 초등학교 2학년 이하 자녀를 양육하기 위해 육아휴직을 사용하는 근로자에게 월 통상임금의 80%(상한 150만원, 하한 70만원)를 지급합니다.")
                .policyType("급여지원")
                .targetAgeMin(0)
                .targetAgeMax(96)
                .targetRegion("전국")
                .benefitAmount(1500000)
                .benefitType("월급여")
                .applicationUrl("https://www.ei.go.kr")
                .contactInfo("고용보험 고객상담센터 1350")
                .requiredDocuments("육아휴직 신청서, 가족관계증명서, 주민등록등본")
                .isActive(true)
                .priority(2)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("AL001")
                .title("아빠육아휴직보너스")
                .description("같은 자녀에 대해 부모가 순차적으로 육아휴직을 사용하는 경우, 두 번째 사용자에게 첫 3개월간 통상임금의 100%(상한 250만원)를 지급합니다.")
                .policyType("급여지원")
                .targetAgeMin(0)
                .targetAgeMax(96)
                .targetRegion("전국")
                .benefitAmount(2500000)
                .benefitType("월급여")
                .applicationUrl("https://www.ei.go.kr")
                .contactInfo("고용보험 고객상담센터 1350")
                .requiredDocuments("육아휴직 신청서, 배우자 육아휴직 확인서")
                .isActive(true)
                .priority(3)
                .policyCategory(category)
                .build()
        );

        policyRepository.saveAll(policies);
        log.info("출산・육아휴직 정책 {}개 생성 완료", policies.size());
    }

    /**
     * 양육수당・보육료 정책
     */
    private void createChildcareAllowances(PolicyCategory category) {
        List<Policy> policies = Arrays.asList(
            Policy.builder()
                .policyCode("CA001")
                .title("아동수당")
                .description("만 7세 미만(0~83개월) 모든 아동에게 월 10만원의 아동수당을 지급합니다. 소득·재산 수준과 관계없이 보편적으로 지급됩니다.")
                .policyType("현금지원")
                .targetAgeMin(0)
                .targetAgeMax(83)
                .targetRegion("전국")
                .benefitAmount(100000)
                .benefitType("월지급")
                .applicationUrl("https://www.bokjiro.go.kr")
                .contactInfo("보건복지상담센터 129")
                .requiredDocuments("아동수당 신청서, 통장사본, 신분증")
                .isActive(true)
                .priority(1)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("PA001")
                .title("부모급여(0세)")
                .description("2023년 1월 이후 출생아 대상 생후 0~11개월 아동에게 월 70만원을 지급합니다. 어린이집·유치원 미이용 시에도 지급됩니다.")
                .policyType("현금지원")
                .targetAgeMin(0)
                .targetAgeMax(11)
                .targetRegion("전국")
                .benefitAmount(700000)
                .benefitType("월지급")
                .applicationUrl("https://www.bokjiro.go.kr")
                .contactInfo("보건복지상담센터 129")
                .requiredDocuments("부모급여 신청서, 통장사본, 가족관계증명서")
                .isActive(true)
                .priority(2)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("PA002")
                .title("부모급여(1세)")
                .description("2022년 1월 이후 출생아 대상 생후 12~23개월 아동에게 월 35만원을 지급합니다. 어린이집·유치원 미이용 시에도 지급됩니다.")
                .policyType("현금지원")
                .targetAgeMin(12)
                .targetAgeMax(23)
                .targetRegion("전국")
                .benefitAmount(350000)
                .benefitType("월지급")
                .applicationUrl("https://www.bokjiro.go.kr")
                .contactInfo("보건복지상담센터 129")
                .requiredDocuments("부모급여 신청서, 통장사본, 가족관계증명서")
                .isActive(true)
                .priority(3)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("CC001")
                .title("보육료 지원")
                .description("만 0~5세 영유아가 어린이집을 이용하는 경우 보육료를 지원합니다. 0세 514천원, 1세 452천원, 2세 375천원, 3~5세 280천원을 지원합니다.")
                .policyType("이용료지원")
                .targetAgeMin(0)
                .targetAgeMax(71)
                .targetRegion("전국")
                .benefitAmount(514000)
                .benefitType("월지원")
                .applicationUrl("https://www.bokjiro.go.kr")
                .contactInfo("보건복지상담센터 129")
                .requiredDocuments("보육료 지원 신청서, 소득증빙서류")
                .isActive(true)
                .priority(4)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("FM001")
                .title("첫만남이용권")
                .description("2022년 1월 1일 이후 출생아에게 200만원의 첫만남이용권(국민행복카드)을 지급하여 유아용품 구입을 지원합니다.")
                .policyType("바우처지원")
                .targetAgeMin(0)
                .targetAgeMax(12)
                .targetRegion("전국")
                .benefitAmount(2000000)
                .benefitType("일시지급")
                .applicationUrl("https://www.bokjiro.go.kr")
                .contactInfo("보건복지상담센터 129")
                .requiredDocuments("첫만남이용권 신청서, 출생신고서, 통장사본")
                .isActive(true)
                .priority(5)
                .policyCategory(category)
                .build()
        );

        policyRepository.saveAll(policies);
        log.info("양육수당・보육료 정책 {}개 생성 완료", policies.size());
    }

    /**
     * 돌봄서비스 정책
     */
    private void createCareServices(PolicyCategory category) {
        List<Policy> policies = Arrays.asList(
            Policy.builder()
                .policyCode("CS001")
                .title("아이돌봄서비스")
                .description("만 12세 이하 아동을 대상으로 아이돌보미가 가정을 방문하여 돌봄 서비스를 제공합니다. 시간제돌봄, 영아종일제돌봄, 질병감염아동특별지원 등이 있습니다.")
                .policyType("서비스지원")
                .targetAgeMin(0)
                .targetAgeMax(144)
                .targetRegion("전국")
                .benefitType("서비스제공")
                .applicationUrl("https://www.idolbom.go.kr")
                .contactInfo("아이돌봄서비스 상담센터 1577-2514")
                .requiredDocuments("아이돌봄서비스 신청서, 소득증빙서류, 건강보험료 납부확인서")
                .isActive(true)
                .priority(1)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("FCC001")
                .title("육아종합지원센터")
                .description("지역별 육아종합지원센터에서 육아정보 제공, 부모교육, 가족상담, 장난감·도서 대여 등 다양한 육아지원 서비스를 제공합니다.")
                .policyType("서비스지원")
                .targetAgeMin(0)
                .targetAgeMax(71)
                .targetRegion("전국")
                .benefitType("서비스제공")
                .applicationUrl("https://www.familynet.or.kr")
                .contactInfo("중앙육아종합지원센터 02-701-0431")
                .requiredDocuments("이용신청서, 신분증")
                .isActive(true)
                .priority(2)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("ES001")
                .title("시간연장형 보육서비스")
                .description("맞벌이 등 장시간 보육이 필요한 가정을 위해 어린이집에서 19:30까지 보육서비스를 제공합니다.")
                .policyType("서비스지원")
                .targetAgeMin(0)
                .targetAgeMax(71)
                .targetRegion("전국")
                .benefitType("서비스제공")
                .applicationUrl("https://www.childcare.go.kr")
                .contactInfo("보건복지상담센터 129")
                .requiredDocuments("시간연장보육 신청서, 재직증명서")
                .isActive(true)
                .priority(3)
                .policyCategory(category)
                .build()
        );

        policyRepository.saveAll(policies);
        log.info("돌봄서비스 정책 {}개 생성 완료", policies.size());
    }

    /**
     * 의료・건강 정책
     */
    private void createHealthcareSupport(PolicyCategory category) {
        List<Policy> policies = Arrays.asList(
            Policy.builder()
                .policyCode("HC001")
                .title("영유아 건강검진")
                .description("생후 14일부터 71개월까지 총 8회의 영유아 건강검진을 무료로 제공합니다. 성장발달평가, 건강교육, 상담 등을 포함합니다.")
                .policyType("의료지원")
                .targetAgeMin(0)
                .targetAgeMax(71)
                .targetRegion("전국")
                .benefitType("무료검진")
                .applicationUrl("https://www.nhis.or.kr")
                .contactInfo("국민건강보험공단 1577-1000")
                .requiredDocuments("건강보험증, 영유아 건강검진표")
                .isActive(true)
                .priority(1)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("VC001")
                .title("국가예방접종 지원")
                .description("만 12세 이하 어린이 대상 17종의 국가예방접종을 무료로 제공합니다. BCG, B형간염, DTaP, IPV, MMR, 수두, 일본뇌염 등이 포함됩니다.")
                .policyType("의료지원")
                .targetAgeMin(0)
                .targetAgeMax(144)
                .targetRegion("전국")
                .benefitType("무료접종")
                .applicationUrl("https://nip.kdca.go.kr")
                .contactInfo("질병관리청 예방접종 콜센터 1339")
                .requiredDocuments("건강보험증, 예방접종수첩")
                .isActive(true)
                .priority(2)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("MC001")
                .title("임신・출산 진료비 지원")
                .description("임신 확인 시부터 분만 예정일까지 임신・출산 관련 진료비 100만원을 국민행복카드로 지원합니다. 다태아의 경우 140만원을 지원합니다.")
                .policyType("의료지원")
                .targetAgeMin(0)
                .targetAgeMax(0)
                .targetRegion("전국")
                .benefitAmount(1000000)
                .benefitType("바우처지원")
                .applicationUrl("https://www.nhis.or.kr")
                .contactInfo("국민건강보험공단 1577-1000")
                .requiredDocuments("임신확인서, 국민행복카드 신청서")
                .isActive(true)
                .priority(3)
                .policyCategory(category)
                .build()
        );

        policyRepository.saveAll(policies);
        log.info("의료・건강 정책 {}개 생성 완료", policies.size());
    }

    /**
     * 교육지원 정책
     */
    private void createEducationSupport(PolicyCategory category) {
        List<Policy> policies = Arrays.asList(
            Policy.builder()
                .policyCode("ED001")
                .title("누리과정 지원")
                .description("만 3~5세 유아에게 유치원·어린이집 구분없이 공통 교육과정인 누리과정을 제공하고 이용료를 지원합니다.")
                .policyType("교육지원")
                .targetAgeMin(36)
                .targetAgeMax(71)
                .targetRegion("전국")
                .benefitAmount(280000)
                .benefitType("월지원")
                .applicationUrl("https://www.childcare.go.kr")
                .contactInfo("보건복지상담센터 129")
                .requiredDocuments("누리과정 지원 신청서")
                .isActive(true)
                .priority(1)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("LIB001")
                .title("도서관 프로그램 지원")
                .description("전국 공공도서관에서 영유아 대상 독서프로그램, 책읽기 모임, 문화행사 등을 무료로 제공합니다.")
                .policyType("교육지원")
                .targetAgeMin(0)
                .targetAgeMax(71)
                .targetRegion("전국")
                .benefitType("무료프로그램")
                .applicationUrl("각 지역 도서관 홈페이지")
                .contactInfo("해당 지역 도서관")
                .requiredDocuments("도서관 회원가입")
                .isActive(true)
                .priority(2)
                .policyCategory(category)
                .build()
        );

        policyRepository.saveAll(policies);
        log.info("교육지원 정책 {}개 생성 완료", policies.size());
    }

    /**
     * 주거지원 정책
     */
    private void createHousingSupport(PolicyCategory category) {
        List<Policy> policies = Arrays.asList(
            Policy.builder()
                .policyCode("HS001")
                .title("신혼희망타운")
                .description("신혼부부 및 예비신혼부부를 대상으로 시세보다 저렴한 분양주택과 임대주택을 공급합니다.")
                .policyType("주거지원")
                .targetAgeMin(0)
                .targetAgeMax(240)
                .targetRegion("전국")
                .benefitType("주택공급")
                .applicationUrl("https://www.lh.or.kr")
                .contactInfo("한국토지주택공사 1600-1004")
                .requiredDocuments("혼인관계증명서, 소득증빙서류")
                .isActive(true)
                .priority(1)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("HS002")
                .title("다자녀 특별공급")
                .description("만 6세 이하 자녀 2명 이상 또는 만 18세 이하 자녀 3명 이상 가구에 신축 분양주택 특별공급 기회를 제공합니다.")
                .policyType("주거지원")
                .targetAgeMin(0)
                .targetAgeMax(216)
                .targetRegion("전국")
                .benefitType("특별공급")
                .applicationUrl("https://www.applyhome.co.kr")
                .contactInfo("청약홈 고객센터 1644-7445")
                .requiredDocuments("가족관계증명서, 혼인관계증명서")
                .isActive(true)
                .priority(2)
                .policyCategory(category)
                .build()
        );

        policyRepository.saveAll(policies);
        log.info("주거지원 정책 {}개 생성 완료", policies.size());
    }

    /**
     * 다자녀혜택 정책
     */
    private void createMultiChildBenefits(PolicyCategory category) {
        List<Policy> policies = Arrays.asList(
            Policy.builder()
                .policyCode("MB001")
                .title("다자녀카드")
                .description("만 18세 미만 자녀 2명 이상 가구에 다자녀카드를 발급하여 전국 가맹점에서 할인 혜택을 제공합니다.")
                .policyType("할인혜택")
                .targetAgeMin(0)
                .targetAgeMax(216)
                .targetRegion("전국")
                .benefitType("할인카드")
                .applicationUrl("https://www.i-love.or.kr")
                .contactInfo("한국건강가정진흥원 1577-9337")
                .requiredDocuments("다자녀카드 신청서, 가족관계증명서")
                .isActive(true)
                .priority(1)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("MB002")
                .title("대중교통 요금할인")
                .description("다자녀 가정의 만 6~18세 자녀에게 지하철, 버스 등 대중교통 요금 할인 혜택을 제공합니다. (지역별 상이)")
                .policyType("교통혜택")
                .targetAgeMin(72)
                .targetAgeMax(216)
                .targetRegion("지역별")
                .benefitType("요금할인")
                .applicationUrl("해당 지자체 홈페이지")
                .contactInfo("해당 지자체 문의")
                .requiredDocuments("다자녀 확인서, 학생증")
                .isActive(true)
                .priority(2)
                .policyCategory(category)
                .build(),

            Policy.builder()
                .policyCode("MB003")
                .title("국공립시설 이용료 할인")
                .description("자녀 3명 이상 다자녀 가정에 국공립 박물관, 미술관, 공원, 체육시설 등의 이용료 할인 혜택을 제공합니다.")
                .policyType("문화혜택")
                .targetAgeMin(0)
                .targetAgeMax(216)
                .targetRegion("전국")
                .benefitType("이용료할인")
                .applicationUrl("각 시설 홈페이지")
                .contactInfo("해당 시설 문의")
                .requiredDocuments("다자녀 확인서, 가족관계증명서")
                .isActive(true)
                .priority(3)
                .policyCategory(category)
                .build()
        );

        policyRepository.saveAll(policies);
        log.info("다자녀혜택 정책 {}개 생성 완료", policies.size());
    }
}

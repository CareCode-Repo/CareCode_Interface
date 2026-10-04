package com.carecode.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 구조 규칙을 테스트로 고정한다.
 *
 * <p>이 프로젝트가 지저분해진 방식은 "로직이 엉킴" 이 아니라 **같은 일을 여러 방식으로 하게 된 것**이다.
 * 예외 체계가 셋이고, 소유권 검증이 세 군데 있고, 도메인 컨트롤러가 core 에 있고, 다른 도메인의
 * 리포지토리를 그냥 가져다 쓴다. 한 번 정리해도 다음 기능에서 다시 섞이면 의미가 없다.
 *
 * <p>그래서 규칙을 코드로 두고, **지금 있는 위반은 baseline 으로 묶는다**({@link FreezingArchRule}).
 * 새 위반만 빌드를 실패시키고, 기존 위반을 고치면 목록에서 자동으로 빠진다. 전부 고칠 때까지
 * 규칙 도입을 미루지 않아도 된다.
 *
 * <p>baseline 은 {@code archunit-baseline/} 에 텍스트로 저장되며 함께 커밋한다.
 * 줄 수가 줄어드는 것이 리팩터링 진척도다.
 */
@DisplayName("구조 규칙")
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.carecode");
    }

    /**
     * core 기반 패키지(보안·웹·설정·저장소·유틸)는 도메인을 몰라야 한다.
     *
     * <p>{@code core/scheduler}·{@code core/client}·{@code core/devtools} 는 공공데이터 동기화와 배치처럼
     * **원래 여러 도메인을 조합하는 코드**다. 도메인을 아는 게 본질이라 이 규칙의 대상이 아니다.
     * 다만 그 코드가 "공통 기반" 자리에 있는 것이 혼란의 원인이므로, 조합 계층으로 따로 옮기는 것은
     * 리팩터링 1단계에서 다룬다.
     */
    @Test
    @DisplayName("core 기반 패키지는 domain 을 의존하지 않는다")
    void coreFoundationDoesNotDependOnDomain() {
        check(noClasses()
                .that().resideInAnyPackage(
                        "com.carecode.core.security..",
                        "com.carecode.core.web..",
                        "com.carecode.core.handler..",
                        "com.carecode.core.config..",
                        "com.carecode.core.storage..",
                        "com.carecode.core.util..",
                        "com.carecode.core.monitoring..",
                        "com.carecode.core.aspect..",
                        "com.carecode.core.annotation..",
                        "com.carecode.core.constants..",
                        "com.carecode.core.components..",
                        "com.carecode.core.ops..")
                .should().dependOnClassesThat().resideInAPackage("com.carecode.domain..")
                .because("기반 코드가 특정 도메인을 알면 그 도메인의 일부지, 공통이 아니다"));
    }

    /**
     * core 에 API 가 있으면 "이 기능은 어느 도메인인가" 에 답할 수 없다.
     * 실제로 {@code core/controller/CareFacilityApiController} 가 373줄짜리 시설 API 다.
     */
    @Test
    @DisplayName("core 에는 컨트롤러가 없다")
    void noControllersInCore() {
        check(noClasses()
                .that().resideInAPackage("com.carecode.core..")
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Controller")
                .because("API 는 도메인 패키지에 둔다. core 에 있으면 소속을 알 수 없다"));
    }

    /**
     * 도메인 사이 결합의 1위가 리포지토리 직접 참조다(UserRepository 24곳, ChildRepository 12곳).
     * 남의 테이블을 직접 읽으면 그쪽 규칙(소유권·소프트 삭제 등)을 건너뛰게 된다.
     */
    @Test
    @DisplayName("다른 도메인의 리포지토리를 직접 쓰지 않는다")
    void noCrossDomainRepositoryAccess() {
        check(classes()
                .that().resideInAPackage("com.carecode.domain..")
                .should(DomainBoundaryConditions.notAccessRepositoriesOfOtherDomains())
                .because("남의 도메인 테이블을 직접 읽으면 그 도메인이 지키는 규칙(소유권 검증 등)을 건너뛴다"));
    }

    /** 컨트롤러가 리포지토리를 직접 쓰면 검증·트랜잭션 경계가 컨트롤러로 흘러 들어온다. */
    @Test
    @DisplayName("컨트롤러는 리포지토리를 직접 쓰지 않는다")
    void controllersDoNotUseRepositories() {
        check(noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                .because("검증과 트랜잭션 경계는 서비스에 둔다"));
    }

    /** 엔티티가 요청·응답 모양을 알면 API 를 바꿀 때마다 테이블 매핑이 흔들린다. */
    @Test
    @DisplayName("엔티티는 DTO·컨트롤러를 의존하지 않는다")
    void entitiesDoNotDependOnWebLayer() {
        check(noClasses()
                .that().resideInAPackage("..entity..")
                .should().dependOnClassesThat().resideInAnyPackage("..dto..", "..controller..")
                .because("API 모양이 바뀔 때 테이블 매핑이 함께 흔들리면 안 된다"));
    }

    /** 서비스가 컨트롤러를 알면 호출 방향이 뒤집힌다. */
    @Test
    @DisplayName("서비스·리포지토리는 컨트롤러를 의존하지 않는다")
    void innerLayersDoNotDependOnControllers() {
        check(noClasses()
                .that().resideInAnyPackage("..service..", "..repository..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .because("호출 방향은 컨트롤러 → 서비스 → 리포지토리 한 쪽이다"));
    }

    /**
     * 현재 사용자를 꺼내는 방법이 하나여야 한다. {@code SecurityContextHolder} 를 도메인에서 직접 열면
     * principal 모양이 바뀔 때마다(실제로 이메일 문자열 principal 때문에 시설 API 전체가 500 이었다)
     * 고쳐야 할 곳이 흩어진다.
     */
    @Test
    @DisplayName("도메인은 SecurityContextHolder 를 직접 쓰지 않는다")
    void domainsUseCurrentUserFacade() {
        check(noClasses()
                .that().resideInAPackage("com.carecode.domain..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.security.core.context.SecurityContextHolder")
                .because("현재 사용자는 CurrentUserFacade 로만 얻는다"));
    }

    /** 매퍼는 변환만 한다. DB 를 읽기 시작하면 변환 한 번에 쿼리가 몇 개 나가는지 알 수 없다. */
    @Test
    @DisplayName("매퍼는 리포지토리를 쓰지 않는다")
    void mappersDoNotQuery() {
        check(noClasses()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                .because("변환 한 번에 쿼리가 숨어 들면 N+1 을 추적할 수 없다"));
    }

    /** 패키지 이름은 소문자 한 단어로 둔다. {@code careFacility} 하나만 낙타등이다. */
    @Test
    @DisplayName("도메인 패키지 이름은 소문자다")
    void domainPackagesAreLowerCase() {
        check(classes()
                .that().resideInAPackage("com.carecode.domain..")
                .should(DomainBoundaryConditions.resideInLowerCaseDomainPackage())
                .because("한 군데만 다른 규칙이면 새로 들어온 사람이 둘 다 흉내 낸다"));
    }

    /**
     * 지금 있는 위반은 baseline 으로 묶고 새 위반만 실패시킨다.
     * baseline 이 줄어드는 것이 진척도다.
     */
    private static void check(ArchRule rule) {
        FreezingArchRule.freeze(rule).check(classes);
    }
}

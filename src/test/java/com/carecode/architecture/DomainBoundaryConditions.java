package com.carecode.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * 도메인 경계 조건. ArchUnit 기본 문법으로는 "자기 도메인인지" 를 비교할 수 없어 직접 쓴다
 * (패키지 패턴만으로는 "A 도메인이 B 도메인의 리포지토리를 쓴다" 를 표현하지 못한다).
 */
final class DomainBoundaryConditions {

    private static final String DOMAIN_ROOT = "com.carecode.domain.";

    private DomainBoundaryConditions() {
    }

    /** 다른 도메인의 리포지토리를 참조하면 위반. 같은 도메인 안에서는 자유롭다. */
    static ArchCondition<JavaClass> notAccessRepositoriesOfOtherDomains() {
        return new ArchCondition<>("다른 도메인의 리포지토리를 직접 참조하지 않는다") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String ownDomain = domainOf(item.getPackageName());
                if (ownDomain == null) {
                    return;
                }
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    if (!targetPackage.startsWith(DOMAIN_ROOT) || !targetPackage.contains(".repository")) {
                        continue;
                    }
                    String targetDomain = domainOf(targetPackage);
                    if (targetDomain == null || targetDomain.equals(ownDomain)) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(item,
                            ownDomain + " → " + targetDomain + " 리포지토리: " + dependency.getDescription()));
                }
            }
        };
    }

    /** 도메인 패키지 이름이 소문자가 아니면 위반 (careFacility 처럼 한 군데만 다른 규칙을 막는다). */
    static ArchCondition<JavaClass> resideInLowerCaseDomainPackage() {
        return new ArchCondition<>("소문자 도메인 패키지에 있다") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String domain = domainOf(item.getPackageName());
                if (domain != null && !domain.equals(domain.toLowerCase(java.util.Locale.ROOT))) {
                    events.add(SimpleConditionEvent.violated(item,
                            "도메인 패키지 이름이 소문자가 아니다: " + domain + " (" + item.getName() + ")"));
                }
            }
        };
    }

    /** com.carecode.domain.<이름>... 에서 <이름>. 도메인 밖이면 null. */
    private static String domainOf(String packageName) {
        if (!packageName.startsWith(DOMAIN_ROOT)) {
            return null;
        }
        String rest = packageName.substring(DOMAIN_ROOT.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}

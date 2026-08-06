package com.carecode.core.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * 테이블 이름은 선언한 그대로, 컬럼 이름은 기존처럼 snake_case 로 변환한다.
 *
 * <p>Boot 기본 전략은 테이블 이름까지 소문자로 바꾼다. 그런데 마이그레이션은 대문자로 만들고
 * Linux MariaDB 는 lower_case_table_names=0 이라 대소문자를 구분해 전 테이블이 검증 실패한다.
 * 반대로 이름을 전부 그대로 쓰면 @Column 없이 선언된 필드가 camelCase 로 남아 또 어긋난다.
 */
public class CareCodeNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        // @Table 로 선언한 이름을 손대지 않는다.
        return name;
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
        return name;
    }
}

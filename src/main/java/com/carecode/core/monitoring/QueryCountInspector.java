package com.carecode.core.monitoring;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Hibernate 가 SQL 을 실행할 때마다 호출된다. SQL 자체는 바꾸지 않고 개수만 센다. */
public class QueryCountInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        QueryCountHolder.increment();
        return sql;
    }
}

package com.carecode.core.monitoring;

/** 요청 하나가 실행한 SQL 수를 센다. N+1 을 추정이 아니라 숫자로 확인하기 위한 도구다. */
public final class QueryCountHolder {

    private static final ThreadLocal<int[]> COUNTER = new ThreadLocal<>();

    private QueryCountHolder() {
    }

    public static void start() {
        COUNTER.set(new int[1]);
    }

    public static void increment() {
        int[] counter = COUNTER.get();
        if (counter != null) {
            counter[0]++;
        }
    }

    public static int get() {
        int[] counter = COUNTER.get();
        return counter == null ? 0 : counter[0];
    }

    /** 스레드 풀 재사용 시 카운트가 누적되지 않도록 요청 종료 시 반드시 호출한다. */
    public static void clear() {
        COUNTER.remove();
    }
}

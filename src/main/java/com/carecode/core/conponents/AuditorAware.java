package com.carecode.core.conponents;

import java.util.Optional;

public interface AuditorAware<T> {
    Optional<T> getCurrentAuditor();
}

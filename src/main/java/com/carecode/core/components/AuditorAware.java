package com.carecode.core.components;

import java.util.Optional;

public interface AuditorAware<T> {
    Optional<T> getCurrentAuditor();
}
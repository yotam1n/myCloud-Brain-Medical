package com.cloudbrain.security;

import com.cloudbrain.common.exception.ApiException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class ActorContextResolver {

    private ActorContextResolver() {
    }

    public static Optional<ActorContext> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof ActorContext actorContext) {
            return Optional.of(actorContext);
        }

        return Optional.empty();
    }

    public static ActorContext requireCurrent() {
        return current().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "unauthorized"));
    }
}

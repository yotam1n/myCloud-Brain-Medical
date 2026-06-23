package com.cloudbrain.domain.auth;

import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.ActorRole;

public record AccountProfile(
        Long userId,
        ActorRole role,
        String username,
        String passwordHash,
        Long patientId,
        Long doctorId,
        String displayName,
        String phone
) {

    public ActorContext toActorContext() {
        return new ActorContext(userId, role, patientId, doctorId, username, displayName);
    }
}

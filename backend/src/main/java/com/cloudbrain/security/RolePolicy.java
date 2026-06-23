package com.cloudbrain.security;

public interface RolePolicy {

    boolean canViewMedicalRecord(ActorContext actorContext, Long targetPatientId);

    boolean canSaveMedicalRecord(ActorContext actorContext, Long registrationId);

    boolean canSubmitPrescription(ActorContext actorContext, Long registrationId);

    boolean canViewDashboardAsDoctor(ActorContext actorContext, Long doctorId);

    boolean canViewDashboardAsAdmin(ActorContext actorContext);

    boolean canViewNotification(ActorContext actorContext, Long recipientId);
}

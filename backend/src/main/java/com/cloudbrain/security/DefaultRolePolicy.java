package com.cloudbrain.security;

import org.springframework.stereotype.Component;

@Component
public class DefaultRolePolicy implements RolePolicy {

    @Override
    public boolean canViewMedicalRecord(ActorContext actorContext, Long targetPatientId) {
        if (actorContext == null || targetPatientId == null) {
            return false;
        }
        return actorContext.isAdmin()
                || (actorContext.isPatient() && targetPatientId.equals(actorContext.patientId()));
    }

    @Override
    public boolean canSaveMedicalRecord(ActorContext actorContext, Long registrationId) {
        return actorContext != null && actorContext.isDoctor() && registrationId != null;
    }

    @Override
    public boolean canSubmitPrescription(ActorContext actorContext, Long registrationId) {
        return actorContext != null && actorContext.isDoctor() && registrationId != null;
    }

    @Override
    public boolean canViewDashboardAsDoctor(ActorContext actorContext, Long doctorId) {
        if (actorContext == null || doctorId == null) {
            return false;
        }
        return actorContext.isAdmin()
                || (actorContext.isDoctor() && doctorId.equals(actorContext.doctorId()));
    }

    @Override
    public boolean canViewDashboardAsAdmin(ActorContext actorContext) {
        return actorContext != null && actorContext.isAdmin();
    }

    @Override
    public boolean canViewNotification(ActorContext actorContext, Long recipientId) {
        if (actorContext == null || recipientId == null) {
            return false;
        }
        return actorContext.isAdmin()
                || recipientId.equals(actorContext.userId())
                || (actorContext.isPatient() && recipientId.equals(actorContext.patientId()))
                || (actorContext.isDoctor() && recipientId.equals(actorContext.doctorId()));
    }
}

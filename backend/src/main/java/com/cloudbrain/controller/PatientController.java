package com.cloudbrain.controller;

import com.cloudbrain.application.patient.PatientService;
import com.cloudbrain.common.Result;
import com.cloudbrain.dto.patient.PatientInfoResponse;
import com.cloudbrain.dto.patient.PatientUpdateRequest;
import com.cloudbrain.security.ActorContextResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/info")
    public Result<PatientInfoResponse> getInfo() {
        return Result.success(patientService.getCurrentPatientInfo(ActorContextResolver.requireCurrent()));
    }

    @PutMapping("/info")
    public Result<PatientInfoResponse> updateInfo(@Valid @RequestBody PatientUpdateRequest request) {
        return Result.success(patientService.updateCurrentPatientInfo(ActorContextResolver.requireCurrent(), request));
    }
}

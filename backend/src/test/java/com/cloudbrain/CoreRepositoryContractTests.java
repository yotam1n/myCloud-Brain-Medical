package com.cloudbrain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudbrain.entity.auth.DoctorEntity;
import com.cloudbrain.entity.auth.PatientEntity;
import com.cloudbrain.entity.core.AIConfigEntity;
import com.cloudbrain.entity.core.DepartmentEntity;
import com.cloudbrain.entity.core.DrugEntity;
import com.cloudbrain.entity.core.MedicalRecordEntity;
import com.cloudbrain.entity.core.RegistrationEntity;
import com.cloudbrain.entity.core.ScheduleEntity;
import com.cloudbrain.entity.core.PrescriptionEntity;
import com.cloudbrain.entity.core.PrescriptionReviewEntity;
import com.cloudbrain.entity.core.PrescriptionRuleDefinitionEntity;
import com.cloudbrain.entity.core.PromptTemplateEntity;
import com.cloudbrain.entity.core.TriageRecordEntity;
import com.cloudbrain.repository.AIConfigJpaRepository;
import com.cloudbrain.repository.DepartmentJpaRepository;
import com.cloudbrain.repository.DoctorJpaRepository;
import com.cloudbrain.repository.DrugJpaRepository;
import com.cloudbrain.repository.MedicalRecordJpaRepository;
import com.cloudbrain.repository.PatientJpaRepository;
import com.cloudbrain.repository.PrescriptionJpaRepository;
import com.cloudbrain.repository.PrescriptionReviewJpaRepository;
import com.cloudbrain.repository.PrescriptionRuleDefinitionJpaRepository;
import com.cloudbrain.repository.PromptTemplateJpaRepository;
import com.cloudbrain.repository.RegistrationJpaRepository;
import com.cloudbrain.repository.ScheduleJpaRepository;
import com.cloudbrain.repository.TriageRecordJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.flyway.enabled=true")
class CoreRepositoryContractTests {

    private static final String ACTIVE = "ACTIVE";
    private static final String WAITING = "WAITING";
    private static final String CANCELLED = "CANCELLED";

    @Autowired
    private DepartmentJpaRepository departmentRepository;

    @Autowired
    private PatientJpaRepository patientRepository;

    @Autowired
    private DoctorJpaRepository doctorRepository;

    @Autowired
    private ScheduleJpaRepository scheduleRepository;

    @Autowired
    private RegistrationJpaRepository registrationRepository;

    @Autowired
    private AIConfigJpaRepository aiConfigRepository;

    @Autowired
    private TriageRecordJpaRepository triageRecordRepository;

    @Autowired
    private MedicalRecordJpaRepository medicalRecordRepository;

    @Autowired
    private PrescriptionJpaRepository prescriptionRepository;

    @Autowired
    private PrescriptionReviewJpaRepository prescriptionReviewRepository;

    @Autowired
    private PrescriptionRuleDefinitionJpaRepository prescriptionRuleDefinitionRepository;

    @Autowired
    private PromptTemplateJpaRepository promptTemplateRepository;

    @Autowired
    private DrugJpaRepository drugRepository;

    @Test
    void activeRegistrationMustBeUniqueButCancelledRegistrationCanBeRebooked() {
        DepartmentEntity department = departmentRepository.save(department("dept-rebook"));
        PatientEntity patient = patientRepository.save(patient("patient-rebook", "13910000001"));
        DoctorEntity doctor = doctorRepository.save(doctor("doctor-rebook", department.getId()));
        ScheduleEntity schedule = scheduleRepository.save(schedule(doctor.getId(), department.getId()));

        registrationRepository.saveAndFlush(registration(patient, doctor, department, schedule, CANCELLED));
        RegistrationEntity rebooked = registrationRepository.saveAndFlush(registration(patient, doctor, department, schedule, WAITING));

        assertThat(rebooked.getId()).isNotNull();
        assertThat(registrationRepository.existsByPatientIdAndScheduleIdAndStatusNot(
                patient.getId(),
                schedule.getId(),
                CANCELLED
        )).isTrue();
    }

    @Test
    void duplicateActiveRegistrationForSamePatientAndScheduleIsRejected() {
        DepartmentEntity department = departmentRepository.save(department("dept-duplicate"));
        PatientEntity patient = patientRepository.save(patient("patient-duplicate", "13910000002"));
        DoctorEntity doctor = doctorRepository.save(doctor("doctor-duplicate", department.getId()));
        ScheduleEntity schedule = scheduleRepository.save(schedule(doctor.getId(), department.getId()));

        registrationRepository.saveAndFlush(registration(patient, doctor, department, schedule, WAITING));

        assertThatThrownBy(() ->
                registrationRepository.saveAndFlush(registration(patient, doctor, department, schedule, WAITING))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void registrationStatusCanAdvanceWithVersionAndCancelOnlyOnce() {
        DepartmentEntity department = departmentRepository.save(department("dept-status"));
        PatientEntity patient = patientRepository.save(patient("patient-status", "13910000003"));
        DoctorEntity doctor = doctorRepository.save(doctor("doctor-status", department.getId()));
        ScheduleEntity schedule = scheduleRepository.save(schedule(doctor.getId(), department.getId()));

        RegistrationEntity saved = registrationRepository.saveAndFlush(registration(patient, doctor, department, schedule, WAITING));
        int advanced = registrationRepository.updateStatusWithVersion(
                saved.getId(),
                WAITING,
                "IN_CONSULTATION",
                saved.getVersion()
        );

        assertThat(advanced).isEqualTo(1);
        assertThat(registrationRepository.findById(saved.getId()))
                .map(RegistrationEntity::getStatus)
                .contains("IN_CONSULTATION");

        int staleAdvance = registrationRepository.updateStatusWithVersion(
                saved.getId(),
                WAITING,
                "MEDICAL_RECORD_SAVED",
                saved.getVersion()
        );
        assertThat(staleAdvance).isZero();

        ScheduleEntity cancelSchedule = scheduleRepository.save(schedule(doctor.getId(), department.getId()));
        RegistrationEntity cancellable = registrationRepository.saveAndFlush(
                registration(patient, doctor, department, cancelSchedule, WAITING)
        );
        int cancelled = registrationRepository.cancelWaitingRegistrationOnce(
                cancellable.getId(),
                patient.getId(),
                WAITING,
                CANCELLED,
                "patient requested",
                LocalDateTime.now()
        );

        assertThat(cancelled).isEqualTo(1);
        assertThat(registrationRepository.findById(cancellable.getId()))
                .map(RegistrationEntity::getStatus)
                .contains(CANCELLED);
        assertThat(registrationRepository.cancelWaitingRegistrationOnce(
                cancellable.getId(),
                patient.getId(),
                WAITING,
                CANCELLED,
                "patient requested",
                LocalDateTime.now()
        )).isZero();

        RegistrationEntity rebooked = registrationRepository.saveAndFlush(
                registration(patient, doctor, department, cancelSchedule, WAITING)
        );
        assertThat(rebooked.getId()).isNotNull();
    }

    @Test
    void scheduleSlotDecrementAndReleaseAreIdempotentByConstraint() {
        DepartmentEntity department = departmentRepository.save(department("dept-slot"));
        PatientEntity patient = patientRepository.save(patient("patient-slot", "13910000004"));
        DoctorEntity doctor = doctorRepository.save(doctor("doctor-slot", department.getId()));
        ScheduleEntity schedule = scheduleRepository.save(schedule(doctor.getId(), department.getId()));

        int decremented = scheduleRepository.decrementSlotWithVersion(schedule.getId(), schedule.getVersion());
        assertThat(decremented).isEqualTo(1);
        ScheduleEntity afterDecrement = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(afterDecrement.getRemainingSlots()).isEqualTo(4);

        int staleDecrement = scheduleRepository.decrementSlotWithVersion(schedule.getId(), schedule.getVersion());
        assertThat(staleDecrement).isZero();

        int released = scheduleRepository.releaseSlotOnce(schedule.getId());
        assertThat(released).isEqualTo(1);
        ScheduleEntity afterRelease = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(afterRelease.getRemainingSlots()).isEqualTo(5);
        assertThat(scheduleRepository.releaseSlotOnce(schedule.getId())).isZero();
    }

    @Test
    void enabledDefaultAiConfigMustBeUniquePerTaskScope() {
        aiConfigRepository.saveAndFlush(aiConfig("openai", "TRIAGE", true, true, 10));

        assertThatThrownBy(() -> aiConfigRepository.saveAndFlush(aiConfig("deepseek", "TRIAGE", true, true, 20)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nonDefaultOrDisabledAiConfigsDoNotConflictWithEnabledDefault() {
        aiConfigRepository.saveAndFlush(aiConfig("openai", "MEDICAL_RECORD", true, true, 10));
        aiConfigRepository.saveAndFlush(aiConfig("deepseek", "MEDICAL_RECORD", false, true, 20));
        aiConfigRepository.saveAndFlush(aiConfig("local", "MEDICAL_RECORD", true, false, 30));

        assertThat(aiConfigRepository.findByTaskScopeAndEnabledTrueAndStatusOrderByPriorityDescUpdatedAtDesc(
                "MEDICAL_RECORD",
                ACTIVE
        )).hasSize(2);
        assertThat(aiConfigRepository.findFirstByTaskScopeAndDefaultConfigTrueAndEnabledTrueAndStatusOrderByPriorityDescUpdatedAtDesc(
                "MEDICAL_RECORD",
                ACTIVE
        )).isPresent();
    }

    @Test
    void repositoryReadPathsSupportMainWorkflowQueries() {
        DepartmentEntity department = departmentRepository.save(department("dept-read"));
        PatientEntity patient = patientRepository.save(patient("patient-read", "13910000005"));
        DoctorEntity doctor = doctorRepository.save(doctor("doctor-read", department.getId()));
        ScheduleEntity schedule = scheduleRepository.save(schedule(doctor.getId(), department.getId()));
        RegistrationEntity registration = registrationRepository.saveAndFlush(
                registration(patient, doctor, department, schedule, WAITING)
        );

        TriageRecordEntity triageRecord = new TriageRecordEntity();
        triageRecord.setPatientId(patient.getId());
        triageRecord.setChiefComplaint("headache");
        triageRecord.setCallStatus("COMPLETED");
        triageRecord.setRecommendationSource("AI");
        triageRecord.setRegistrationId(registration.getId());
        triageRecordRepository.saveAndFlush(triageRecord);

        MedicalRecordEntity medicalRecord = new MedicalRecordEntity();
        medicalRecord.setPatientId(patient.getId());
        medicalRecord.setDoctorId(doctor.getId());
        medicalRecord.setRegistrationId(registration.getId());
        medicalRecord.setChiefComplaint("headache");
        medicalRecord.setAiGenerated(false);
        medicalRecord.setVersion(0);
        medicalRecordRepository.saveAndFlush(medicalRecord);

        PrescriptionReviewEntity review = new PrescriptionReviewEntity();
        review.setRegistrationId(registration.getId());
        review.setDoctorId(doctor.getId());
        review.setPatientId(patient.getId());
        review.setBindStatus("UNBOUND");
        review.setVersion(0);
        PrescriptionReviewEntity savedReview = prescriptionReviewRepository.saveAndFlush(review);

        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setPatientId(patient.getId());
        prescription.setDoctorId(doctor.getId());
        prescription.setRegistrationId(registration.getId());
        prescription.setReviewId(savedReview.getId());
        prescription.setStatus("DRAFT");
        prescriptionRepository.saveAndFlush(prescription);

        PrescriptionRuleDefinitionEntity rule = new PrescriptionRuleDefinitionEntity();
        rule.setRuleCode("rule-1");
        rule.setRuleType("DRUG_CONTRAINDICATION");
        rule.setBasis("basis");
        rule.setSeeded(true);
        rule.setVersion(0);
        rule.setStatus(ACTIVE);
        prescriptionRuleDefinitionRepository.saveAndFlush(rule);

        PromptTemplateEntity prompt = new PromptTemplateEntity();
        prompt.setTemplateCode("prompt-1");
        prompt.setTaskType("TRIAGE");
        prompt.setTemplateBody("body");
        prompt.setVersion(0);
        prompt.setDefaultTemplate(true);
        prompt.setStatus(ACTIVE);
        promptTemplateRepository.saveAndFlush(prompt);

        DrugEntity drug = new DrugEntity();
        drug.setCode("drug-1");
        drug.setName("drug-1");
        drug.setStatus(ACTIVE);
        drugRepository.saveAndFlush(drug);

        assertThat(triageRecordRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())).hasSize(1);
        assertThat(triageRecordRepository.findByRegistrationId(registration.getId())).isPresent();
        assertThat(medicalRecordRepository.findByRegistrationIdOrderByVersionDesc(registration.getId())).hasSize(1);
        assertThat(medicalRecordRepository.findFirstByRegistrationIdOrderByVersionDesc(registration.getId())).isPresent();
        assertThat(prescriptionRepository.findByRegistrationIdOrderByCreatedAtDesc(registration.getId())).hasSize(1);
        assertThat(prescriptionReviewRepository.findByIdAndBindStatus(savedReview.getId(), "UNBOUND")).isPresent();
        assertThat(prescriptionRuleDefinitionRepository.findByRuleCode("rule-1")).isPresent();
        assertThat(promptTemplateRepository.findByTemplateCode("prompt-1")).isPresent();
        assertThat(drugRepository.findByCode("drug-1")).isPresent();
        assertThat(departmentRepository.findByStatusOrderByNameAsc(ACTIVE))
                .extracting(DepartmentEntity::getCode)
                .contains("dept-read");
        assertThat(doctorRepository.findByDepartmentIdAndStatusOrderByNameAsc(department.getId(), ACTIVE)).hasSize(1);
        assertThat(scheduleRepository.findByDoctorIdAndWorkDateAndStatusOrderByPeriodAsc(
                doctor.getId(),
                schedule.getWorkDate(),
                ACTIVE
        )).hasSize(1);
    }

    private DepartmentEntity department(String code) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setCode(code);
        entity.setName(code);
        entity.setType("OUTPATIENT");
        entity.setStatus(ACTIVE);
        return entity;
    }

    private PatientEntity patient(String username, String phone) {
        PatientEntity entity = new PatientEntity();
        entity.setUsername(username);
        entity.setPasswordHash("{noop}password");
        entity.setPhone(phone);
        entity.setName(username);
        entity.setStatus(ACTIVE);
        return entity;
    }

    private DoctorEntity doctor(String username, Long departmentId) {
        DoctorEntity entity = new DoctorEntity();
        entity.setUsername(username);
        entity.setPasswordHash("{noop}password");
        entity.setName(username);
        entity.setDepartmentId(departmentId);
        entity.setStatus(ACTIVE);
        return entity;
    }

    private ScheduleEntity schedule(Long doctorId, Long departmentId) {
        ScheduleEntity entity = new ScheduleEntity();
        entity.setDoctorId(doctorId);
        entity.setDepartmentId(departmentId);
        entity.setWorkDate(LocalDate.now().plusDays(1));
        entity.setPeriod("AM");
        entity.setTotalSlots(5);
        entity.setRemainingSlots(5);
        entity.setVisitLevel("NORMAL");
        entity.setStatus(ACTIVE);
        entity.setVersion(0);
        return entity;
    }

    private RegistrationEntity registration(PatientEntity patient,
                                            DoctorEntity doctor,
                                            DepartmentEntity department,
                                            ScheduleEntity schedule,
                                            String status) {
        RegistrationEntity entity = new RegistrationEntity();
        entity.setPatientId(patient.getId());
        entity.setDoctorId(doctor.getId());
        entity.setDepartmentId(department.getId());
        entity.setScheduleId(schedule.getId());
        entity.setRegistrationTime(LocalDateTime.now());
        entity.setStatus(status);
        entity.setDepartmentSnapshot(department.getName());
        entity.setDoctorSnapshot(doctor.getName());
        entity.setVisitLevelSnapshot(schedule.getVisitLevel());
        entity.setSlotReleased(CANCELLED.equals(status));
        entity.setVersion(0);
        return entity;
    }

    private AIConfigEntity aiConfig(String provider,
                                    String taskScope,
                                    boolean defaultConfig,
                                    boolean enabled,
                                    int priority) {
        AIConfigEntity entity = new AIConfigEntity();
        entity.setProvider(provider);
        entity.setModelName(provider + "-model");
        entity.setTaskScope(taskScope);
        entity.setTimeoutSeconds(30);
        entity.setDefaultConfig(defaultConfig);
        entity.setHealthStatus("HEALTHY");
        entity.setConfigVersion("v" + priority);
        entity.setEnabled(enabled);
        entity.setPriority(priority);
        entity.setStatus(ACTIVE);
        return entity;
    }
}

ALTER TABLE registration
    DROP INDEX uk_registration_patient_schedule;

ALTER TABLE registration
    ADD COLUMN active_schedule_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status <> 'CANCELLED' THEN schedule_id ELSE NULL END);

ALTER TABLE registration
    ADD CONSTRAINT uk_registration_active_patient_schedule UNIQUE (patient_id, active_schedule_id);

ALTER TABLE ai_config
    ADD COLUMN default_enabled_task_scope VARCHAR(64)
        GENERATED ALWAYS AS (CASE WHEN is_default = TRUE AND enabled = TRUE THEN task_scope ELSE NULL END);

ALTER TABLE ai_config
    ADD CONSTRAINT uk_ai_config_default_enabled_scope UNIQUE (default_enabled_task_scope);

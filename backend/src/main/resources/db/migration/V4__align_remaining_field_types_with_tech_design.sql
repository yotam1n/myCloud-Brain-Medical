ALTER TABLE ai_config
    MODIFY COLUMN config_version VARCHAR(64);

ALTER TABLE ai_call_record
    MODIFY COLUMN config_version VARCHAR(64);

ALTER TABLE ai_call_record
    MODIFY COLUMN prompt_version VARCHAR(64);

ALTER TABLE prescription_item
    MODIFY COLUMN dosage DECIMAL(12, 2);

ALTER TABLE prescription_item
    MODIFY COLUMN quantity INT;

ALTER TABLE prescription_item
    MODIFY COLUMN default_usage VARCHAR(128);

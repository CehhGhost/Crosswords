ALTER TABLE _documents ADD COLUMN source_new VARCHAR(50);

UPDATE _documents
SET source_new = CASE source
    WHEN 0 THEN 'INTER'
    WHEN 1 THEN 'COMMER'
    WHEN 2 THEN 'CBUZ'
    WHEN 3 THEN 'CBTAJ'
    WHEN 4 THEN 'CBKIR'
    WHEN 5 THEN 'CBAZER'
    WHEN 6 THEN 'CBRF'
    ELSE 'UNKNOWN'
END;

ALTER TABLE _documents DROP COLUMN source;

ALTER TABLE _documents RENAME COLUMN source_new TO source;



ALTER TABLE _digest_templates_sources ADD COLUMN sources_new VARCHAR(50);

UPDATE _digest_templates_sources
SET sources_new = CASE sources
    WHEN 0 THEN 'INTER'
    WHEN 1 THEN 'COMMER'
    WHEN 2 THEN 'CBUZ'
    WHEN 3 THEN 'CBTAJ'
    WHEN 4 THEN 'CBKIR'
    WHEN 5 THEN 'CBAZER'
    WHEN 6 THEN 'CBRF'
    ELSE 'UNKNOWN'
END;

ALTER TABLE _digest_templates_sources DROP COLUMN sources;

ALTER TABLE _digest_templates_sources RENAME COLUMN sources_new TO sources;
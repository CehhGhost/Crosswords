ALTER TABLE _documents ADD COLUMN language_new VARCHAR(50);

UPDATE _documents
SET language_new = CASE language
    WHEN 0 THEN 'RU'
    WHEN 1 THEN 'ENG'
    ELSE 'UNKNOWN'
END;

ALTER TABLE _documents DROP COLUMN language;

ALTER TABLE _documents RENAME COLUMN language_new TO language;



ALTER TABLE _users ADD COLUMN role_new VARCHAR(255);

UPDATE _users
SET role_new = CASE role
    WHEN 0 THEN 'ROLE_ADMIN'
    WHEN 1 THEN 'ROLE_MODERATOR'
    ELSE 'UNKNOWN'
END;

ALTER TABLE _users DROP COLUMN role;

ALTER TABLE _users RENAME COLUMN role_new TO role;
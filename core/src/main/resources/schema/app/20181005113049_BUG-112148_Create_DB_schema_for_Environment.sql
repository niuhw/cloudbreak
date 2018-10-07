-- // BUG-112148 Create DB schema for Environment
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS environment (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    credential_id bigint,
    workspace_id bigint,
    owner character varying(255)
);

ALTER TABLE ONLY environment
    ADD CONSTRAINT environment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY environment
    ADD CONSTRAINT uk_environment_workspace_name UNIQUE (workspace_id, name);

CREATE SEQUENCE IF NOT EXISTS  environment_id_seq START WITH 1
                                                  INCREMENT BY 1
                                                  NO MINVALUE
                                                  NO MAXVALUE
                                                  CACHE 1;

CREATE TABLE IF NOT EXISTS env_ldap (
    env_id bigint NOT NULL,
    ldap_id bigint NOT NULL
);

ALTER TABLE ONLY env_ldap ADD CONSTRAINT fk_env_ldap_env_id FOREIGN KEY (env_id) REFERENCES environment(id);

ALTER TABLE ONLY env_ldap ADD CONSTRAINT fk_env_ldap_ldap_id FOREIGN KEY (ldap_id) REFERENCES ldapconfig(id);

CREATE INDEX IF NOT EXISTS idx_env_ldap_env_id ON env_ldap (env_id);

CREATE INDEX IF NOT EXISTS idx_env_ldap_ldap_id ON env_ldap (ldap_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS environment;

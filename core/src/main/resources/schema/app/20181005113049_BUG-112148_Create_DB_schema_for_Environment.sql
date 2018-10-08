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

CREATE TABLE IF NOT EXISTS env_proxy (
    env_id bigint NOT NULL,
    proxy_id bigint NOT NULL
);

ALTER TABLE ONLY env_proxy ADD CONSTRAINT fk_env_proxy_env_id FOREIGN KEY (env_id) REFERENCES environment(id);

ALTER TABLE ONLY env_proxy ADD CONSTRAINT fk_env_proxy_proxy_id FOREIGN KEY (proxy_id) REFERENCES proxyconfig(id);

CREATE INDEX IF NOT EXISTS idx_env_proxy_env_id ON env_proxy (env_id);

CREATE INDEX IF NOT EXISTS idx_env_proxy_proxy_id ON env_proxy (proxy_id);

CREATE TABLE IF NOT EXISTS env_rds (
    env_id bigint NOT NULL,
    rds_id bigint NOT NULL
);

ALTER TABLE ONLY env_rds ADD CONSTRAINT fk_env_rds_env_id FOREIGN KEY (env_id) REFERENCES environment(id);

ALTER TABLE ONLY env_rds ADD CONSTRAINT fk_env_rds_rds_id FOREIGN KEY (rds_id) REFERENCES rdsconfig(id);

CREATE INDEX IF NOT EXISTS idx_env_rds_env_id ON env_rds (env_id);

CREATE INDEX IF NOT EXISTS idx_env_rds_rds_id ON env_rds (rds_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_env_rds_rds_id;
DROP INDEX IF EXISTS idx_env_rds_env_id;
ALTER TABLE ONLY env_rds DROP CONSTRAINT IF EXISTS fk_env_rds_rds_id;
ALTER TABLE ONLY env_rds DROP CONSTRAINT IF EXISTS fk_env_rds_env_id;
DROP TABLE IF EXISTS env_rds;

DROP INDEX IF EXISTS idx_env_proxy_proxy_id;
DROP INDEX IF EXISTS idx_env_proxy_env_id;
ALTER TABLE ONLY env_proxy DROP CONSTRAINT IF EXISTS fk_env_proxy_proxy_id;
ALTER TABLE ONLY env_proxy DROP CONSTRAINT IF EXISTS fk_env_proxy_env_id;
DROP TABLE IF EXISTS env_proxy;

DROP INDEX IF EXISTS idx_env_ldap_ldap_id;
DROP INDEX IF EXISTS idx_env_ldap_env_id;
ALTER TABLE ONLY env_ldap DROP CONSTRAINT IF EXISTS fk_env_ldap_ldap_id;
ALTER TABLE ONLY env_ldap DROP CONSTRAINT IF EXISTS fk_env_ldap_env_id;
DROP TABLE IF EXISTS env_ldap;

DROP SEQUENCE IF EXISTS environment_id_seq;
ALTER TABLE ONLY environment DROP CONSTRAINT IF EXISTS uk_environment_workspace_name;
ALTER TABLE ONLY environment DROP CONSTRAINT IF EXISTS environment_pkey;
DROP TABLE IF EXISTS environment;

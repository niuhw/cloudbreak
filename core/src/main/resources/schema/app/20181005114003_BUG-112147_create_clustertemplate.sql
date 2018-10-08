-- // BUG-112147 create clustertemplate
-- Migration SQL that makes the change goes here.


CREATE SEQUENCE IF NOT EXISTS clustertemplate_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE  IF NOT EXISTS clustertemplate (
  id bigint PRIMARY KEY DEFAULT nextval('clustertemplate_id_seq'),
  name character varying(255) NOT NULL,
  description TEXT,
  template TEXT,
  workspace_id bigint,
  status character varying(255)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS clustertemplate;

DROP SEQUENCE IF EXISTS clustertemplate_id_seq;

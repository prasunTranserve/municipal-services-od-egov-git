CREATE SEQUENCE eg_noc_nma_architect_registration_seq;

CREATE TABLE public.eg_noc_nma_architect_registration(
    id integer NOT NULL DEFAULT nextval('eg_noc_nma_architect_registration_seq'),
    tenantid character varying(256),
    userid int,
	token character varying(256),
	uniqueid character varying(256),
   	createddate timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    lastmodifiedtime timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT eg_noc_nma_architect_registration_pk PRIMARY KEY (id)
);

ALTER SEQUENCE eg_noc_nma_architect_registration_seq
OWNED BY eg_noc_nma_architect_registration.id;
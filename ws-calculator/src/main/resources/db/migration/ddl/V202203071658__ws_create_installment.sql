CREATE TABLE IF NOT EXISTS eg_ws_installment
(
  id character varying(64) NOT NULL,
  tenantid character varying(250) NOT NULL,
  applicationno character varying(64),
  consumerno character varying(256),
  feetype character varying(64),
  installmentno numeric(12,2),
  installmentamount numeric(12,2),
  demandid character varying(256),
  additionaldetails jsonb,
  createdBy character varying(64),
  lastModifiedBy character varying(64),
  createdTime bigint,
  lastModifiedTime bigint,
  CONSTRAINT eg_ws_installment_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS index_eg_ws_installment_tenantid ON eg_ws_installment (tenantid);
CREATE INDEX IF NOT EXISTS index_eg_ws_installment_consumerno ON eg_ws_installment (consumerno);
CREATE INDEX IF NOT EXISTS index_eg_ws_installment_applicationno ON eg_ws_installment (applicationno);
CREATE INDEX IF NOT EXISTS index_eg_ws_installment_feetype ON eg_ws_installment (feetype);

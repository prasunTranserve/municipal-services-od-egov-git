CREATE TABLE IF NOT EXISTS eg_ws_annualadvancedetails
(
  id character varying(64) NOT NULL,
  tenantid character varying(250) NOT NULL,
  connectionno character varying(256) NOT NULL,
  finYear character varying(10) NOT NULL,
  status character varying(64) NOT NULL,
  channel character varying(256) NOT NULL,
  additionaldetails jsonb,
  createdBy character varying(64),
  lastModifiedBy character varying(64),
  createdTime bigint,
  lastModifiedTime bigint,
  CONSTRAINT eg_ws_annualadvancedetails_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS index_eg_ws_annualadvancedetails_tenantid ON eg_ws_annualadvancedetails (tenantid);
CREATE INDEX IF NOT EXISTS index_eg_ws_annualadvancedetails_connectionno ON eg_ws_annualadvancedetails (connectionno);
CREATE INDEX IF NOT EXISTS index_eg_ws_annualadvancedetails_status ON eg_ws_annualadvancedetails (status);
CREATE INDEX IF NOT EXISTS index_eg_ws_annualadvancedetails_channel ON eg_ws_annualadvancedetails (channel);

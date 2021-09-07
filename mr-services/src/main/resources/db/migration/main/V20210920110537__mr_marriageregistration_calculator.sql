CREATE TABLE eg_mr_calculator(
id CHARACTER VARYING(64),
tenantid CHARACTER VARYING(64),
consumercode CHARACTER VARYING(64),
billingSlabIds JSONB NOT NULL,
createdtime bigint,
createdby varchar,
lastmodifiedtime bigint,
lastmodifiedby varchar,
CONSTRAINT uk_mr_calculator UNIQUE (id)
);
ALTER TABLE eg_ws_service
ADD COLUMN IF NOT EXISTS usageCategory character varying(256),
ADD COLUMN IF NOT EXISTS noOfFlats INTEGER;

ALTER TABLE eg_ws_service_audit
ADD COLUMN IF NOT EXISTS usageCategory character varying(256),
ADD COLUMN IF NOT EXISTS noOfFlats INTEGER; 

ALTER TABLE eg_ws_connection 
ALTER COLUMN property_id DROP NOT NULL;

ALTER TABLE eg_ws_connection_audit 
ALTER COLUMN property_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS eg_ws_legacybillhistory (
	ulb_name varchar(32767),
	connection_no varchar(32767),
	mon_year varchar(32767),
	bill_number varchar(32767),
	prev_due varchar(32767),
	adjusted_amount varchar(32767),
	prev_payment varchar(32767),
	rebate_availed varchar(32767),
	fine_levied varchar(32767),
	current_water_demand varchar(32767),
	current_sewerage_demand varchar(32767),
	net_pay varchar(32767),
	"NPR" varchar(32767),
	"NPF" varchar(32767),
	billed_date varchar(32767),
	rebate_date varchar(32767),
	previous_reading varchar(32767),
	current_reading varchar(32767),
	total_unit_consumed varchar(32767)
);

CREATE INDEX IF NOT EXISTS index_eg_ws_legacybillhistory_ulb_name ON eg_ws_legacybillhistory (ulb_name);
CREATE INDEX IF NOT EXISTS index_eg_ws_legacybillhistory_connection_no ON eg_ws_legacybillhistory (connection_no);
CREATE INDEX IF NOT EXISTS index_eg_ws_legacybillhistory_bill_number ON eg_ws_legacybillhistory (bill_number);

CREATE TABLE IF NOT EXISTS eg_ws_legacypaymenthistory (
	ulb_name varchar(32767),
	connection_no varchar(32767),
	"month" varchar(32767),
	total_paid varchar(32767),
	mode_of_payment varchar(32767),
	receipt_no varchar(32767),
	receipt_date varchar(32767),
	payment_approval_status varchar(32767),
	counter_name varchar(32767)
);

CREATE INDEX IF NOT EXISTS index_eg_ws_legacypaymenthistory_ulb_name ON eg_ws_legacypaymenthistory (ulb_name);
CREATE INDEX IF NOT EXISTS index_eg_ws_legacypaymenthistory_connection_no ON eg_ws_legacypaymenthistory (connection_no);
CREATE INDEX IF NOT EXISTS index_eg_ws_legacypaymenthistory_receipt_no ON eg_ws_legacypaymenthistory (receipt_no);

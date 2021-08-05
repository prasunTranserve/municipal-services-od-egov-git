package org.egov.tl.web.models.excel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "eg_tl_tradelicense_migration")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TradeLicense {

	@Id
	private String id;
	private Long applicationdate;
	private String applicationstatus = "";
	private String applicationno = "";
	private String tenantid = "";
	private String tradetype = "";
	private String tradesubtype = "";
	private String tradename = "";
	private String tradeunitmeasurementname = "";
	private String tradeunitofmeasurementvalue = "";
	private String licensetype = "";
	private String licensenumber = "";
	private Long commencementdate ;
	private Long issueddate ;
	private String finacialyear = "";
	private Long validfromdate;
	private Long validtodate ;
	private String traderaddress = "";
	private String ward = "";
	private String tradevillage = "";
	private String tradecity = "";
	private String pincode = "";
	private String tradecategory = "";
	private String tradeprimaryownername = "";
	private String tradesecondaryownername = "";
	private String authorizedpersonname = "";
	private String tradeinstitutionofficialcorrespondanceaddress = "";
	private String ownermobilenumber = "";
	private String tradeinstitutionphonenumber = "";
	private Long createdtime ;

}

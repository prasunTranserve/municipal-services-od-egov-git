package org.egov.noc.thirdparty.nma.model;

import java.sql.Timestamp;
import java.util.List;

import org.egov.noc.web.model.NocSearchCriteria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NmaArchitectRegistration {

	private int id;
	
	private String tenantid;
	
	private Long userid;
	
	private String token;
	
	private String uniqueid;
	
	private Timestamp createdDate;
	
	private Timestamp lastmodifiedtime;

	
}

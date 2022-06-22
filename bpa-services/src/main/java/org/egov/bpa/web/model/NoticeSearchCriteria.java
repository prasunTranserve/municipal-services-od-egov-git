package org.egov.bpa.web.model;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeSearchCriteria {
	
	@JsonProperty("ids")
	private List<String> ids;
	
	
	@JsonProperty("businessid")
	private String businessid;
	
	@JsonProperty("tenantid")
	private String tenantid;
	
	@JsonProperty("LetterNo")
	private String LetterNo;
	
	@JsonProperty("filestoreid")
	private String filestoreid;
	
	@JsonProperty("offset")
	private Integer offset;

	@JsonProperty("limit")
	private Integer limit;
	
	@JsonProperty("fromDate")
	private Long fromDate;

	@JsonProperty("toDate")
	private Long toDate;
	
	
	public boolean isEmpty() {
		return (this.ids == null && this.businessid == null && this.LetterNo == null && this.filestoreid == null &&
				 this.fromDate == null && this.toDate == null);
	}


	

}

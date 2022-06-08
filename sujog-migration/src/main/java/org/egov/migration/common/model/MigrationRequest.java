package org.egov.migration.common.model;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.egov.migration.reader.model.BulkBillCriteria;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MigrationRequest {
	
	@JsonProperty("auth_token")
	@NotEmpty(message = "auth_token not provided")
	private String authToken;
	
	@JsonProperty("billCriteria")
	private @Valid BulkBillCriteria bulkBillCriteria;

}

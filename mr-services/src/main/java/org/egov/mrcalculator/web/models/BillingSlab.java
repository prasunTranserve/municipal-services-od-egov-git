package org.egov.mrcalculator.web.models;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.egov.mr.web.models.AuditDetails;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BillingSlab {
	
	@JsonProperty("tenantId")
	@NotNull
	@Size(min = 2, max = 128)
	private String tenantId = null;

	@JsonProperty("id")
	@Size(min = 2, max = 64)
	private String id = null;



	@JsonProperty("rate")
	private BigDecimal rate = null;
	
	private AuditDetails auditDetails;

}

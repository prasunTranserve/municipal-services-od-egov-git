package org.egov.migration.business.model;

import org.egov.migration.reader.model.WnsMeterReading;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MeterReadingDTO {
	@JsonProperty("id")
	private String id = null;

	@JsonProperty("billingPeriod")
	private String billingPeriod = null;
	
	@JsonProperty("meterStatus")
	private String meterStatus = null;

	@JsonProperty("lastReading")
	private Double lastReading = null;

	@JsonProperty("lastReadingDate")
	private Long lastReadingDate = null;

	@JsonProperty("currentReading")
	private Double currentReading = null;

	@JsonProperty("currentReadingDate")
	private Long currentReadingDate = null;

	@JsonProperty("connectionNo")
	private String connectionNo = null;

	@JsonProperty("consumption")
	private Double consumption = null;

	@JsonProperty("generateDemand")
	private Boolean generateDemand = Boolean.TRUE;

	@JsonProperty("tenantId")
	private String tenantId = null;

}

package org.egov.wscalculation.web.models;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualAdvance {
	
	@JsonProperty("id")
	private String id = null;
	
	@NotBlank(message = "tenantId should not be blank")
	@JsonProperty("tenantId")
	private String tenantId;
	
	@NotBlank(message = "connectionNo should not be blank")
	@JsonProperty("connectionNo")
	private String connectionNo;
	
	@JsonProperty("financialYear")
	private String financialYear;
	
	@JsonProperty("channel")
	private String channel;
	
	@JsonProperty("status")
	private AnnualAdvanceStatus status;
	
	/**
	 * Gets or Sets status
	 */
	public enum AnnualAdvanceStatus {
		ACTIVE("Active"),

		CANCELLED("Cancelled"),
		
		COMPLETED("Completed");

		private String value;

		AnnualAdvanceStatus(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static AnnualAdvanceStatus fromValue(String text) {
			for (AnnualAdvanceStatus b : AnnualAdvanceStatus.values()) {
				if (String.valueOf(b.value).equals(text)) {
					return b;
				}
			}
			return null;
		}
	}
	
	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;
}

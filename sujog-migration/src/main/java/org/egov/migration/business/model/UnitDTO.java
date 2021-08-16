package org.egov.migration.business.model;

import java.math.BigDecimal;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Unit
 */

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = { "id" })
public class UnitDTO {

	@JsonProperty("id")
	private String id;

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("floorNo")
	private Long floorNo;

	@JsonProperty("unitType")
	private String unitType;

	@JsonProperty("usageCategory")
	@NotNull
	private String usageCategory;

	@JsonProperty("occupancyType")
	private String occupancyType;

	@JsonProperty("active")
	private Boolean active;

	@JsonProperty("occupancyDate")
	private Long occupancyDate;

	@JsonProperty("constructionDetail")
	private ConstructionDetailDTO constructionDetail;

	@JsonProperty("additionalDetails")
	private Object additionalDetails;

	@JsonProperty("arv")
	private BigDecimal arv;

}

package org.egov.mrcalculator.web.models;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.egov.common.contract.response.ResponseInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * All APIs will return ErrorRes in case of failure which will carry
 * ResponseInfo as metadata and Error object as actual representation of error.
 * In case of bulk apis, some apis may chose to return the array of Error
 * objects to indicate individual failure.
 */
@ApiModel(description = "All APIs will return ErrorRes in case of failure which will carry ResponseInfo as metadata and Error object as actual representation of error. In case of bulk apis, some apis may chose to return the array of Error objects to indicate individual failure.")
@Validated


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorRes {
	@JsonProperty("ResponseInfo")
	@NotNull
	@Valid
	private ResponseInfo responseInfo = null;

	@JsonProperty("Errors")
	@Valid
	private List<Error> errors = null;

}

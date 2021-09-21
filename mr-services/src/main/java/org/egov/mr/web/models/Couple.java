package org.egov.mr.web.models;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Couple {
	
	@Size(max=64)
    @JsonProperty("id")
    private String id;

	@JsonProperty("bride")
    @Valid
	private CoupleDetails bride ;
	
	@JsonProperty("groom")
    @Valid
	private CoupleDetails groom ;
	
}

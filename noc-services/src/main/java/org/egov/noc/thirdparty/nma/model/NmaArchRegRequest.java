package org.egov.noc.thirdparty.nma.model;

import java.sql.Timestamp;
import java.util.List;

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
public class NmaArchRegRequest {
	
	@JsonProperty("Users")
	private List<NmaUser> users;
}

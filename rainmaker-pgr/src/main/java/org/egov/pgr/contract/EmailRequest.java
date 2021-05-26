package org.egov.pgr.contract;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pgr.model.Email;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@ToString
public class EmailRequest {

	private RequestInfo requestInfo;

	private Email email;
	
}

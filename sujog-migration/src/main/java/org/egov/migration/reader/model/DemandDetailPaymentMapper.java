package org.egov.migration.reader.model;

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
public class DemandDetailPaymentMapper {

	//private String billId;
	private String demandid;
	private String amountpaid;
	private String collectionamount;
	private String requestInfo;
	private String businessService;
	private String tenantId;

	@Override
	public String toString() {
		return "DemandDetailPaymentMapper [demandId=" + demandid + ", amountpaid=" + amountpaid + ", collectionamount="
				+ collectionamount + "]";
	}
}

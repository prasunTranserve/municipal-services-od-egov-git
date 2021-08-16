package org.egov.migration.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.egov.migration.reader.model.Property;
import org.egov.migration.reader.model.WnsConnection;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationService {
	
	public boolean isValidProperty(Property property) {
		try {
			if(property == null || property.getPropertyId() == null) {
				return false;
			}
			List<String> errMessages = new ArrayList<String>();
			Set<ConstraintViolation<Property>> violations = new HashSet<>();
			if (MigrationUtility.isActiveProperty(property)) {

				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
				Validator validator = factory.getValidator();

				violations = validator.validate(property);
				if (!violations.isEmpty()) {
					errMessages = violations.stream().map(violation -> String.format("value: \"%s\" , Error: %s", violation.getInvalidValue(), violation.getMessage())).collect(Collectors.toList());
				}
				validateFinancialYear(property, errMessages);
				validateDemandAmout(property, errMessages);
			} else {
				log.error("Property: "+ property.getPropertyId() +" is not a valid record");
				errMessages.add("Inactive property");
			}
			
			if(errMessages.isEmpty()) {
				return true;
			} else {
				MigrationUtility.addErrorsForProperty(property.getPropertyId(), errMessages);
				return false;
			}
		} catch (Exception e) {
			log.error(String.format("Exception generated while validationg property %s, message: ", property.getPropertyId(), e.getMessage()));
			MigrationUtility.addErrorForProperty(property.getPropertyId(), e.getMessage());
			return false;
		}
		
		
	}

	private void validateFinancialYear(Property property, List<String> errMessages) {
		if(property.getAssessments() != null) {
			property.getAssessments().forEach(asmt -> {
				if(asmt.getFinYear().matches(MigrationUtility.finyearRegex)) {
					int firstYear = Integer.parseInt(asmt.getFinYear().split("-")[0]);
					int lastYear = Integer.parseInt("20".concat(asmt.getFinYear().split("-")[1]));
					if(firstYear+1 != lastYear) {
						MigrationUtility.addErrorForProperty(property.getPropertyId(), String.format("%s is not a valid financial year", asmt.getFinYear()));
					}
				}
			});
		}
	}

	private void validateDemandAmout(Property property, List<String> errMessages) {
		if(property.getDemandDetails() == null)
			return;
		Map<String, BigDecimal> finYearDemandAmtMap = new HashMap<>();
		
		BigDecimal demandDetailsTotalAmount = BigDecimal.valueOf(property.getDemandDetails().stream().mapToDouble(dedmanDetail -> Double.parseDouble(dedmanDetail.getTaxAmt()))
				.sum());
		
		List<String> dueDemandYear = property.getDemands().stream()
			.filter(demand -> demand.getPaymentComplete().equalsIgnoreCase("N"))
			.map(demand -> demand.getTaxPeriodFrom().split("/")[0])
			.distinct().collect(Collectors.toList());
		
		property.getDemands().stream().filter(demand -> dueDemandYear.contains(demand.getTaxPeriodFrom().split("/")[0]))
			.forEach(demand -> {
				BigDecimal dueAmt = finYearDemandAmtMap.get(demand.getTaxPeriodFrom().split("/")[0]);
				if (dueAmt == null) {
					finYearDemandAmtMap.put(demand.getTaxPeriodFrom().split("/")[0], new BigDecimal(demand.getMinPayableAmt()));
				} else {
					finYearDemandAmtMap.put(demand.getTaxPeriodFrom().split("/")[0], dueAmt.add(new BigDecimal(demand.getMinPayableAmt())));
				}
			});
		
		finYearDemandAmtMap.keySet().forEach(finYear -> {
			BigDecimal diffAmt = finYearDemandAmtMap.get(finYear).subtract(demandDetailsTotalAmount);
			if(diffAmt.abs().compareTo(BigDecimal.ONE) > 0) {
				errMessages.add("Total demand amount not matches for fin Year "+finYear);
			}
		});
	}

	public boolean isValidConnection(WnsConnection connection) {
		// TODO Auto-generated method stub
		return true;
	}
}

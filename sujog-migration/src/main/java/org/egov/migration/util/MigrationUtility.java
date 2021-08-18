package org.egov.migration.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.egov.migration.business.model.AssessmentDTO;
import org.egov.migration.business.model.LocalityDTO;
import org.egov.migration.business.model.PropertyDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.Address;
import org.egov.migration.reader.model.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MigrationUtility {
	
	private static MigrationUtility instance;
	
	@Autowired
	SystemProperties properties;
	
	@Autowired
	RecordStatistic recordStatistic;

	public static final BigDecimal sqmtrToSqyard = BigDecimal.valueOf(1.196);
	
	public static final String decimalRegex = "((\\d+)(((\\.)(\\d+)){0,1}))";
	
	public static final String digitRegex = "\\d+";
	
	public static final String property_active = "A";
	
	public static final String comma = ",";
	
	public static final String dateFormat = "dd-MM-yy";

	public static final String convertDateFormat = "dd/MM/yyyy";
	
	public static final String finyearRegex = "^\\d{4}-\\d{2}$";
	
	 @PostConstruct
	  public void fillInstance() {
	    instance = this;
	  }
	
	public static String readCellValue(Cell cell, boolean isDecimal) {
		if (cell == null) {
			return null;
		}

		String value = null;
		switch (cell.getCellType()) {
		case NUMERIC:
			value = getNumericValue(cell, isDecimal);
			break;
		case STRING:
			value = cell.getStringCellValue().trim().replaceAll("  +", " ");;
			break;
		case BOOLEAN:
			value = String.valueOf(cell.getBooleanCellValue());
			break;
		}
		return value;
	}

	private static String getNumericValue(Cell cell, boolean isDecimal) {
		if(DateUtil.isCellDateFormatted(cell)) {
			Date cellValue = cell.getDateCellValue();
			DateFormat formatter = new SimpleDateFormat(dateFormat);
			return formatter.format(cellValue);
		}
		
		Double value = cell.getNumericCellValue();
		String returnVal;
		if(isDecimal) {
			returnVal = NumberToTextConverter.toText(value);
		} else {
			returnVal = NumberToTextConverter.toText(value.longValue());
		}
		return returnVal;
	}

	public static String getOwnershioCategory(String ownershipCategory) {
		if(ownershipCategory == null) {
			return "INDIVIDUAL.SINGLEOWNER";
		} else if ("Single Owner".equalsIgnoreCase(ownershipCategory)) {
			return "INDIVIDUAL.SINGLEOWNER";
		} else if ("Multi Owner".equalsIgnoreCase(ownershipCategory)) {
			return "INDIVIDUAL.MULTIPLEOWNERS";
		} else {
			return "INDIVIDUAL.SINGLEOWNER";
		}
	}

	public static String getUsageCategory(String usageCategory) {
		if(usageCategory == null) {
			return "RESIDENTIAL";
		} else if ("Residential".equalsIgnoreCase(usageCategory)) {
			return "RESIDENTIAL";
		} else if ("Commercial".equalsIgnoreCase(usageCategory)) {
			return "NONRESIDENTIAL.COMMERCIAL";
		} else
			return "RESIDENTIAL";
	}

	public static String getProperty(String propertyType) {
		return "BUILTUP.INDEPENDENTPROPERTY";
	}

	public static BigDecimal convertAreaToYard(String area) {
		if(area == null)
			return null;
		return BigDecimal.valueOf(Double.parseDouble(area)).multiply(sqmtrToSqyard).setScale(2, RoundingMode.UP);
	}

	public static LocalityDTO getLocality(String code) {
		return LocalityDTO.builder().code(code).build();
	}

	public static String processMobile(String mobileNumber) {
		if(mobileNumber==null)
			return null;
		
//		String specialCharRegex = "[\\D]";
//		String leadingZeroRegex = "^0+(?!$)";
//		
//		mobileNumber = mobileNumber.trim();
//		
//		if(mobileNumber.startsWith("+")) {
//			mobileNumber = mobileNumber.replaceAll(specialCharRegex, "");
//			mobileNumber = mobileNumber.substring(2);
//		}
//		mobileNumber = mobileNumber.replaceAll(specialCharRegex, "");
//		mobileNumber = mobileNumber.replaceAll(leadingZeroRegex, "");
		if(!mobileNumber.matches(digitRegex))
			return null;
		
		if(!startsWith6to9(mobileNumber))
			return null;
		
		return mobileNumber.length() == 10 ? mobileNumber : null;
	}

	private static boolean startsWith6to9(String mobileNumber) {
		char startWith = mobileNumber.charAt(0);
		if(startWith=='6' || startWith=='7' || startWith=='8' || startWith=='9')
			return true;
		return false;
	}

	public static String getGender(String gender) {
		if (gender == null) {
			return null;
		}

		if (gender.equalsIgnoreCase("MALE") || gender.equalsIgnoreCase("M")) {
			return "Male";
		} else if (gender.equalsIgnoreCase("FEMALE") || gender.equalsIgnoreCase("F")) {
			return "Female";
		} else
			return null;
	}

	public static String getCorrespondanceAddress(Address address) {
		String correspondenceAddress = null;
		if( address == null) {
			return null;
		}
		
		if (address.getAddressLine1() != null) {
			correspondenceAddress = address.getAddressLine1();
		}

		if (address.getAddressLine2() != null) {
			correspondenceAddress = correspondenceAddress != null
					? correspondenceAddress.concat(comma).concat(address.getAddressLine2())
					: address.getAddressLine2();
		}
		
		if (address.getCity() != null) {
			correspondenceAddress = correspondenceAddress != null
					? correspondenceAddress.concat(comma).concat(address.getCity())
					: address.getCity();
		}
		
		if (address.getPin() != null) {
			correspondenceAddress = correspondenceAddress != null
					? correspondenceAddress.concat(comma).concat(address.getPin())
					: address.getPin();
		}

		return correspondenceAddress;
	}

	public static Long getLongDate(String dateString, String dateformat) {
		if(dateString == null) {
			return 0L;
		}
		return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateformat)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public static BigDecimal getAmount(String taxAmt) {
		if(taxAmt==null || !taxAmt.matches(decimalRegex)) {
			return BigDecimal.ZERO;
		}
		return new BigDecimal(taxAmt).setScale(2, RoundingMode.UP);
	}
	
	public static String removeLeadingZeros(String value) {
		return value;
//		String regex = "^0+(?!$)";
//		return value.replaceAll(regex, "");
	}

	public static Long getTaxPeriodFrom(String finYear) {
		if(finYear==null)
			return null;
	
		String year = finYear.split("-")[0];
		
		LocalDateTime finFirstDate = LocalDateTime.of(Integer.parseInt(year) , 4, 2, 1, 0, 0);
		return finFirstDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
	
	public static Long getTaxPeriodTo(String finYear) {
		if(finYear==null)
			return null;
	
		String year = finYear.split("-")[0];
		LocalDateTime finLastDate = LocalDateTime.of(Integer.parseInt(year) , 3, 30, 13, 0, 0);
		finLastDate = finLastDate.plusYears(1);
		return finLastDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public static boolean isActiveProperty(Property property) {
		if(property != null && property.getStatus() != null && property.getStatus().trim().toUpperCase().equalsIgnoreCase(property_active)) {
			return true;
		}
		return false;
	}
	
	public static Long getFloorNo(String floorNo) {
		if(floorNo == null) {
			return null;
		} else if(floorNo.matches(digitRegex)) {
			return Long.parseLong(floorNo);
		} else {
			return MigrationUtility.instance.getSystemProperties().getFloorNo().get(floorNo.trim().replace(" ", "_"));
		}
	}

	public static String getOccupancyType(String occupancyType, BigDecimal arv) {
		String ocType = MigrationConst.OCCUPANCY_TYPE_SELFOCCUPIED;
		if(arv != null) {
			ocType = MigrationConst.OCCUPANCY_TYPE_RENTED;
		}
		return ocType;
	}

	public static BigDecimal getAnnualRentValue(String arv) {
		if(arv == null)
			return null;
		
		if(arv.matches(decimalRegex)) {
			return new BigDecimal(arv.trim()).setScale(2, RoundingMode.UP);
		}
		return BigDecimal.ZERO;
	}
	

	public static SystemProperties getSystemProperties() {
		return instance.properties;
	}
	
	public static RecordStatistic getRecordStatistic() {
		return instance.recordStatistic;
	}

	public static String addLeadingZeros(String cellValue) {
		if(cellValue.contains("/")) {
			return cellValue;
		} else if(cellValue.length() < 6 && cellValue.matches(digitRegex)) {
			StringBuffer zeros = new StringBuffer("000000");
			zeros.append(cellValue);
			return zeros.substring(zeros.length()-6, zeros.length());
		} else {
			return cellValue;
		}
	}

	public static String getConnectionCategory(String connectionCategory) {
		if(connectionCategory == null)
			return null;
		return connectionCategory.trim().toUpperCase();
	}

	public static String getConnectionType(String connectionType) {
		if(connectionType == null) {
			return null;
		}
		return connectionType.replace("-", " ");
	}

	public static String getWaterSource(String waterSource) {
		return waterSource;
	}

	public static Integer getDefaultZero(String noOfFlats) {
		if(noOfFlats.trim().matches(digitRegex)) {
			return Integer.parseInt(noOfFlats.trim());
		}
		return Integer.parseInt("0");
	}

	public static Long getMeterInstallationDate(String meterInstallationDate, String connectionType) {
		if(MigrationConst.CONNECTION_METERED.equals(connectionType)) {
			return getLongDate(connectionType, dateFormat);
		} else {
			return 0L;
		}
	}

	public static String getConnectionBillingPeriod(String billingPeriod) {
		DateTimeFormatter fromFormatter = DateTimeFormatter.ofPattern(dateFormat);
		DateTimeFormatter toFormatter = DateTimeFormatter.ofPattern(convertDateFormat);
		
		LocalDate billingMonth = LocalDate.parse(billingPeriod, fromFormatter);
		LocalDate firstDate = billingMonth.withDayOfMonth(1);
		LocalDate lastDate = billingMonth.withDayOfMonth(billingMonth.lengthOfMonth());
		
		return firstDate.format(toFormatter).concat("-").concat(lastDate.format(toFormatter));
	}
	
	public static void addErrorsForProperty(String propertyId, List<String> errors) {
		Map<String, List<String>> errorMap = MigrationUtility.instance.recordStatistic.getErrorRecords();
		if(errorMap.get(propertyId) == null) {
			errorMap.put(propertyId, new ArrayList<>());
		}
		errorMap.get(propertyId).addAll(errors);
	}
	
	public static void addErrorForProperty(String propertyId, String error) {
		Map<String, List<String>> errorMap = MigrationUtility.instance.recordStatistic.getErrorRecords();
		if(errorMap.get(propertyId) == null) {
			errorMap.put(propertyId, new ArrayList<>());
		} 
		errorMap.get(propertyId).add(error);
	}
	
	public static void addSuccessForProperty(PropertyDTO migratedProperty) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if(successMap.get(migratedProperty.getOldPropertyId()) == null) {
			successMap.put(migratedProperty.getOldPropertyId(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(migratedProperty.getOldPropertyId());
		itemMap.put(MigrationConst.PROPERTY_ID, migratedProperty.getPropertyId());
	}
	
	public static void addSuccessForAssessment(PropertyDTO migratedProperty, AssessmentDTO migratedAssessment) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if(successMap.get(migratedProperty.getOldPropertyId()) == null) {
			successMap.put(migratedProperty.getOldPropertyId(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(migratedProperty.getOldPropertyId());
		itemMap.put(MigrationConst.ASSESSMENT_NUMBER, migratedAssessment.getAssessmentNumber());
	}

	public static String getSalutation(String salutation) {
		salutation = salutation.trim();
		if(salutation.length()>5) {
			return null;
		}
		return salutation;
	}

	public static boolean isPropertyEmpty(Property property) {
		if(property.getPropertyId() == null && property.getStatus() == null) {
			return true;
		}
		return false;
	}
}

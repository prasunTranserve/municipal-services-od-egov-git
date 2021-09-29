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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.egov.migration.business.model.AssessmentDTO;
import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.business.model.DemandDTO;
import org.egov.migration.business.model.LocalityDTO;
import org.egov.migration.business.model.MeterReadingDTO;
import org.egov.migration.business.model.PropertyDTO;
import org.egov.migration.business.model.SewerageConnectionDTO;
import org.egov.migration.business.model.WaterConnectionDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.Address;
import org.egov.migration.reader.model.Owner;
import org.egov.migration.reader.model.Property;
import org.egov.migration.reader.model.WnsConnection;
import org.egov.migration.reader.model.WnsMeterReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MigrationUtility {

	private static MigrationUtility instance;

	@Autowired
	SystemProperties properties;

	@Autowired
	RecordStatistic recordStatistic;

	public static final BigDecimal sqmtrToSqyard = BigDecimal.valueOf(1.196);
	public static final BigDecimal sqftToSqyard = BigDecimal.valueOf(9);
	
	public static final BigDecimal maxDigitSupportedArea = BigDecimal.valueOf(99999999);

	public static final String decimalRegex = "((\\d+)(((\\.)(\\d+)){0,1}))";
	
	public static final String negativeDecimalRegex = "^-?[0-9]\\d*(\\.\\d+)?$";

	public static final String digitRegex = "\\d+";

	public static final String property_active = "A";

	public static final String comma = ",";

	public static final String dateFormat = "dd-MM-yy";

	public static final String convertDateFormat = "dd/MM/yyyy";

	public static final String finyearRegex = "^\\d{4}-\\d{2}$";
	
	public static String namePattern = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
	
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
			//value = cell.getStringCellValue().trim().replaceAll("  +", " ");
			value = getNumericIfCellTypeString(cell, isDecimal);
			break;
		case BOOLEAN:
			value = String.valueOf(cell.getBooleanCellValue());
			break;
		}
		return value;
	}
	
	private static String getNumericIfCellTypeString(Cell cell, boolean isDecimal) {
		// TODO Auto-generated method stub
		if(isDecimal) {
			return cell.getStringCellValue().trim().replaceAll(" +", "");
		} else {
			return cell.getStringCellValue().trim().replaceAll("  +", " ");
		}
		
	}

	private static String getNumericValue(Cell cell, boolean isDecimal) {
		if (DateUtil.isCellDateFormatted(cell)) {
			Date cellValue = cell.getDateCellValue();
			DateFormat formatter = new SimpleDateFormat(dateFormat);
			return formatter.format(cellValue);
		}

		Double value = cell.getNumericCellValue();
		String returnVal;
		if (isDecimal) {
			returnVal = NumberToTextConverter.toText(value);
		} else {
			returnVal = NumberToTextConverter.toText(value.longValue());
		}
		return returnVal;
	}

	public static String getOwnershioCategory(String ownershipCategory) {
		if (ownershipCategory == null) {
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
		if (usageCategory == null) {
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
		BigDecimal convertedArea;
		if (area == null)
			return BigDecimal.ONE;
		if(!area.matches(decimalRegex))
			return BigDecimal.ONE;
		convertedArea = BigDecimal.valueOf(Double.parseDouble(area)).multiply(sqmtrToSqyard).setScale(2, RoundingMode.UP);
		if(convertedArea.compareTo(maxDigitSupportedArea) > 0) {
			convertedArea = BigDecimal.valueOf(Double.parseDouble(area)).divide(sqftToSqyard, 2, RoundingMode.UP);
			if(convertedArea.compareTo(maxDigitSupportedArea) > 0) {
				convertedArea = maxDigitSupportedArea;
			}
		}
		return convertedArea;
	}

	public static LocalityDTO getLocality(String code) {
		return LocalityDTO.builder().code(code).build();
	}

	public static String processMobile(String mobileNumber) {
		if (mobileNumber == null)
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
		if (!mobileNumber.matches(digitRegex))
			return null;

		if (!startsWith6to9(mobileNumber))
			return null;

		return mobileNumber.length() == 10 ? mobileNumber : null;
	}

	private static boolean startsWith6to9(String mobileNumber) {
		char startWith = mobileNumber.charAt(0);
		if (startWith == '6' || startWith == '7' || startWith == '8' || startWith == '9')
			return true;
		return false;
	}

	public static String getGender(String gender) {
		if (gender == null) {
			return "Male";
		}

		if (gender.equalsIgnoreCase("MALE") || gender.equalsIgnoreCase("M")) {
			return "Male";
		} else if (gender.equalsIgnoreCase("FEMALE") || gender.equalsIgnoreCase("F")) {
			return "Female";
		} else
			return "Male";
	}

	public static String getCorrespondanceAddress(Address address) {
		String correspondenceAddress = null;
		if (address == null) {
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
		if (dateString == null) {
			return 0L;
		}
		try {
			return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateformat)).atTime(11, 0)
					.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		} catch (Exception e) {
			dateString = dateString.replaceAll("  +", " ").split(" ")[0];
			return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd-MM-yy")).atTime(11, 0)
					.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}
	}
	
	public static Long getPreviousMonthLongDate(String dateString, String dateformat) {
		if (dateString == null) {
			return 0L;
		}
		try {
			return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateformat)).minusMonths(1).atTime(11, 0)
					.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		} catch (Exception e) {
			dateString = dateString.replaceAll("  +", " ").split(" ")[0];
			return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd-MM-yy")).minusMonths(1).atTime(11, 0)
					.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}
	}
	
	public static Long getExecutionDate(String dateString, String dateformat) {
		if (dateString == null) {
			return LocalDate.now().atTime(11,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}
		try {
			return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateformat)).atTime(11, 0)
					.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		} catch (Exception e) {
			dateString = dateString.replaceAll("  +", " ").split(" ")[0];
			return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd-MM-yy")).atTime(11, 0)
					.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}
	}

	public static BigDecimal getAmount(String taxAmt) {
		if (taxAmt == null || !taxAmt.matches(decimalRegex)) {
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
		if (finYear == null)
			return null;
		finYear = finYear.replaceAll("[^0-9-\\/Q]", "");
		String year = finYear.split("-")[0];

		LocalDateTime finFirstDate = LocalDateTime.of(Integer.parseInt(year), 4, 2, 1, 0, 0);
		return finFirstDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public static Long getTaxPeriodTo(String finYear) {
		if (finYear == null)
			return null;
		
		finYear = finYear.replaceAll("[^0-9-\\/Q]", "");
		String year = finYear.split("-")[0];
		LocalDateTime finLastDate = LocalDateTime.of(Integer.parseInt(year), 3, 30, 13, 0, 0);
		finLastDate = finLastDate.plusYears(1);
		return finLastDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public static boolean isActiveProperty(Property property) {
		if (property != null && property.getStatus() != null
				&& property.getStatus().trim().toUpperCase().equalsIgnoreCase(property_active)) {
			return true;
		}
		return false;
	}

	public static Long getFloorNo(String floorNo) {
		if (floorNo == null) {
			return 1L;
		} else if (floorNo.matches(digitRegex)) {
			return Long.parseLong(floorNo);
		} else if(MigrationUtility.instance.getSystemProperties().getFloorNo().get(floorNo.trim().toLowerCase().replace(" ", "_")) != null) {
			return MigrationUtility.instance.getSystemProperties().getFloorNo().get(floorNo.trim().toLowerCase().replace(" ", "_"));
		} else {
			return 1L;
		}
	}

	public static String getOccupancyType(String occupancyType, BigDecimal arv) {
		String ocType = MigrationConst.OCCUPANCY_TYPE_SELFOCCUPIED;
		if (arv != null) {
			ocType = MigrationConst.OCCUPANCY_TYPE_RENTED;
		}
		return ocType;
	}

	public static BigDecimal getAnnualRentValue(String arv) {
		if (arv == null)
			return null;

		if (arv.matches(decimalRegex)) {
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
		return cellValue;
		/*if (cellValue.contains("/")) {
			return cellValue;
		} else if (cellValue.length() < 6 && cellValue.matches(digitRegex)) {
			StringBuffer zeros = new StringBuffer("000000");
			zeros.append(cellValue);
			return zeros.substring(zeros.length() - 6, zeros.length());
		} else {
			return cellValue;
		}*/
	}

	public static String getConnectionCategory(String connectionCategory) {
		if (connectionCategory == null)
			return "PERMANENT";
		if(connectionCategory.equalsIgnoreCase("Temporary")) {
			return "TEMPORARY";
		}
		return "PERMANENT";
	}

	public static String getConnectionType(String connectionType) {
		if (connectionType == null) {
			return MigrationConst.CONNECTION_NON_METERED;
		}
		
		if(connectionType.equalsIgnoreCase(MigrationConst.CONNECTION_METERED)) {
			return MigrationConst.CONNECTION_METERED;
		}
		return MigrationConst.CONNECTION_NON_METERED;
	}

	public static String getWaterSource(String waterSource) {
		if(waterSource == null)
			return "SURFACE.CANAL";
		return waterSource;
	}

	public static Integer getDefaultZero(String noOfFlats) {
		if(noOfFlats == null)
			return 0;
		
		if (noOfFlats.trim().matches(digitRegex)) {
			return Integer.parseInt(noOfFlats.trim());
		}
		return Integer.parseInt("0");
	}

	public static Long getMeterInstallationDate(String meterInstallationDate, String connectionType) {
		if (MigrationConst.CONNECTION_METERED.equals(connectionType)) {
			if(meterInstallationDate == null)
				return 1L;
			return getLongDate(meterInstallationDate, dateFormat);
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

	public static void addErrors(String key, List<String> errors) {
		Map<String, List<String>> errorMap = MigrationUtility.instance.recordStatistic.getErrorRecords();
		if (errorMap.get(key) == null) {
			errorMap.put(key, new ArrayList<>());
		}
		errorMap.get(key).addAll(errors);
	}

	public static void addError(String key, String error) {
		Map<String, List<String>> errorMap = MigrationUtility.instance.recordStatistic.getErrorRecords();
		if (errorMap.get(key) == null) {
			errorMap.put(key, new ArrayList<>());
		}
		errorMap.get(key).add(error);
	}

	public static void addSuccessForProperty(PropertyDTO migratedProperty) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if (successMap.get(migratedProperty.getOldPropertyId()) == null) {
			successMap.put(migratedProperty.getOldPropertyId(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(migratedProperty.getOldPropertyId());
		itemMap.put(MigrationConst.PROPERTY_ID, migratedProperty.getPropertyId());
	}

	public static void addSuccessForAssessment(PropertyDTO migratedProperty, AssessmentDTO migratedAssessment) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if (successMap.get(migratedProperty.getOldPropertyId()) == null) {
			successMap.put(migratedProperty.getOldPropertyId(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(migratedProperty.getOldPropertyId());
		itemMap.put(MigrationConst.ASSESSMENT_NUMBER, migratedAssessment.getAssessmentNumber());
	}

	public static String getSalutation(String salutation) {
		if(salutation == null)
			return "Mr";
		
		salutation = salutation.trim();
		if (salutation.length() > 5) {
			return "Mr";
		}
		return salutation;
	}

	public static boolean isPropertyEmpty(Property property) {
		if (property.getPropertyId() == null && property.getStatus() == null) {
			return true;
		}
		return false;
	}

	public static String getMeterStatus(String meterStatus) {
		if (meterStatus == null) {
			return "Working";
		} else if (meterStatus.equalsIgnoreCase("NW")) {
			return "Breakdown";
		} else if (meterStatus.equalsIgnoreCase("W")) {
			return "Working";
		}
		return "Working";
	}

	public static Integer getWaterClosets(WnsConnection connection) {
		int waterClosets = 4;
		if(connection.getService().getNoOfClosets() != null && connection.getService().getNoOfClosets().matches(digitRegex)) {
			waterClosets = Integer.parseInt(connection.getService().getNoOfClosets());
		}
		return waterClosets;
	}

	public static Integer getToilets(WnsConnection connection) {
		if (connection.getService().getNoOfToilets() == null) {
			return 0;
		}
		if(!connection.getService().getNoOfToilets().matches(decimalRegex)) {
			return 0;
		}
		return Integer.parseInt(connection.getService().getNoOfToilets());
	}

	public static Double getMeterLastReading(WnsMeterReading meterReading) {
		if (meterReading.getPreviousReading() == null) {
			return 0D;
		} else {
			try {
				return Double.parseDouble(meterReading.getPreviousReading());
			} catch (Exception e) {
				return 0D;
			}
		}
	}

	public static Long getMeterLastReadingDate(WnsMeterReading meterReading) {
		if (meterReading.getPreviousReadingDate() == null) {
			LocalDate currentDate = null;
			if(meterReading.getCurrentReadingDate() == null) {
				currentDate = LocalDate.parse(meterReading.getCreatedDate(),
						DateTimeFormatter.ofPattern(dateFormat));
			} else {
				currentDate = LocalDate.parse(meterReading.getCurrentReadingDate(),
						DateTimeFormatter.ofPattern(dateFormat));
			}
			return currentDate.minusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}
		LocalDate previousDate = LocalDate.parse(meterReading.getPreviousReadingDate(),
				DateTimeFormatter.ofPattern(dateFormat));
		return previousDate.atTime(10,00).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public static Double getMeterCurrentReading(WnsMeterReading meterReading) {
		if (meterReading.getCurrentReading() == null) {
			return 0D;
		} else {
			try {
				return Double.parseDouble(meterReading.getCurrentReading());
			} catch (Exception e) {
				return 0D;
			}
		}
	}

	public static Long getMeterCurrentReadingDate(WnsMeterReading meterReading) {
		LocalDate currentDate = null;
		if (meterReading.getCurrentReadingDate() == null) {
			currentDate = LocalDate.parse(meterReading.getCreatedDate(),
					DateTimeFormatter.ofPattern(dateFormat));
		} else {
			currentDate = LocalDate.parse(meterReading.getCurrentReadingDate(),
					DateTimeFormatter.ofPattern(dateFormat));
		}
		
		return currentDate.atTime(11,00).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public static void addSuccessForWaterConnection(WaterConnectionDTO waterConnection) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if (successMap.get(waterConnection.getOldConnectionNo()) == null) {
			successMap.put(waterConnection.getOldConnectionNo(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(waterConnection.getOldConnectionNo());
		itemMap.put(MigrationConst.WATER_CONNECTION_NO, waterConnection.getConnectionNo());
	}
	
	public static void addSuccessForSewerageConnection(SewerageConnectionDTO sewerageConnection) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if (successMap.get(sewerageConnection.getOldConnectionNo()) == null) {
			successMap.put(sewerageConnection.getOldConnectionNo(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(sewerageConnection.getOldConnectionNo());
		itemMap.put(MigrationConst.SEWERAGE_CONNECTION_NO, sewerageConnection.getConnectionNo());
	}

	public static void addSuccessForMeterReading(ConnectionDTO conn) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if (successMap.get(conn.getWaterConnection().getOldConnectionNo()) == null) {
			successMap.put(conn.getWaterConnection().getOldConnectionNo(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(conn.getWaterConnection().getOldConnectionNo());
		itemMap.put(MigrationConst.METER_READING, conn.getMeterReading().getId());
	}
	
	public static void addSuccessForWaterDemand(ConnectionDTO conn) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if (successMap.get(conn.getWaterConnection().getOldConnectionNo()) == null) {
			successMap.put(conn.getWaterConnection().getOldConnectionNo(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(conn.getWaterConnection().getOldConnectionNo());
		if(itemMap.get(MigrationConst.DEMAND_WATER) == null) {
			itemMap.put(MigrationConst.DEMAND_WATER, conn.getWaterDemands().stream().map(DemandDTO::getId).collect(Collectors.joining(",")));
		}
		
	}
	
	public static void addSuccessForSewerageDemand(ConnectionDTO conn) {
		Map<String, Map<String, String>> successMap = MigrationUtility.instance.recordStatistic.getSuccessRecords();
		if (successMap.get(conn.getSewerageConnection().getOldConnectionNo()) == null) {
			successMap.put(conn.getSewerageConnection().getOldConnectionNo(), new HashMap<>());
		}
		Map<String, String> itemMap = successMap.get(conn.getSewerageConnection().getOldConnectionNo());
		if(itemMap.get(MigrationConst.DEMAND_SEWERAGE) == null) {
			itemMap.put(MigrationConst.DEMAND_SEWERAGE, conn.getSewerageDemands().stream().map(DemandDTO::getId).collect(Collectors.joining(",")));
		}
		
	}

	public static boolean isActiveConnection(WnsConnection connection) {
		if(connection.getApplicationStatus() == null) {
			return false;
		}
		String applicationStatus = connection.getApplicationStatus().trim().replaceAll("  +", " ");
		if(MigrationConst.APPLICATION_APPROVED.equalsIgnoreCase(applicationStatus)
				&& MigrationConst.STATUS_ACTIVE.equalsIgnoreCase(connection.getStatus().trim())) {
			return true;
		}
		return false;
	}

	public static boolean isNumeric(String noOfTaps) {
		if(noOfTaps.trim().matches(digitRegex))
			return true;
		return false;
	}

	public static Double getConsumption(MeterReadingDTO meterReadingDTO) {
		if(meterReadingDTO.getCurrentReading() != null && meterReadingDTO.getLastReading() != null)
			return meterReadingDTO.getCurrentReading().doubleValue() - meterReadingDTO.getLastReading().doubleValue();
		if(meterReadingDTO.getCurrentReading() == null && meterReadingDTO.getLastReading() != null)
			return Double.parseDouble("0");
		if(meterReadingDTO.getCurrentReading() != null && meterReadingDTO.getLastReading() == null)
			return meterReadingDTO.getCurrentReading().doubleValue();
		return Double.parseDouble("0");
	}
	
	public static LocalDate toDate(String strDate) {
		try {
			return LocalDate.parse(strDate, DateTimeFormatter.ofPattern(dateFormat));
		} catch (Exception e) {
			return LocalDate.of(1980, 1, 1);
		}
		
	}

	public static String getConnectionUsageCategory(String usageCategory) {
		if(usageCategory == null) {
			return "DOMESTIC";
		}
		usageCategory = usageCategory.trim();
		if(usageCategory.equalsIgnoreCase("Apartment")) {
			return "DOMESTIC";
		}
		return usageCategory.toUpperCase();
	}

	public static Integer getNoOfFlat(String usageCategory, String noOfFlats) {
		if(noOfFlats == null) {
			return Integer.parseInt("0");
		}
		if(usageCategory.equalsIgnoreCase("Apartment")) {
			return Integer.parseInt(noOfFlats.trim());
		}
		return Integer.parseInt("0");
	}

	public static void correctOwner(Property property) {
		if(property.getOwners() == null) {
			property.setOwners(Arrays.asList(Owner.builder().ownerName(property.getPropertyId())
					.gurdianName("Other").relationship("FATHER")
					.ownerType(MigrationConst.DEFAULT_OWNER_TYPE)
					.gender("MALE").build()));
		} else {
			property.getOwners().forEach(owner -> {
				owner.setOwnerName(prepareName(property.getPropertyId(), owner.getOwnerName()));
				owner.setGender(prepareGender(owner));
				owner.setRelationship(owner.getRelationship()==null? "FATHER" : owner.getRelationship());
				owner.setGurdianName(prepareName("Other", owner.getGurdianName()));
			});
		}
	}

	private static String prepareGender(@Valid Owner owner) {
		String gender = "MALE";
		if(owner.getGender()==null) {
			if(owner.getSalutation() != null && 
					(owner.getSalutation().equalsIgnoreCase("M/S")
							|| owner.getSalutation().equalsIgnoreCase("Miss")
							|| owner.getSalutation().equalsIgnoreCase("Mrs"))) {
				gender="FEMALE";
			}
		} else {
			gender = owner.getGender();
		}
		return gender;
	}

	public static String prepareName(String key, String ownerName) {
		if(ownerName == null) {
			return key;
		}
		
		if(!ownerName.matches(MigrationConst.OWNER_NAME_PATTERN)) {
			ownerName = ownerName.replaceAll("[^a-zA-Z0-9,\\s\\.]", "");
			if(ownerName.contains(",")) {
				ownerName = ownerName.substring(0, ownerName.indexOf(","));
			}
		}
		
		if(ownerName.length() > 50) {
			ownerName = ownerName.substring(0, 50);
		}
		return ownerName;
	}
	
	public static String getNearest(String amount, String divident) {
		if(amount == null)
			return "0";
		BigDecimal amt = new BigDecimal(amount);
		BigDecimal di = new BigDecimal(divident);
		amt = amt.divide(di).setScale(0, RoundingMode.CEILING).multiply(di);
		return amt.toString();
	}

	public static String getFinYear() {
		LocalDate today = LocalDate.now();
		if(today.getMonthValue() > 3) {
			return String.format("%s-%s", today.getYear(), String.valueOf(today.plusYears(1).getYear()).substring(2));
		} else {
			return String.format("%s-%s", today.minusYears(1).getYear(), String.valueOf(today.getYear()).substring(2));
		}
	}

	public static Double getDoubleAmount(String amount) {
		if (amount == null || !amount.matches(decimalRegex)) {
			return Double.parseDouble("0");
		}
		return Double.parseDouble(amount);
	}
	
	public static String getAssessmentFinYear(String finYear) {
		if(StringUtils.isEmpty(finYear)) {
			return "1980-81";
		}
		finYear = finYear.replaceAll("[^0-9-]", "");
		if(!finYear.matches(finyearRegex)) {
			finYear = "1980-81";
		}
		return finYear;
	}

	public static String getCurrentDate() {
		Date today = new Date();
		DateFormat formatter = new SimpleDateFormat(dateFormat);
		return formatter.format(today);
	}

	public static String getPaymentComplete(String paymentComplete) {
		if(paymentComplete == null)
			return "N";
		if(!(paymentComplete.equalsIgnoreCase("N") || paymentComplete.equalsIgnoreCase("Y"))) {
			return "N";
		}
		return paymentComplete;
	}

	public static Integer getNoOfTaps(String noOfTaps) {
		int taps = 2;
		if(noOfTaps != null && noOfTaps.matches(digitRegex)) {
			taps = Integer.parseInt(noOfTaps);
		}
		if(taps > 100) {
			taps = 2;
		}
		return taps;
	}

	public static String getDummyConnectionBillingPeriod() {
		DateTimeFormatter toFormatter = DateTimeFormatter.ofPattern(convertDateFormat);

		LocalDate billingMonth = LocalDate.now().minusMonths(1);
		LocalDate firstDate = billingMonth.withDayOfMonth(1);
		LocalDate lastDate = billingMonth.withDayOfMonth(billingMonth.lengthOfMonth());

		return firstDate.format(toFormatter).concat("-").concat(lastDate.format(toFormatter));
	}

	public static String getGuardian(String guardian) {
		if(guardian == null) {
			return "Other";
		}
		return prepareName(guardian, guardian);
	}

	public static String getRelationship(String guardianRelation) {
		if(guardianRelation == null)
			return "FATHER";
		if(guardianRelation.equalsIgnoreCase("husband"))
			return "HUSBAND";
		if(guardianRelation.equalsIgnoreCase("mother"))
			return "MOTHER";
		return "FATHER";
	}

	public static String getAddress(String holderAddress) {
		if(holderAddress == null) {
			return "Other";
		}
		return holderAddress;
	}

	public static String getDoorNo(String doorNo) {
		if(StringUtils.isEmpty(doorNo))
			return "1";
		return doorNo;
	}

	public static String getPIN(String pin) {
		if(StringUtils.isEmpty(pin))
			return "111111";
		return pin;
	}

	public static String getWard(String ward) {
		if(ward == null) {
			return "01";
		}
		if(!ward.matches(digitRegex)) {
			return "01";
		}
		if(Integer.parseInt(ward) > 69) {
			return "01";
		}
		if(Integer.parseInt(ward) < 10) {
			return "0".concat(ward);
		}
		return ward;
	}

	public static Integer getPipeSize(String actualPipeSize) {
		if(actualPipeSize==null)
			return 0;
		if(actualPipeSize.matches(decimalRegex)) {
			return Integer.parseInt(actualPipeSize);
		}
		return 0;
	}

	public static String getOwnerType(String connectionHolderType) {
		if(connectionHolderType == null)
			return "NONE";
		if(connectionHolderType.equalsIgnoreCase("BPL"))
			return "BPL";
		return "NONE";
	}

	public static String getStreet(Address address) {
		String street = "";
		if(StringUtils.isEmpty(address.getAddressLine1()) && StringUtils.isEmpty(address.getAddressLine2())) {
			return "Other";
		}
		if(!StringUtils.isEmpty(address.getAddressLine1())) {
			street = street.concat(address.getAddressLine1());
		}
		if(!StringUtils.isEmpty(address.getAddressLine2())) {
			street = street.concat(comma).concat(address.getAddressLine2());
		}
		if(street.startsWith(comma)) {
			street = street.substring(1);
		}
		return StringUtils.isEmpty(street)?"Other":street;
	}

	public static String getDefaultOther(String value) {
		if(StringUtils.isEmpty(value)) {
			return "Other";
		}
		return value;
	}

	public static BigDecimal convertToBigDecimal(String amt) {
		if(StringUtils.isEmpty(amt)) {
			return BigDecimal.ZERO;
		}
		if(!amt.matches(negativeDecimalRegex)) {
			return BigDecimal.ZERO;
		}
		return new BigDecimal(amt);
	}
}

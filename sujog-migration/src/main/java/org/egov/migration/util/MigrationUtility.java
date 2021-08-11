package org.egov.migration.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.egov.migration.business.model.LocalityDTO;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.model.Address;
import org.egov.migration.model.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MigrationUtility {
	
	private static MigrationUtility instance;
	
	@Autowired
	SystemProperties properties;

	public static final BigDecimal sqmtrToSqyard = BigDecimal.valueOf(1.196);
	
	public static final String decimalRegex = "((\\d+)(((\\.)(\\d+)){0,1}))";
	
	public static final String digitRegex = "\\d+";
	
	public static final String property_active = "A";
	
	public static final String comma = ",";
	
	public static final String dateFormat = "dd-MM-yy";
	
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
			value = cell.getStringCellValue().trim();
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
			returnVal = String.valueOf(value);
		} else {
			returnVal = String.valueOf(value.longValue());
		}
		return returnVal;
	}

	public static String getOwnershioCategory(String ownershipCategory) {
		if ("Single Owner".equalsIgnoreCase(ownershipCategory)) {
			return "INDIVIDUAL.SINGLEOWNER";
		} else if ("Multi Owner".equalsIgnoreCase(ownershipCategory)) {
			return "INDIVIDUAL.MULTIPLEOWNERS";
		} else {
			return "INDIVIDUAL.SINGLEOWNER";
		}
	}

	public static String getUsageCategory(String usageCategory) {
		if ("Residential".equalsIgnoreCase(usageCategory)) {
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
		return BigDecimal.valueOf(Double.parseDouble(area)).multiply(sqmtrToSqyard).setScale(2, RoundingMode.UP);
	}

	public static LocalityDTO getLocality(String code) {
		return LocalityDTO.builder().code(code).build();
	}

	public static String processMobile(String mobileNumber) {
		if(mobileNumber==null)
			return null;
		
		String specialCharRegex = "[\\D]";
		String leadingZeroRegex = "^0+(?!$)";
		
		mobileNumber = mobileNumber.trim();
		
		if(mobileNumber.startsWith("+")) {
			mobileNumber = mobileNumber.replaceAll(specialCharRegex, "");
			mobileNumber = mobileNumber.substring(2);
		}
		mobileNumber = mobileNumber.replaceAll(specialCharRegex, "");
		mobileNumber = mobileNumber.replaceAll(leadingZeroRegex, "");
		
		return mobileNumber.length() == 10 ? mobileNumber : null;
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
		
		LocalDateTime finFirstDate = LocalDateTime.of(Integer.parseInt(year) , 4, 1, 22, 0, 0);
		return finFirstDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
	
	public static Long getTaxPeriodTo(String finYear) {
		if(finYear==null)
			return null;
	
		String year = finYear.split("-")[0];
		LocalDateTime finLastDate = LocalDateTime.of(Integer.parseInt(year) , 3, 31, 22, 0, 0);
		finLastDate = finLastDate.plusYears(1);
		return finLastDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public static boolean isActiveProperty(Property property) {
		if(property.getStatus() != null && property.getStatus().trim().toUpperCase().equalsIgnoreCase(property_active)) {
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
		// TODO Auto-generated method stub
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
}

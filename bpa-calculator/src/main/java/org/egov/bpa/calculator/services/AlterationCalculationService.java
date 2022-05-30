package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AlterationCalculationService {
	
	private static final BigDecimal ZERO_TWO_FIVE = new BigDecimal("0.25");// BigDecimal.valueOf(0.25);
	private static final BigDecimal ZERO_FIVE = new BigDecimal("0.5");// BigDecimal.valueOf(0.5);
	private static final BigDecimal TEN = new BigDecimal("10");// BigDecimal.valueOf(10);
	private static final BigDecimal FIFTEEN = new BigDecimal("15");// BigDecimal.valueOf(15);
//	private static final BigDecimal SEVENTEEN_FIVE = new BigDecimal("17.50");// BigDecimal.valueOf(17.50);
	private static final BigDecimal SEVENTEEN_POINT_EIGHT_FIVE = new BigDecimal("17.85");// BigDecimal.valueOf(17.50);
	private static final BigDecimal EIGHTEEN_POINT_TWO_ONE = new BigDecimal("18.21");
	private static final BigDecimal TWENTY = new BigDecimal("20");// BigDecimal.valueOf(20);
	private static final BigDecimal TWENTY_FIVE = new BigDecimal("25");// BigDecimal.valueOf(25);
	private static final BigDecimal THIRTY = new BigDecimal("30");// BigDecimal.valueOf(30);
	private static final BigDecimal FIFTY = new BigDecimal("50");// BigDecimal.valueOf(50);
	private static final BigDecimal HUNDRED = new BigDecimal("100");// BigDecimal.valueOf(100);
	private static final BigDecimal TWO_HUNDRED = new BigDecimal("200");// BigDecimal.valueOf(200);
	private static final BigDecimal TWO_HUNDRED_FIFTY = new BigDecimal("250");// BigDecimal.valueOf(250);
	private static final BigDecimal THREE_HUNDRED = new BigDecimal("300");// BigDecimal.valueOf(300);
	private static final BigDecimal FIVE_HUNDRED = new BigDecimal("500");// BigDecimal.valueOf(500);
	private static final BigDecimal FIFTEEN_HUNDRED = new BigDecimal("1500");// BigDecimal.valueOf(1500);
	private static final BigDecimal SEVENTEEN_FIFTY = new BigDecimal("1750");// BigDecimal.valueOf(1750);
	private static final BigDecimal TWO_THOUSAND = new BigDecimal("2000");// BigDecimal.valueOf(2000);
	private static final BigDecimal THOUSAND = new BigDecimal("1000");// BigDecimal.valueOf(2000);
	private static final BigDecimal TEN_LAC = new BigDecimal("1000000");// BigDecimal.valueOf(1000000);
	private static final BigDecimal SQMT_SQFT_MULTIPLIER = new BigDecimal("10.764");// BigDecimal.valueOf(10.764);
	private static final BigDecimal ACRE_SQMT_MULTIPLIER = new BigDecimal("4046.85");// BigDecimal.valueOf(4046.85);
	
	public BigDecimal calculateFeeForBuildingOperation(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(
						paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForAgricultureOccupancy(paramMap);
			}

		}
		generateTaxHeadEstimate(estimates, feeForBuildingOperation,
				BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE, Category.FEE);
		log.info("FeeForBuildingOperation:::::::::::" + feeForBuildingOperation);
		return feeForBuildingOperation;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				feeForBuildingOperation = calculateVariableFee1(totalBuitUpArea);
			}

		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee1(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = TWO_HUNDRED_FIFTY;
			} else if (totalBuitUpArea <= 300) {
				amount = (TWO_HUNDRED_FIFTY
						.add(FIFTEEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (TWO_HUNDRED_FIFTY.add(FIFTEEN.multiply(TWO_HUNDRED))
						.add(TEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);
			}

		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee2(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 20) {
				amount = FIVE_HUNDRED;
			} else if (totalBuitUpArea <= 50) {
				amount = (FIVE_HUNDRED.add(FIFTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(TWENTY))))
						.setScale(2, BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 50) {
				amount = (FIVE_HUNDRED.add(FIFTY.multiply(THIRTY))
						.add(TWENTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(FIFTY)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(
			Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if (((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))) {
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))) {
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {

					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				}
			}

		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param effectiveArea
	 * @param multiplicationFactor
	 * @return
	 */
	private BigDecimal calculateConstantFeeNew(Double effectiveArea, int multiplicationFactor) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		if (null != effectiveArea) {
			totalAmount = (BigDecimal.valueOf(effectiveArea).multiply(BigDecimal.valueOf(multiplicationFactor)))
					.setScale(2, BigDecimal.ROUND_UP);

		}
		return totalAmount;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

			}
		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				feeForBuildingOperation = calculateVariableFee3(totalBuitUpArea);
			}
		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee3(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = FIFTEEN_HUNDRED;
			} else if (totalBuitUpArea <= 300) {
				amount = (FIFTEEN_HUNDRED
						.add(TWENTY_FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (FIFTEEN_HUNDRED.add(TWENTY_FIVE.multiply(TWO_HUNDRED))
						.add(FIFTEEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
			}
		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
			}
		}
		return feeForBuildingOperation;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				feeForBuildingOperation = calculateVariableFee1(totalBuitUpArea);
			}
		}
		return feeForBuildingOperation;
	}
	
	//SANCTION FEES CALCULATIONS-
	
	public BigDecimal calculateTotalSanctionFeeForPermit(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		BigDecimal sanctionFee = calculateSanctionFee(paramMap, estimates);
		BigDecimal constructionWorkerWelfareCess = calculateConstructionWorkerWelfareCess(paramMap, estimates);
		BigDecimal shelterFee = calculateShelterFee(paramMap, estimates);
		BigDecimal temporaryRetentionFee = calculateTemporaryRetentionFee(paramMap, estimates);
		BigDecimal securityDeposit = calculateSecurityDeposit(paramMap, estimates);
		BigDecimal purchasableFAR = calculatePurchasableFAR(paramMap, estimates);
		BigDecimal eidpFee = calculateEIDPFee(paramMap, estimates);

		calculatedTotalPermitFee = (calculatedTotalPermitFee.add(sanctionFee).add(constructionWorkerWelfareCess)
				.add(shelterFee).add(temporaryRetentionFee).add(securityDeposit).add(purchasableFAR)).setScale(2,
						BigDecimal.ROUND_UP);
		return calculatedTotalPermitFee;
	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateSanctionFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				sanctionFee = calculateSanctionFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				sanctionFee = calculateSanctionFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				sanctionFee = calculateSanctionFeeForPublicSemiPublicInstitutionalOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				sanctionFee = calculateSanctionFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				sanctionFee = calculateSanctionFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				sanctionFee = calculateSanctionFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				sanctionFee = calculateSanctionFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				sanctionFee = calculateSanctionFeeForAgricultureOccupancy(paramMap);
			}

		}

		generateTaxHeadEstimate(estimates, sanctionFee, BPACalculatorConstants.TAXHEAD_BPA_SANCTION_FEE, Category.FEE);

		// System.out.println("SanctionFee::::::::" + sanctionFee);
		log.info("SanctionFee::::::::" + sanctionFee);
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))) {
					// sanctionFee = calculateConstantFee(paramMap, 15);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);

				} else {
					// sanctionFee = calculateConstantFee(paramMap, 50);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 50);
				}

			}

		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// sanctionFee = calculateConstantFee(paramMap, 60);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 60);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForPublicSemiPublicInstitutionalOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C)) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {

					// sanctionFee = calculateConstantFee(paramMap, 30);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))) {
					// sanctionFee = calculateConstantFee(paramMap, 10);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))) {
					// sanctionFee = calculateConstantFee(paramMap, 30);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {
					// sanctionFee = calculateConstantFee(paramMap, 10);
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				}
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// sanctionFee = calculateConstantFee(paramMap, 10);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// sanctionFee = calculateConstantFee(paramMap, 60);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 60);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// sanctionFee = calculateConstantFee(paramMap, 30);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// sanctionFee = calculateConstantFee(paramMap, 10);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				// sanctionFee = calculateConstantFee(paramMap, 30);
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
			}
		}
		return sanctionFee;
	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateConstructionWorkerWelfareCess(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal welfareCess = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double totalFloorArea = null;
		Double alterationTotalBuiltupAreaProposed = null;
		Double alterationTotalBuiltupArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalFloorArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		alterationTotalBuiltupAreaProposed = getProposedAreaParameterForAlteration(paramMap);
		alterationTotalBuiltupArea = getTotalAreaParameterForAlteration(paramMap);
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
			// Double costOfConstruction = (1750 * totalBuitUpArea * 10.764);
			//Use builtup area instead of floor area to calculate totalCostOfConstruction and check if totalCostOfConstruction>10Lakh.If true,
			//then use builtup area instead of floor area to calculate ConstructionWorkerWelfareCess-
			BigDecimal totalCostOfConstruction = (SEVENTEEN_FIFTY
					.multiply(BigDecimal.valueOf(alterationTotalBuiltupArea)).multiply(SQMT_SQFT_MULTIPLIER))
							.setScale(2, BigDecimal.ROUND_UP);
			if (totalCostOfConstruction.compareTo(TEN_LAC) > 0) {
				welfareCess = (EIGHTEEN_POINT_TWO_ONE
						.multiply(BigDecimal.valueOf(alterationTotalBuiltupAreaProposed))
						.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, BigDecimal.ROUND_UP);
			}

		}
		generateTaxHeadEstimate(estimates, welfareCess, BPACalculatorConstants.TAXHEAD_BPA_WORKER_WELFARE_CESS, Category.FEE);

		// System.out.println("WelfareCess::::::::::::::" + welfareCess);
		log.info("WelfareCess::::::::::::::" + welfareCess);
		return welfareCess;

	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateShelterFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal shelterFee = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				shelterFee = calculateShelterFeeForResidentialOccupancy(paramMap);
			}
		}

		generateTaxHeadEstimate(estimates, shelterFee, BPACalculatorConstants.TAXHEAD_BPA_SHELTER_FEE, Category.FEE);

		// System.out.println("ShelterFee::::::::::::::::" + shelterFee);
		log.info("ShelterFee::::::::::::::::" + shelterFee);
		return shelterFee;

	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateShelterFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal shelterFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalEWSArea = null;
		boolean isShelterFeeRequired = false;
		int totalNoOfDwellingUnits = 0;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.EWS_AREA)) {
			totalEWSArea = (Double) paramMap.get(BPACalculatorConstants.EWS_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.SHELTER_FEE)) {
			isShelterFeeRequired = (boolean) paramMap.get(BPACalculatorConstants.SHELTER_FEE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS)) {
			totalNoOfDwellingUnits = (int) paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS);
		}
		if (isShelterFeeRequired && totalNoOfDwellingUnits > 8) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
				if ((StringUtils.hasText(applicationType)
						&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
						&& (StringUtils.hasText(serviceType)
								&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {

					if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))) {

						shelterFee = (BigDecimal.valueOf(totalEWSArea).multiply(SQMT_SQFT_MULTIPLIER)
								.multiply(SEVENTEEN_FIFTY).multiply(ZERO_TWO_FIVE)).setScale(2, BigDecimal.ROUND_UP);

					}

				}

			}

		}
		return shelterFee;
	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTemporaryRetentionFee(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal retentionFee = BigDecimal.ZERO;
		Double numberOfTemporaryStructures = null;
		String applicationType = null;
		String serviceType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES)) {
			numberOfTemporaryStructures = (Double) paramMap.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES);
		}
		boolean isRetentionFeeApplicable = false;
		if (null != paramMap.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE)) {
			isRetentionFeeApplicable = (boolean) paramMap.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))
				&& isRetentionFeeApplicable && Objects.nonNull(numberOfTemporaryStructures)) {
			Object mdmsData = paramMap.get("mdmsData");
			String tenantId = String.valueOf(paramMap.get("tenantId"));
			List jsonOutput = JsonPath.read(mdmsData, BPACalculatorConstants.MDMS_RETENTION_FEE_PATH);
			String filterExp = "$.[?(@.ulb == '" + tenantId + "')]";
			List<Map<String, String>> retentionFeeForTenantJson = JsonPath.read(jsonOutput, filterExp);
			if (!CollectionUtils.isEmpty(retentionFeeForTenantJson)) {
				String retentionFeeForTenant = retentionFeeForTenantJson.get(0)
						.get(BPACalculatorConstants.MDMS_RETENTION_FEE);
				retentionFee = new BigDecimal(retentionFeeForTenant).multiply(new BigDecimal(numberOfTemporaryStructures));
			}
		}

		generateTaxHeadEstimate(estimates, retentionFee, BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE, Category.FEE);

		// System.out.println("RetentionFee:::::::::::" + retentionFee);
		log.info("RetentionFee:::::::::::" + retentionFee);
		return retentionFee;

	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateSecurityDeposit(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		boolean isSecurityDepositRequired = false;
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SECURITY_DEPOSIT)) {
			isSecurityDepositRequired = (boolean) paramMap.get(BPACalculatorConstants.SECURITY_DEPOSIT);
		}

		if (totalBuitUpArea != null && isSecurityDepositRequired) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				securityDeposit = calculateSecurityDepositForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				securityDeposit = calculateSecurityDepositForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				securityDeposit = calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				securityDeposit = calculateSecurityDepositForEducationOccupancy(paramMap);
			}
		}

		generateTaxHeadEstimate(estimates, securityDeposit, BPACalculatorConstants.TAXHEAD_BPA_SECURITY_DEPOSIT, Category.FEE);
		// System.out.println("SecurityDeposit::::::::::::::" + securityDeposit);
		log.info("SecurityDeposit::::::::::::::" + securityDeposit);

		return securityDeposit;

	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {

				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_E))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_LIH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SQ))) {

					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}

			}

		}
		return securityDeposit;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}
			}
		}
		return securityDeposit;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}
			}
		}
		return securityDeposit;
	}
	
	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		totalBuitUpArea = getProposedAreaParameterForAlteration(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);
				}
			}
		}
		return securityDeposit;
	}
	
	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculatePurchasableFAR(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal purchasableFARFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double benchmarkValuePerAcre = null;
		Double baseFar = null;
		Double providedFar = null;
		Double maxPermissibleFar = null;
		Double tdrFarRelaxation = null;
		Double plotArea = null;
		String subOccupancyType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BMV_ACRE)) {
			benchmarkValuePerAcre = (Double) paramMap.get(BPACalculatorConstants.BMV_ACRE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BASE_FAR)) {
			baseFar = (Double) paramMap.get(BPACalculatorConstants.BASE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROVIDED_FAR)) {
			providedFar = (Double) paramMap.get(BPACalculatorConstants.PROVIDED_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = (Double) paramMap.get(BPACalculatorConstants.PLOT_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR)) {
			maxPermissibleFar = (Double) paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.TDR_FAR_RELAXATION)) {
			tdrFarRelaxation = (Double) paramMap.get(BPACalculatorConstants.TDR_FAR_RELAXATION);
		}

		//calculation for MIG sub-occupancy-
		if ((null != providedFar) && (null != baseFar) && (providedFar > baseFar) && (null != plotArea)
				&& StringUtils.hasText(subOccupancyType)
				&& BPACalculatorConstants.A_MIH.equalsIgnoreCase(subOccupancyType)
				&& (StringUtils.hasText(applicationType)
						&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
				BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre).divide(ACRE_SQMT_MULTIPLIER, 2,
					BigDecimal.ROUND_UP);

				BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(2,
					BigDecimal.ROUND_UP);

				BigDecimal deltaFAR = (BigDecimal.valueOf(providedFar).subtract(BigDecimal.valueOf(baseFar))).setScale(2,
					BigDecimal.ROUND_UP);
				
				BigDecimal applicableDiscountFar = (BigDecimal.valueOf(maxPermissibleFar)
						.subtract(BigDecimal.valueOf(baseFar)).multiply(new BigDecimal("0.25"))).setScale(2,
								BigDecimal.ROUND_UP);
				if (deltaFAR.compareTo(applicableDiscountFar) > 0) {
					deltaFAR = deltaFAR.subtract(applicableDiscountFar);
				} else {
					deltaFAR = BigDecimal.ZERO;
				}

				purchasableFARFee = (purchasableFARRate.multiply(deltaFAR).multiply(BigDecimal.valueOf(plotArea)))
					.setScale(2, BigDecimal.ROUND_UP);
		}
		//calculation for all cases other than MIG sub-occupancy-
		else if ((null != providedFar) && (null != baseFar) && (providedFar > baseFar) && (null != plotArea)) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {

				BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre).divide(ACRE_SQMT_MULTIPLIER,
						2, BigDecimal.ROUND_UP);

				BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(2,
						BigDecimal.ROUND_UP);

				BigDecimal deltaFAR = (BigDecimal.valueOf(providedFar).subtract(BigDecimal.valueOf(baseFar)))
						.setScale(2, BigDecimal.ROUND_UP);
				//tdr relaxation- decrease deltaFar based on tdrFarRelaxation-
				if(null!=tdrFarRelaxation) {
					deltaFAR=deltaFAR.subtract(new BigDecimal(tdrFarRelaxation)).setScale(2, BigDecimal.ROUND_UP);
				}
				if (deltaFAR.compareTo(BigDecimal.ZERO) < 0) {
					deltaFAR = BigDecimal.ZERO;
				}

				purchasableFARFee = (purchasableFARRate.multiply(deltaFAR).multiply(BigDecimal.valueOf(plotArea)))
						.setScale(2, BigDecimal.ROUND_UP);

			}

		}

		generateTaxHeadEstimate(estimates, purchasableFARFee, BPACalculatorConstants.TAXHEAD_BPA_PURCHASABLE_FAR, Category.FEE);

		// System.out.println("PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		log.info("PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		return purchasableFARFee;

	}
	
	private BigDecimal calculateEIDPFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal eidpFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double projectValue = null;
		Double alterationTotalBuiltupAreaProposed = null;
		Double alterationTotalBuiltupArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP)) {
			projectValue = (Double) paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP);
		}
		alterationTotalBuiltupAreaProposed = getProposedAreaParameterForAlteration(paramMap);
		alterationTotalBuiltupArea = getTotalAreaParameterForAlteration(paramMap);
		
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
				&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))
				&& projectValue != null) {
			
			eidpFee = BigDecimal.valueOf(projectValue).multiply(BigDecimal.valueOf(alterationTotalBuiltupAreaProposed))
					.divide(BigDecimal.valueOf(alterationTotalBuiltupArea), 2, RoundingMode.HALF_UP).divide(HUNDRED)
					.setScale(2, BigDecimal.ROUND_HALF_UP);
		}
		generateTaxHeadEstimate(estimates, eidpFee, BPACalculatorConstants.TAXHEAD_BPA_EIDP_FEE, Category.FEE);

		log.info("EIDP Fee:::::::::::::::::" + eidpFee);
		return eidpFee;
	}
	
	private void generateTaxHeadEstimate(ArrayList<TaxHeadEstimate> estimates, BigDecimal feeAmount, String taxHeadCode,
			Category category) {
		TaxHeadEstimate estimate = new TaxHeadEstimate();
		estimate.setEstimateAmount(feeAmount.setScale(0, BigDecimal.ROUND_UP));
		estimate.setCategory(category);
		estimate.setTaxHeadCode(taxHeadCode);
		estimates.add(estimate);
	}
	
	private Double getProposedAreaParameterForAlteration(Map<String, Object> paramMap) {
		String applicableAreaParameterName = BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA;
		// TODO should return null or 0?
		return null != paramMap.get(applicableAreaParameterName) ? (Double) paramMap.get(applicableAreaParameterName)
				: Double.valueOf(0);
	}
	
	private Double getTotalAreaParameterForAlteration(Map<String, Object> paramMap) {
		String applicableAreaParameterName = BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA;
		// TODO should return null or 0?
		return null != paramMap.get(applicableAreaParameterName) ? (Double) paramMap.get(applicableAreaParameterName)
				: Double.valueOf(0);
	}

}

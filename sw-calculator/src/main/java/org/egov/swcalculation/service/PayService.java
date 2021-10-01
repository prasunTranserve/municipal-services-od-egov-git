package org.egov.swcalculation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.egov.swcalculation.constants.SWCalculationConstant;
import org.egov.swcalculation.web.models.TaxHeadEstimate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.minidev.json.JSONArray;

@Service
public class PayService {

	@Autowired
	private MasterDataService mDService;
	
	@Autowired
	private EstimationService estimationService;

	/**
	 * 
	 * @param creditAmount - Credit Amount
	 * @param debitAmount - Debit Amount
	 * @return TaxHead for SW round off
	 */
	public TaxHeadEstimate roundOfDecimals(BigDecimal creditAmount, BigDecimal debitAmount, boolean isConnectionFee) {
		BigDecimal roundOffPos = BigDecimal.ZERO;
		BigDecimal roundOffNeg = BigDecimal.ZERO;
		String taxHead = isConnectionFee ? SWCalculationConstant.SW_Round_Off
				: SWCalculationConstant.SW_ONE_TIME_FEE_ROUND_OFF;
		BigDecimal result = creditAmount.add(debitAmount);
		BigDecimal roundOffAmount = result.setScale(2, 2);
		BigDecimal reminder = roundOffAmount.remainder(BigDecimal.ONE);

		if (reminder.doubleValue() >= 0.5)
			roundOffPos = roundOffPos.add(BigDecimal.ONE.subtract(reminder));
		else if (reminder.doubleValue() < 0.5)
			roundOffNeg = roundOffNeg.add(reminder).negate();

		if (roundOffPos.doubleValue() > 0)
			return TaxHeadEstimate.builder().estimateAmount(roundOffPos).taxHeadCode(taxHead).build();
		else if (roundOffNeg.doubleValue() < 0)
			return TaxHeadEstimate.builder().estimateAmount(roundOffNeg).taxHeadCode(taxHead).build();
		else
			return null;
		
	}
	
	
	/**
	 * 
	 * @param sewerageCharge - Sewerage Charge
	 * @param assessmentYear - Assessment Year
	 * @param timeBasedExemptionMasterMap - List of time based exemption map
	 * @param billingExpiryDate - Bill Expiry date
	 * @return estimation of time based exemption
	 */
	public Map<String, BigDecimal> applyPenaltyRebateAndInterest(BigDecimal sewerageCharge,
			String assessmentYear, Map<String, JSONArray> timeBasedExemptionMasterMap, Long billingExpiryDate) {

		if (BigDecimal.ZERO.compareTo(sewerageCharge) >= 0)
			return Collections.emptyMap();
		Map<String, BigDecimal> estimates = new HashMap<>();
		BigDecimal rebate = getApplicableRebate(sewerageCharge, assessmentYear, timeBasedExemptionMasterMap.get(SWCalculationConstant.SW_REBATE_MASTER));
		long currentUTC = System.currentTimeMillis();
		long numberOfDaysInMillis = billingExpiryDate - currentUTC;
		BigDecimal noOfDays = BigDecimal.valueOf((TimeUnit.MILLISECONDS.toDays(Math.abs(numberOfDaysInMillis))));
		if(BigDecimal.ONE.compareTo(noOfDays) <= 0) noOfDays = noOfDays.add(BigDecimal.ONE);
		BigDecimal penalty = getApplicablePenalty(sewerageCharge, noOfDays, timeBasedExemptionMasterMap.get(SWCalculationConstant.SW_PENANLTY_MASTER));
		BigDecimal interest = getApplicableInterest(sewerageCharge, noOfDays, timeBasedExemptionMasterMap.get(SWCalculationConstant.SW_INTEREST_MASTER));
		estimates.put(SWCalculationConstant.SW_TIME_REBATE, rebate.setScale(2, 2));
		estimates.put(SWCalculationConstant.SW_TIME_PENALTY, penalty.setScale(2, 2));
		estimates.put(SWCalculationConstant.SW_TIME_INTEREST, interest.setScale(2, 2));
		return estimates;
	}
	
	
	private BigDecimal getApplicableRebate(BigDecimal sewerageCharge, String assessmentYear, JSONArray rebateMasterList) {
		BigDecimal rebateAmt = BigDecimal.ZERO;
		Map<String, Object> rebate = mDService.getApplicableMaster(assessmentYear, rebateMasterList);

		if (null == rebate) return rebateAmt;

		String time = ((String) rebate.get(SWCalculationConstant.ENDING_DATE_APPLICABLES));
		Calendar cal = Calendar.getInstance();
		setDateToCalendar(assessmentYear, time, cal);

		if (cal.getTimeInMillis() > System.currentTimeMillis())
			rebateAmt = calculateApplicables(sewerageCharge, rebate);

		return rebateAmt;
	}


	private BigDecimal calculateApplicables(BigDecimal sewerageCharge, Map<String, Object> rebateConfig) {

		BigDecimal currentApplicable = BigDecimal.ZERO;

		if (null == rebateConfig)
			return currentApplicable;

		@SuppressWarnings("unchecked")
		Map<String, Object> configMap = (Map<String, Object>) rebateConfig;

		BigDecimal rate = null != configMap.get(SWCalculationConstant.RATE_FIELD_NAME)
				? BigDecimal.valueOf(((Number) configMap.get(SWCalculationConstant.RATE_FIELD_NAME)).doubleValue())
				: null;

		BigDecimal maxAmt = null != configMap.get(SWCalculationConstant.MAX_AMOUNT_FIELD_NAME)
				? BigDecimal.valueOf(((Number) configMap.get(SWCalculationConstant.MAX_AMOUNT_FIELD_NAME)).doubleValue())
				: null;

		BigDecimal minAmt = null != configMap.get(SWCalculationConstant.MIN_AMOUNT_FIELD_NAME)
				? BigDecimal.valueOf(((Number) configMap.get(SWCalculationConstant.MIN_AMOUNT_FIELD_NAME)).doubleValue())
				: null;

		BigDecimal flatAmt = null != configMap.get(SWCalculationConstant.FLAT_AMOUNT_FIELD_NAME)
				? BigDecimal.valueOf(((Number) configMap.get(SWCalculationConstant.FLAT_AMOUNT_FIELD_NAME)).doubleValue())
				: BigDecimal.ZERO;

		if (null == rate)
			currentApplicable = flatAmt.compareTo(sewerageCharge) > 0 ? sewerageCharge : flatAmt;
		else {
			currentApplicable = sewerageCharge.multiply(rate.divide(SWCalculationConstant.HUNDRED));

			if (null != maxAmt && BigDecimal.ZERO.compareTo(maxAmt) < 0 && currentApplicable.compareTo(maxAmt) > 0)
				currentApplicable = maxAmt;
			else if (null != minAmt && currentApplicable.compareTo(minAmt) < 0)
				currentApplicable = minAmt;
		}
		return currentApplicable;
	}


	private void setDateToCalendar(String assessmentYear, String time, Calendar cal) {
		cal.clear();
		Integer day = Integer.valueOf(time);
		// One is subtracted because calender reads january as 0
		Integer month = LocalDate.now().getMonthValue() - 1;
		Integer year = Integer.valueOf(assessmentYear.split("-")[0]);
		if (month < 3) year += 1;
		cal.set(year, month, day);
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
	}


	/**
	 * 
	 * @param sewerageCharge - Sewerage Charge
	 * @param noOfDays - No of Days
	 * @param config - Config object
	 * @return - Returns Penalty details
	 */
	public BigDecimal getApplicablePenalty(BigDecimal sewerageCharge, BigDecimal noOfDays, JSONArray config) {
		BigDecimal applicablePenalty = BigDecimal.ZERO;
		Map<String, Object> penaltyMaster = mDService.getApplicableMaster(estimationService.getAssessmentYear(), config);
		if (null == penaltyMaster) return applicablePenalty;
		BigDecimal daysApplicable = null != penaltyMaster.get(SWCalculationConstant.DAYA_APPLICABLE_NAME)
				? BigDecimal.valueOf(((Number) penaltyMaster.get(SWCalculationConstant.DAYA_APPLICABLE_NAME)).intValue())
				: null;
		if (daysApplicable == null)
			return applicablePenalty;
		BigDecimal daysDiff = noOfDays.subtract(daysApplicable);
		if (daysDiff.compareTo(BigDecimal.ONE) < 0) {
			return applicablePenalty;
		}
		BigDecimal rate = null != penaltyMaster.get(SWCalculationConstant.RATE_FIELD_NAME)
				? BigDecimal.valueOf(((Number) penaltyMaster.get(SWCalculationConstant.RATE_FIELD_NAME)).doubleValue())
				: null;

		BigDecimal flatAmt = null != penaltyMaster.get(SWCalculationConstant.FLAT_AMOUNT_FIELD_NAME)
				? BigDecimal
						.valueOf(((Number) penaltyMaster.get(SWCalculationConstant.FLAT_AMOUNT_FIELD_NAME)).doubleValue())
				: BigDecimal.ZERO;

		if (rate == null)
			applicablePenalty = flatAmt.compareTo(sewerageCharge) > 0 ? BigDecimal.ZERO : flatAmt;
		else {
			applicablePenalty = sewerageCharge.multiply(rate.divide(SWCalculationConstant.HUNDRED));
		}
		return applicablePenalty;
	}
	
	/**
	 * 
	 * @param sewerageCharge - Sewerage Charge
	 * @param noOfDays - No of Days
	 * @param config - Config object
	 * @return - Returns applicable interest details
	 */
	public BigDecimal getApplicableInterest(BigDecimal sewerageCharge, BigDecimal noOfDays, JSONArray config) {
		BigDecimal applicableInterest = BigDecimal.ZERO;
		Map<String, Object> interestMaster = mDService.getApplicableMaster(estimationService.getAssessmentYear(), config);
		if (null == interestMaster) return applicableInterest;
		BigDecimal daysApplicable = null != interestMaster.get(SWCalculationConstant.DAYA_APPLICABLE_NAME)
				? BigDecimal.valueOf(((Number) interestMaster.get(SWCalculationConstant.DAYA_APPLICABLE_NAME)).intValue())
				: null;
		if (daysApplicable == null)
			return applicableInterest;
		BigDecimal daysDiff = noOfDays.subtract(daysApplicable);
		if (daysDiff.compareTo(BigDecimal.ONE) < 0) {
			return applicableInterest;
		}
		BigDecimal rate = null != interestMaster.get(SWCalculationConstant.RATE_FIELD_NAME)
				? BigDecimal.valueOf(((Number) interestMaster.get(SWCalculationConstant.RATE_FIELD_NAME)).doubleValue())
				: null;

		BigDecimal flatAmt = null != interestMaster.get(SWCalculationConstant.FLAT_AMOUNT_FIELD_NAME)
				? BigDecimal
						.valueOf(((Number) interestMaster.get(SWCalculationConstant.FLAT_AMOUNT_FIELD_NAME)).doubleValue())
				: BigDecimal.ZERO;

		if (rate == null)
			applicableInterest = flatAmt.compareTo(sewerageCharge) > 0 ? BigDecimal.ZERO : flatAmt;
		else {
			// rate of interest
			applicableInterest = sewerageCharge.multiply(rate.divide(SWCalculationConstant.HUNDRED));
		}
		//applicableInterest.multiply(noOfDays.divide(BigDecimal.valueOf(365), 6, 5));
		return applicableInterest;
	}
	
	public BigDecimal getApplicableRebateForInitialDemand(BigDecimal sewerageCharge, String assessmentYear, JSONArray rebateMasterList) {
		BigDecimal rebateAmt = BigDecimal.ZERO;
		Map<String, Object> rebate = mDService.getApplicableMaster(assessmentYear, rebateMasterList);

		if (null == rebate) return rebateAmt;

		rebateAmt = calculateApplicables(sewerageCharge, rebate);

		return rebateAmt;
	}
}

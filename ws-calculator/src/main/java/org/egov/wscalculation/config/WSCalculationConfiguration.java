package org.egov.wscalculation.config;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Component
public class WSCalculationConfiguration {

	@Value("${egov.ws.search.meterReading.pagination.default.limit}")
	private Integer meterReadingDefaultLimit;

	@Value("${egov.ws_calculation.meterReading.default.offset}")
	private Integer meterReadingDefaultOffset;

	/*
	 * Calculator Configs
	 */

	// billing service
	@Value("${egov.billingservice.host}")
	private String billingServiceHost;

	@Value("${egov.taxhead.search.endpoint}")
	private String taxheadsSearchEndpoint;

	@Value("${egov.taxperiod.search.endpoint}")
	private String taxPeriodSearchEndpoint;

	@Value("${egov.demand.create.endpoint}")
	private String demandCreateEndPoint;

	@Value("${egov.demand.update.endpoint}")
	private String demandUpdateEndPoint;

	@Value("${egov.demand.search.endpoint}")
	private String demandSearchEndPoint;
	
	@Value("${egov.demand.migrate.endpoint}")
	private String demandMigrateEndPoint;

	@Value("${egov.bill.fetch.endpoint}")
	private String fetchBillEndPoint;

	@Value("${egov.demand.billexpirytime}")
	private Long demandBillExpiryTime;

	@Value("${egov.bill.gen.endpoint}")
	private String billGenEndPoint;

	// MDMS
	@Value("${egov.mdms.host}")
	private String mdmsHost;

	@Value("${egov.mdms.search.endpoint}")
	private String mdmsEndPoint;

	@Value("${egov.bill.gen.endpoint}")
	private String billGenerateEndpoint;

	// water demand configs

	@Value("${ws.module.code}")
	private String wsModuleCode;

	@Value("${ws.module.minpayable.amount}")
	private Integer ptMinAmountPayable;

	@Value("${ws.financialyear.start.month}")
	private String financialYearStartMonth;

	@Value("${egov.demand.businessservice}")
	private String businessService;

	@Value("${egov.demand.minimum.payable.amount}")
	private BigDecimal minimumPayableAmount;

	// water Registry
	@Value("${egov.ws.host}")
	private String waterConnectionHost;

	@Value("${egov.wc.search.endpoint}")
	private String waterConnectionSearchEndPoint;

	// Demand Topic
	@Value("${ws.calculator.demand.successful.topic}")
	private String onDemandsSaved;

	@Value("${ws.calculator.demand.failed}")
	private String onDemandsFailure;

	// Localization
	@Value("${egov.localization.host}")
	private String localizationHost;

	@Value("${egov.localization.context.path}")
	private String localizationContextPath;

	@Value("${egov.localization.search.endpoint}")
	private String localizationSearchEndpoint;

	@Value("${egov.localization.statelevel}")
	private Boolean isLocalizationStateLevel;

	// SMS
	@Value("${kafka.topics.notification.sms}")
	private String smsNotifTopic;

	@Value("${notification.sms.enabled}")
	private Boolean isSMSEnabled;

	@Value("${notification.sms.link}")
	private String smsNotificationLink;

	@Value("${notification.email.enabled}")
	private Boolean isEmailEnabled;

	// Email
	@Value("${kafka.topics.notification.mail.name}")
	private String emailNotifyTopic;

	// User Configuration
	@Value("${egov.user.host}")
	private String userHost;

	@Value("${egov.user.context.path}")
	private String userContextPath;

	@Value("${egov.user.search.path}")
	private String userSearchEndpoint;

	@Value("${egov.user.auth.token}")
	private String userAuthTokenEndPoint;

	// SW Calculator
	@Value("${egov.sw.calculator.host}")
	private String swCalculatorHost;

	@Value("${egov.sw.calculator.jobscheduler}")
	private String swJobSchedulerEndpoint;

	// Job Scheduler
	@Value("${egov.user.auth.username}")
	private String authUsername;

	@Value("${egov.user.auth.scope}")
	private String authScope;

	@Value("${egov.user.auth.password}")
	private String authPassword;

	@Value("${egov.user.auth.granttype}")
	private String authGrantType;

	@Value("${egov.user.auth.tenantid}")
	private String authTenantId;

	@Value("${egov.user.auth.usertype}")
	private String authUserType;

	@Value("${egov.user.auth.authorization}")
	private String authAuthorization;

	// Request info
	@Value("${egov.requestinfo.apiid}")
	private String requestApiId;

	@Value("${egov.requestinfo.ver}")
	private String requestVer;

	@Value("${egov.requestinfo.action}")
	private String requestAction;

	@Value("${egov.requestinfo.did}")
	private String requestDid;

	@Value("${egov.requestinfo.msgid}")
	private String requestMsgId;

	// payment
	@Value("${egov.usr.events.pay.triggers}")
	private String billgenTopic;

	// USER EVENTS
	@Value("${egov.ui.app.host}")
	private String uiAppHost;

	@Value("${egov.usr.events.create.topic}")
	private String saveUserEventsTopic;

	@Value("${egov.usr.events.pay.link}")
	private String payLink;

	@Value("${egov.usr.events.pay.code}")
	private String payCode;

	@Value("${egov.user.event.notification.enabled}")
	private Boolean isUserEventsNotificationEnabled;

	@Value("${kafka.topics.billgen.topic}")
	private String payTriggers;

	@Value("${egov.watercalculatorservice.createdemand.topic}")
	private String createDemand;

	@Value("${ws.demand.based.batch.size}")
	private Integer batchSize;

	@Value("${persister.demand.based.dead.letter.topic.batch}")
	private String deadLetterTopicBatch;

	@Value("${persister.demand.based.dead.letter.topic.single}")
	private String deadLetterTopicSingle;

	@Value("${notification.url}")
	private String notificationUrl;

	@Value("${egov.shortener.url}")
	private String shortenerURL;

	@Value("${egov.property.service.host}")
	private String propertyHost;

	@Value("${egov.property.searchendpoint}")
	private String searchPropertyEndPoint;

	@Value("${workflow.workDir.path}")
	private String workflowHost;

	@Value("${workflow.process.search.path}")
	private String searchWorkflowProcessEndPoint;
	
	@Value("${egov.demand.manualstartenddate.enabled}")
	private boolean isDemandStartEndDateManuallyConfigurable;

	@Value("${egov.demand.manualmonthnumber}")
	private int demandManualMonthNo;
	
	@Value("${egov.demand.manualyear}")
	private int demandManualYear;
	
	@Value("${egov.demand.tenants}")
	private String schedulerTenants;

	@Value("${egov.demand.tenants.skip}")
	private String skipSchedulerTenants;
	
	@Value("${egov.demand.specialrebate.months}")
	private String specialRebateMonths;
	
	@Value("${egov.demand.specialrebate.year}")
	private String specialRebateYear;
	
	@Value("${egov.demand.specialrebate.metered.months}")
	private String specialRebateMonthsForMeteredConnection;
	
	@Value("${egov.demand.sw.migratedamount.enabled}")
	private boolean swDemandMigratedAmountEnabled;
	
	@Value("${egov.demand.arrear.sw.enabled}")
	private boolean swArrearDemandEnabled;
	
	@Value("${egov.demand.arrear.sw.billing.month.meter}")
	private int swArrearBillingMonthMeter;
	
	@Value("${egov.demand.arrear.sw.billing.year.meter}")
	private int swArrearBillingYearMeter;
	
	@Value("${egov.demand.arrear.sw.months.count.meter}")
	private int swArrearMonthCountForMeter;
	
	@Value("${egov.demand.arrear.sw.billing.month.nonmeter}")
	private int swArrearBillingMonthNonMeter;
	
	@Value("${egov.demand.arrear.sw.billing.year.nonmeter}")
	private int swArrearBillingYearNonMeter;
	
	@Value("${egov.demand.arrear.sw.months.count.nonmeter}")
	private int swArrearMonthCountForNonMeter;
	
	@Value("${egov.demand.arrear.sw.special.rebate.applicable}")
	private boolean swSpecialRebateApplicable;
	
	@Value("#{new java.text.SimpleDateFormat('dd/MM/yyyy').parse('${egov.demand.sw.active.meter}')}")
    private Date swApplicableForMeter;
	
	@Value("#{new java.text.SimpleDateFormat('dd/MM/yyyy').parse('${egov.demand.sw.active.nonmeter}')}")
    private Date swApplicableForNonMeter;

	@Value("${bulk.demand.batch.value}")
	private Integer bulkbatchSize;

	@Value("${bulk.demand.offset.value}")
	private Integer batchOffset;

	@Value("${kafka.waterservice.create.installment.topic}")
    private String wsCreateInstallmentTopic;

	@Value("${kafka.waterservice.update.installment.topic}")
    private String wsUpdateInstallmentTopic;
	
	@Value("${kafka.topic.ws.installment.update}")
    private String wsInstallmentUpdateTopic;
	
}

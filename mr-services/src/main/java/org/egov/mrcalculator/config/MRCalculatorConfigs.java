package org.egov.mrcalculator.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@Data
public class MRCalculatorConfigs {



    @Value("${egov.billingservice.host}")
    private String billingHost;

    @Value("${egov.taxhead.search.endpoint}")
    private String taxHeadSearchEndpoint;

    @Value("${egov.taxperiod.search.endpoint}")
    private String taxPeriodSearchEndpoint;

    @Value("${egov.demand.create.endpoint}")
    private String demandCreateEndpoint;

    @Value("${egov.demand.update.endpoint}")
    private String demandUpdateEndpoint;

    @Value("${egov.demand.search.endpoint}")
    private String demandSearchEndpoint;

    @Value("${egov.bill.gen.endpoint}")
    private String billGenerateEndpoint;

    @Value("${egov.demand.minimum.payable.amount}")
    private BigDecimal minimumPayableAmount;

    @Value("${egov.demand.businessserviceMR}")
    private String businessServiceMR;



    //TaxHeads
    @Value("${egov.taxhead.basetax}")
    private String baseTaxHead;


    //MDMS
    @Value("${egov.mdms.host}")
    private String mdmsHost;

    @Value("${egov.mdms.search.endpoint}")
    private String mdmsSearchEndpoint;


    //Kafka Topics
    @Value("${persister.save.mr.calculation.topic}")
    private String saveTopic;


    //CalculaterType Default Values
    @Value("${egov.mr.calculationtype.default}")
    private String defaultCalculationType;



}

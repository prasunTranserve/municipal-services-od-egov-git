package org.egov.migration.processor;

import org.egov.migration.business.model.DemandDetailsDTO;
import org.egov.migration.reader.model.DemandDetailPaymentMapper;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemProcessor;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DemandSearchTransformProcessor implements ItemProcessor<DemandDetailPaymentMapper, DemandDetailsDTO> {

	//@Autowired
	//private SystemProperties properties;

	@Override
	public DemandDetailsDTO process(DemandDetailPaymentMapper demand) throws Exception {
		try {
			return transformProperty(demand);
		} catch (Exception e) {
			MigrationUtility.addError(demand.getDemandid(), e.getMessage());
		}
		return null;
	}

	private DemandDetailsDTO transformProperty(DemandDetailPaymentMapper demand) {
		try {
			DemandDetailsDTO demandDetailsDTO = new DemandDetailsDTO();
			transformProperty(demandDetailsDTO, demand);

			return demandDetailsDTO;//DemandDetailsDTO.builder().build();
		} catch (Exception e) {
			log.error(String.format("Some exception generated while reading property %s, Message: ",
					demand.getDemandid(), e.getMessage()));
			MigrationUtility.addError(demand.getDemandid(), e.getMessage());
			return null;
		}

	}

	private void transformProperty(DemandDetailsDTO demandDetailsDTO, DemandDetailPaymentMapper demand) {
		demandDetailsDTO.setDemandId(demand.getDemandid());
		demandDetailsDTO.setPaidAmount(Double.valueOf(demand.getAmountpaid()));
		demandDetailsDTO.setCollectionAmount(Double.valueOf(demand.getCollectionamount()));
		demandDetailsDTO.setRequestInfo(demand.getRequestInfo());
		demandDetailsDTO.setBusinessService(demand.getBusinessService());
		demandDetailsDTO.setTenantId(demand.getTenantId());
		System.out.println("Processor .. Demand.."+demandDetailsDTO.getDemandId());
	}

}


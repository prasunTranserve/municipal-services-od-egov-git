package org.egov.waterconnection.service;

import static org.egov.waterconnection.constants.WCConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.producer.WaterConnectionProducer;
import org.egov.waterconnection.repository.WaterDao;
import org.egov.waterconnection.repository.WaterDaoImpl;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.egov.waterconnection.validator.ActionValidator;
import org.egov.waterconnection.validator.MDMSValidator;
import org.egov.waterconnection.validator.ValidateProperty;
import org.egov.waterconnection.validator.WaterConnectionValidator;
import org.egov.waterconnection.web.models.Connection.StatusEnum;
import org.egov.waterconnection.web.models.Installments;
import org.egov.waterconnection.web.models.InstallmentsRequest;
import org.egov.waterconnection.web.models.Property;
import org.egov.waterconnection.web.models.SearchCriteria;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.workflow.BusinessService;
import org.egov.waterconnection.workflow.WorkflowIntegrator;
import org.egov.waterconnection.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WaterServiceImpl implements WaterService {

	@Autowired
	private WaterDao waterDao;

	@Autowired
	private WaterConnectionValidator waterConnectionValidator;

	@Autowired
	private ValidateProperty validateProperty;

	@Autowired
	private MDMSValidator mDMSValidator;

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
	private WSConfiguration config;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private ActionValidator actionValidator;

	@Autowired
	private WaterServicesUtil waterServiceUtil;

	@Autowired
	private CalculationService calculationService;

	@Autowired
	private WaterDaoImpl waterDaoImpl;

	@Autowired
	private UserService userService;

	@Autowired
	private WaterServicesUtil wsUtil;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private WaterConnectionProducer waterConnectionProducer;

	/**
	 * 
	 * @param waterConnectionRequest WaterConnectionRequest contains water
	 *                               connection to be created
	 * @return List of WaterConnection after create
	 */
	@Override
	public List<WaterConnection> createWaterConnection(WaterConnectionRequest waterConnectionRequest) {
		int reqType = WCConstants.CREATE_APPLICATION;
		if (wsUtil.isModifyConnectionRequest(waterConnectionRequest)) {
			reqType = getModifyConnectionRequestType(waterConnectionRequest);
			
			List<WaterConnection> previousConnectionsList = getAllWaterApplications(waterConnectionRequest);

			// Validate any process Instance exists with WF
			if (!CollectionUtils.isEmpty(previousConnectionsList)) {
				workflowService.validateInProgressWF(previousConnectionsList, waterConnectionRequest.getRequestInfo(),
						waterConnectionRequest.getWaterConnection().getTenantId());
				waterConnectionValidator.validateConnectionStatus(previousConnectionsList, waterConnectionRequest, reqType);
			}
			
		}
		waterConnectionValidator.validateWaterConnection(waterConnectionRequest, reqType);
		// Property property =
		// validateProperty.getOrValidateProperty(waterConnectionRequest);
		// validateProperty.validatePropertyFields(property,waterConnectionRequest.getRequestInfo());
		Property property = Property.builder().tenantId(waterConnectionRequest.getWaterConnection().getTenantId())
				.build();
		mDMSValidator.validateMasterForCreateRequest(waterConnectionRequest);
		enrichmentService.enrichWaterConnection(waterConnectionRequest, reqType);
		userService.createUser(waterConnectionRequest);
		// call work-flow
		if (config.getIsExternalWorkFlowEnabled())
			wfIntegrator.callWorkFlow(waterConnectionRequest, property);
		waterDao.saveWaterConnection(waterConnectionRequest);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}

	private int getModifyConnectionRequestType(WaterConnectionRequest waterConnectionRequest) {
		int reqType = WCConstants.MODIFY_CONNECTION;
		
		if(null != waterConnectionRequest.getWaterConnection().getApplicationType()) {
			switch (waterConnectionRequest.getWaterConnection().getApplicationType()) {
			case WCConstants.DISCONNECT_WATER_CONNECTION:
				reqType = WCConstants.DISCONNECT_CONNECTION;
				break;
			case WCConstants.WATER_RECONNECTION:
				reqType = WCConstants.RECONNECTION;
				break;
			case WCConstants.CONNECTION_OWNERSHIP_CHANGE:
				reqType = WCConstants.OWNERSHIP_CHANGE_CONNECTION;
				break;
			case WCConstants.CLOSE_WATER_CONNECTION:
				reqType = WCConstants.CLOSE_CONNECTION;
				break;
			case WCConstants.METER_REPLACEMENT:
				reqType = WCConstants.METER_REPLACE;
				break;
			default:
				reqType = WCConstants.MODIFY_CONNECTION;
				break;
			}
		}
		
		return reqType;
	}

	/**
	 * 
	 * @param criteria    WaterConnectionSearchCriteria contains search criteria on
	 *                    water connection
	 * @param requestInfo
	 * @return List of matching water connection
	 */
	public List<WaterConnection> search(SearchCriteria criteria, RequestInfo requestInfo) {
		List<WaterConnection> waterConnectionList;
		waterConnectionList = getWaterConnectionsList(criteria, requestInfo);
		if (!StringUtils.isEmpty(criteria.getSearchType())
				&& criteria.getSearchType().equals(WCConstants.SEARCH_TYPE_CONNECTION)) {
			waterConnectionList = enrichmentService.filterConnections(waterConnectionList);
			if (criteria.getIsPropertyDetailsRequired()) {
				waterConnectionList = enrichmentService.enrichPropertyDetails(waterConnectionList, criteria,
						requestInfo);

			}
		}
		waterConnectionValidator.validatePropertyForConnection(waterConnectionList);
		enrichmentService.enrichConnectionHolderDeatils(waterConnectionList, criteria, requestInfo);
		enrichmentService.enrichConnectionHolderInfo(waterConnectionList);
		return waterConnectionList;
	}

	/**
	 * 
	 * @param criteria    WaterConnectionSearchCriteria contains search criteria on
	 *                    water connection
	 * @param requestInfo
	 * @return List of matching water connection
	 */
	public List<WaterConnection> getWaterConnectionsList(SearchCriteria criteria, RequestInfo requestInfo) {
		return waterDao.getWaterConnectionList(criteria, requestInfo);
	}

	/**
	 * 
	 * @param waterConnectionRequest WaterConnectionRequest contains water
	 *                               connection to be updated
	 * @return List of WaterConnection after update
	 */
	@Override
	public List<WaterConnection> updateWaterConnection(WaterConnectionRequest waterConnectionRequest) {
		if (waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.LINK_MOBILE_NUMBER)) {
			return linkMobileWithWaterConnection(waterConnectionRequest);
		}
		if (waterConnectionRequest.getWaterConnection().getApplicationType()
				.equalsIgnoreCase(WCConstants.APPL_TYPE_UPDATE_VOLUMETRIC)) {
			return updateVolumetricCharges(waterConnectionRequest);
		}
		if (wsUtil.isModifyConnectionRequest(waterConnectionRequest)) {
			// Received request to update the connection for modifyConnection WF
			return updateWaterConnectionForModifyFlow(waterConnectionRequest);
		}
		waterConnectionValidator.validateWaterConnection(waterConnectionRequest, WCConstants.UPDATE_APPLICATION);
		mDMSValidator.validateMasterData(waterConnectionRequest, WCConstants.UPDATE_APPLICATION);
		// Property property =
		// validateProperty.getOrValidateProperty(waterConnectionRequest);
		// validateProperty.validatePropertyFields(property,waterConnectionRequest.getRequestInfo());
		Property property = Property.builder().tenantId(waterConnectionRequest.getWaterConnection().getTenantId())
				.build();
		BusinessService businessService = workflowService.getBusinessService(
				waterConnectionRequest.getWaterConnection().getTenantId(), waterConnectionRequest.getRequestInfo(),
				config.getBusinessServiceValue());
		WaterConnection searchResult = getConnectionForUpdateRequest(
				waterConnectionRequest.getWaterConnection().getId(), waterConnectionRequest.getRequestInfo());
		String previousApplicationStatus = workflowService.getApplicationStatus(waterConnectionRequest.getRequestInfo(),
				waterConnectionRequest.getWaterConnection().getApplicationNo(),
				waterConnectionRequest.getWaterConnection().getTenantId(), config.getBusinessServiceValue());
		enrichmentService.enrichUpdateWaterConnection(waterConnectionRequest);
		actionValidator.validateUpdateRequest(waterConnectionRequest, businessService, previousApplicationStatus);
		waterConnectionValidator.validateUpdate(waterConnectionRequest, searchResult, WCConstants.UPDATE_APPLICATION);
		userService.updateUser(waterConnectionRequest, searchResult);
		wfIntegrator.callWorkFlow(waterConnectionRequest, property);
		// call calculator service to generate the demand for one time fee
		calculationService.calculateFeeAndGenerateDemand(waterConnectionRequest, property);
		// check for edit and send edit notification
		waterDaoImpl.pushForEditNotification(waterConnectionRequest);
		// Enrich file store Id After payment
		enrichmentService.enrichFileStoreIds(waterConnectionRequest);
		userService.createUser(waterConnectionRequest);
		// Call workflow
		enrichmentService.postStatusEnrichment(waterConnectionRequest);
		boolean isStateUpdatable = waterServiceUtil.getStatusForUpdate(businessService, previousApplicationStatus);
		waterDao.updateWaterConnection(waterConnectionRequest, isStateUpdatable);		
		enrichmentService.postForMeterReading(waterConnectionRequest, WCConstants.UPDATE_APPLICATION);
		
		//TODO update installment table into enrichmentService
		updateInstallmentsWithConsumerNo(waterConnectionRequest);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}

	private void calculateFeeAndGenerateDemand(WaterConnectionRequest waterConnectionRequest, Property property) {
		if(WCConstants.WATER_RECONNECTION.equals(waterConnectionRequest.getWaterConnection().getApplicationType())
				|| WCConstants.CONNECTION_OWNERSHIP_CHANGE.equals(waterConnectionRequest.getWaterConnection().getApplicationType())) {
			calculationService.calculateFeeAndGenerateDemand(waterConnectionRequest, property);
		}
	}

	/**
	 * Link Mobile number with connection
	 * @param waterConnectionRequest
	 * @return
	 */
	private List<WaterConnection> linkMobileWithWaterConnection(WaterConnectionRequest waterConnectionRequest) {
		userService.linkMobileWithConnectionHolder(waterConnectionRequest);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}

	/**
	 * Search Water connection to be update
	 * 
	 * @param id
	 * @param requestInfo
	 * @return water connection
	 */
	public WaterConnection getConnectionForUpdateRequest(String id, RequestInfo requestInfo) {
		Set<String> ids = new HashSet<>(Arrays.asList(id));
		SearchCriteria criteria = new SearchCriteria();
		criteria.setIds(ids);
		List<WaterConnection> connections = getWaterConnectionsList(criteria, requestInfo);
		if (CollectionUtils.isEmpty(connections)) {
			StringBuilder builder = new StringBuilder();
			builder.append("WATER CONNECTION NOT FOUND FOR: ").append(id).append(" :ID");
			throw new CustomException("INVALID_WATERCONNECTION_SEARCH", builder.toString());
		}

		return connections.get(0);
	}
	
	/**
	 * 
	 * @param waterConnectionRequest
	 * @return
	 */
	private List<WaterConnection> getAllWaterApplications(WaterConnectionRequest waterConnectionRequest) {
		SearchCriteria criteria = SearchCriteria.builder()
				.connectionNumber(waterConnectionRequest.getWaterConnection().getConnectionNo()).build();
		return search(criteria, waterConnectionRequest.getRequestInfo());
	}

	private List<WaterConnection> updateWaterConnectionForModifyFlow(WaterConnectionRequest waterConnectionRequest) {
		int reqType = (WCConstants.METER_REPLACEMENT.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getApplicationType())) ? WCConstants.METER_REPLACE : WCConstants.MODIFY_CONNECTION;
		waterConnectionValidator.validateWaterConnection(waterConnectionRequest, reqType);
		mDMSValidator.validateMasterData(waterConnectionRequest, WCConstants.MODIFY_CONNECTION);
		BusinessService businessService = getBusinessService(waterConnectionRequest);
		
		WaterConnection searchResult = getConnectionForUpdateRequest(
				waterConnectionRequest.getWaterConnection().getId(), waterConnectionRequest.getRequestInfo());
		// Property property =
		// validateProperty.getOrValidateProperty(waterConnectionRequest);
		// validateProperty.validatePropertyFields(property,waterConnectionRequest.getRequestInfo());
		Property property = Property.builder().tenantId(waterConnectionRequest.getWaterConnection().getTenantId())
				.build();
		String previousApplicationStatus = getApplicationStatus(waterConnectionRequest);
		if(previousApplicationStatus==null) {
			previousApplicationStatus = searchResult.getApplicationStatus();
		}
		
		enrichmentService.enrichUpdateWaterConnection(waterConnectionRequest);
		actionValidator.validateUpdateRequest(waterConnectionRequest, businessService, previousApplicationStatus);
		userService.updateUser(waterConnectionRequest, searchResult);
		validateUpdate(waterConnectionRequest, searchResult);
		wfIntegrator.callWorkFlow(waterConnectionRequest, property);
		boolean isStateUpdatable = waterServiceUtil.getStatusForUpdate(businessService, previousApplicationStatus);
		// setting status as Inactive for closed connection
		inactiveConnection(waterConnectionRequest);
		markOldApplicationForReject(waterConnectionRequest);
		waterDao.updateWaterConnection(waterConnectionRequest, isStateUpdatable);
		calculateFeeAndGenerateDemand(waterConnectionRequest, property);
		// setting oldApplication Flag
		markOldApplication(waterConnectionRequest);
		// check for edit and send edit notification
		waterDaoImpl.pushForEditNotification(waterConnectionRequest);
		int reqInt = (waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.METER_REPLACEMENT)) ? WCConstants.METER_REPLACE : WCConstants.MODIFY_CONNECTION;
		//Temporary solution for exectuionDate and meterInstallationDate
		changeExecutionDate(waterConnectionRequest);
		enrichmentService.postForMeterReading(waterConnectionRequest, reqInt);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}
	
	
	/**
	 * Added by kaustubh
	 * To change the connectionExecutionDate to meterInstallationDate
	 * This change was necessary as the process under createMeter function takes connectionExecutionDate rather than meterInstallationDate
	 */
	private void changeExecutionDate(WaterConnectionRequest waterConnectionRequest) {
		waterConnectionRequest.getWaterConnection().setConnectionExecutionDate(waterConnectionRequest.getWaterConnection().getMeterInstallationDate());
	}

	/**
	 * 
	 * @param waterConnectionRequest
	 * @param searchResult
	 */
	private void validateUpdate(WaterConnectionRequest waterConnectionRequest, WaterConnection searchResult) {
		if (null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.DISCONNECT_WATER_CONNECTION)) {
					waterConnectionValidator.validateUpdate(waterConnectionRequest, searchResult, WCConstants.DISCONNECT_CONNECTION);
		} else if (null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.CLOSE_WATER_CONNECTION)) {
			waterConnectionValidator.validateUpdate(waterConnectionRequest, searchResult, WCConstants.CLOSE_CONNECTION);
		} else if (null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(METER_REPLACEMENT)) {
			waterConnectionValidator.validateUpdate(waterConnectionRequest, searchResult, METER_REPLACE);
		} else {
			waterConnectionValidator.validateUpdate(waterConnectionRequest, searchResult, WCConstants.MODIFY_CONNECTION);
		}
	}

	/**
	 * 
	 * @param waterConnectionRequest
	 * @return
	 */
	private String getApplicationStatus(WaterConnectionRequest waterConnectionRequest) {
		String businessServiceName;
		if(null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.DISCONNECT_WATER_CONNECTION)) {
			businessServiceName = config.getDisconnectWSBusinessServiceName();
		} else if(null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.WATER_RECONNECTION)) {
			businessServiceName = config.getWsWorkflowReconnectionName();
		} else if(null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.CONNECTION_OWNERSHIP_CHANGE)) {
			businessServiceName = config.getWsWorkflowownershipChangeName();
		} else if (null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.CLOSE_WATER_CONNECTION)) {
			businessServiceName = config.getCloseWSBusinessServiceName();
		} else if (null !=  waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.METER_REPLACEMENT)) {
			businessServiceName = config.getWsWorkflowMeterReplacement();
		} else {
			businessServiceName = config.getModifyWSBusinessServiceName();
		}

		return workflowService.getApplicationStatus(waterConnectionRequest.getRequestInfo(),
				waterConnectionRequest.getWaterConnection().getApplicationNo(),
				waterConnectionRequest.getWaterConnection().getTenantId(), businessServiceName);
	}

	/**
	 * get business service
	 * @param waterConnectionRequest
	 * @return
	 */
	private BusinessService getBusinessService(WaterConnectionRequest waterConnectionRequest) {
		String businessServiceName;
		if(null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.DISCONNECT_WATER_CONNECTION)) {
			businessServiceName = config.getDisconnectWSBusinessServiceName();
		} else if(null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.WATER_RECONNECTION)) {
			businessServiceName = config.getWsWorkflowReconnectionName();
		} else if(null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.CONNECTION_OWNERSHIP_CHANGE)) {
			businessServiceName = config.getWsWorkflowownershipChangeName();
		} else if(null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.CLOSE_WATER_CONNECTION)) {
			businessServiceName = config.getCloseWSBusinessServiceName();
		} else if(null != waterConnectionRequest.getWaterConnection().getApplicationType() 
				&& waterConnectionRequest.getWaterConnection().getApplicationType().equalsIgnoreCase(WCConstants.METER_REPLACEMENT) ) {
			businessServiceName = config.getWsWorkflowMeterReplacement();
		}
		else {
			businessServiceName = config.getModifyWSBusinessServiceName();
		}
		
		return workflowService.getBusinessService(
				waterConnectionRequest.getWaterConnection().getTenantId(), waterConnectionRequest.getRequestInfo(), businessServiceName);
	}

	/**
	 * Update oldApplication flag
	 * @param waterConnectionRequest
	 */
	public void markOldApplication(WaterConnectionRequest waterConnectionRequest) {
		if (isApplicableForOldApplicationUpdate(waterConnectionRequest)) {
			String currentModifiedApplicationNo = waterConnectionRequest.getWaterConnection().getApplicationNo();
			List<WaterConnection> previousConnectionsList = getAllWaterApplications(waterConnectionRequest);

			for (WaterConnection waterConnection : previousConnectionsList) {
				if (!waterConnection.getOldApplication()
						&& !(waterConnection.getApplicationNo().equalsIgnoreCase(currentModifiedApplicationNo))) {
					waterConnection.setOldApplication(Boolean.TRUE);
					WaterConnectionRequest previousWaterConnectionRequest = WaterConnectionRequest.builder()
							.requestInfo(waterConnectionRequest.getRequestInfo()).waterConnection(waterConnection)
							.build();
					waterDao.updateWaterConnection(previousWaterConnectionRequest, Boolean.TRUE);
				}
			}
		}
	}

	/**
	 * Checking when to update the oldApplication flag
	 * @param waterConnectionRequest
	 * @return
	 */
	private boolean isApplicableForOldApplicationUpdate(WaterConnectionRequest waterConnectionRequest) {
		boolean isApplicable = false;
		if(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction().equalsIgnoreCase(APPROVE_CONNECTION)) {
			isApplicable = true;
		}
		if(WCConstants.ACTION_DISCONNECT_CONNECTION.equals(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			isApplicable = true;
		}
		if(WCConstants.WATER_RECONNECTION.equals(waterConnectionRequest.getWaterConnection().getApplicationType())
				&& WCConstants.ACTIVATE_CONNECTION.equals(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			isApplicable = true;
		}
		if(WCConstants.CONNECTION_OWNERSHIP_CHANGE.equals(waterConnectionRequest.getWaterConnection().getApplicationType())
						&& WCConstants.ACTION_PAY.equals(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			isApplicable = true;
		}
		if(WCConstants.ACTION_CLOSE_CONNECTION.equals(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			isApplicable = true;
		}
		if(WCConstants.METER_REPLACEMENT.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getApplicationType()) 
				&& WCConstants.APPROVE_CONNECTION.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			isApplicable = true;
		}
		
		return isApplicable;
	}
	
	/**
	 * Set inactive status
	 * @param waterConnectionRequest
	 */
	private void inactiveConnection(WaterConnectionRequest waterConnectionRequest) {
		if (waterConnectionRequest.getWaterConnection().getProcessInstance().getAction()
				.equalsIgnoreCase(WCConstants.ACTION_CLOSE_CONNECTION)) {
					waterConnectionRequest.getWaterConnection().setStatus(StatusEnum.INACTIVE);
		}
	}
	
	@Override
	public List<WaterConnection> migrateWaterConnection(WaterConnectionRequest waterConnectionRequest) {
		int reqType = WCConstants.CREATE_APPLICATION;
		mDMSValidator.validateMasterForCreateRequest(waterConnectionRequest);
		enrichmentService.enrichWaterConnectionForMigration(waterConnectionRequest, reqType);
		waterConnectionRequest.getWaterConnection().setApplicationStatus(APPLICATION_STATUS_ACTIVATED);
		userService.createUserForMigration(waterConnectionRequest);
		enrichmentService.setConnectionNO(waterConnectionRequest);
		waterDao.saveWaterConnection(waterConnectionRequest);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}
	
	private void markOldApplicationForReject(WaterConnectionRequest waterConnectionRequest) {
		if(REJECT_CONNECTION.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			waterConnectionRequest.getWaterConnection().setOldApplication(Boolean.TRUE);
		}
		
	}
	
	@Override
	public List<WaterConnection> migrateSewerageConnection(WaterConnectionRequest waterConnectionRequest) {
		
		Property property = Property.builder().tenantId(waterConnectionRequest.getWaterConnection().getTenantId())
				.build();
		WaterConnection searchResult = getConnectionForUpdateRequest(
				waterConnectionRequest.getWaterConnection().getId(), waterConnectionRequest.getRequestInfo());
		enrichmentService.enrichUpdateWaterConnection(waterConnectionRequest);
		waterConnectionValidator.validateUpdate(waterConnectionRequest, searchResult, WCConstants.UPDATE_APPLICATION);
		waterDao.updateWaterConnection(waterConnectionRequest, true);
		return Arrays.asList(waterConnectionRequest.getWaterConnection());
	}
	
	/**
	 * Update volumetric charges in additionalDetails
	 * 
	 * @param waterConnectionRequest
	 * @return
	 */
	private List<WaterConnection> updateVolumetricCharges(WaterConnectionRequest waterConnectionRequest) {
		WaterConnection searchResult = getConnectionForUpdateRequest(
				waterConnectionRequest.getWaterConnection().getId(), waterConnectionRequest.getRequestInfo());

		HashMap<String, Object> additionalDetailsFromDb = mapper.convertValue(searchResult.getAdditionalDetails(),
				HashMap.class);
		HashMap<String, Object> additionalDetailsFromRequest = mapper
				.convertValue(waterConnectionRequest.getWaterConnection().getAdditionalDetails(), HashMap.class);
		switch (searchResult.getConnectionFacility()) {
		case SERVICE_WATER:
			additionalDetailsFromDb.put(WCConstants.ADDITIONAL_DETAIL_VOLUMETRIC_WATER_CHARGE,
					additionalDetailsFromRequest.get(WCConstants.ADDITIONAL_DETAIL_VOLUMETRIC_WATER_CHARGE));
			break;
		case SERVICE_SEWERAGE:
			additionalDetailsFromDb.put(ADDITIONAL_DETAIL_MIGRATED_SEWERAGE_FEE,
					additionalDetailsFromRequest.get(ADDITIONAL_DETAIL_MIGRATED_SEWERAGE_FEE));
			break;
		case SERVICE_WATER_SEWERAGE:
			additionalDetailsFromDb.put(WCConstants.ADDITIONAL_DETAIL_VOLUMETRIC_WATER_CHARGE,
					additionalDetailsFromRequest.get(WCConstants.ADDITIONAL_DETAIL_VOLUMETRIC_WATER_CHARGE));
			additionalDetailsFromDb.put(ADDITIONAL_DETAIL_MIGRATED_SEWERAGE_FEE,
					additionalDetailsFromRequest.get(ADDITIONAL_DETAIL_MIGRATED_SEWERAGE_FEE));
			break;
		}
		// push to DB-
		searchResult.setAdditionalDetails(additionalDetailsFromDb);
		waterConnectionRequest.setWaterConnection(searchResult);
		enrichmentService.enrichUpdateWaterConnection(waterConnectionRequest);
		waterDao.updateWaterConnection(waterConnectionRequest, true);

		List<WaterConnection> returnList = new ArrayList<>();
		returnList.add(searchResult);
		return returnList;
	}
	
	/**
	 * Update eg_ws_installment table with connection no once the connection has been activated
	 * @param waterConnectionRequest
	 */
	private void updateInstallmentsWithConsumerNo(WaterConnectionRequest waterConnectionRequest) {
		if (WCConstants.ACTIVATE_CONNECTION
				.equalsIgnoreCase(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			WaterConnection waterConnection = waterConnectionRequest.getWaterConnection();
			List<Installments> installments = waterDao.getAllInstallmentsByApplicationNo(waterConnection.getTenantId(), waterConnection.getApplicationNo());
			for(Installments installment : installments) {
				installment.setConsumerNo(waterConnection.getConnectionNo());
				installment.getAuditDetails().setLastModifiedBy(waterConnectionRequest.getRequestInfo().getUserInfo().getUuid());
				installment.getAuditDetails().setLastModifiedTime(System.currentTimeMillis());
			}
			waterConnectionProducer.push(config.getWsUpdateInstallmentTopic(), InstallmentsRequest.builder()
					.requestInfo(waterConnectionRequest.getRequestInfo()).installments(installments).build());
		}
	}
}

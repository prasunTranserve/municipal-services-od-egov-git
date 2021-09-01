package org.egov.noc.thirdparty.nma.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.ServiceRequestRepository;
import org.egov.noc.thirdparty.nma.model.NmaArchRegRequest;
import org.egov.noc.thirdparty.nma.model.NmaArchitectRegistration;
import org.egov.noc.thirdparty.nma.model.NmaUser;
import org.egov.noc.thirdparty.nma.repository.NmaArchitectRegistrationRepo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Slf4j
@Service
public class NmaArchitectRegistrationService {

	@Autowired
	private NmaArchitectRegistrationRepo registrationRepo;

	@Autowired
	private NOCConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	private NmaUtility nmaUtility;

	public NmaArchitectRegistration validateNmaArchitectRegistration(NmaUser user) {
		log.info("validateNmaArchitectRegistration called for " + user);
		String department=user.getTenantid();
		user.setDepartment(department);
		NmaArchitectRegistration nmaArchitectRegistration = registrationRepo.search(user.getUserid(),user.getDepartment());
		if (nmaArchitectRegistration==null) {
			NmaArchRegRequest archRegRequest=new NmaArchRegRequest();
			archRegRequest.setUsers(Arrays.asList(new NmaUser[] { user }));
			nmaArchitectRegistration=registerArchToNmaDept(archRegRequest);
		}

		return nmaArchitectRegistration;
	}

	private NmaArchitectRegistration registerArchToNmaDept(NmaArchRegRequest nmaArchRegRequest) {
		StringBuilder uri = getNmaArchRegURL();
		NmaArchitectRegistration architectRegistration=null;
		String response = null;
		try {
			log.debug("Registering arch with nma : " + nmaArchRegRequest);
			response = (String) nmaUtility.fetchResult(uri,nmaArchRegRequest);
			Map<String, JSONArray> responseMap=JsonPath.parse(response).json();
			//Map<String, JSONArray> responseMap=JsonPath.parse("{\"Users\":[{\"EmailId\":\"rezakhan9494@gmail.com\",\"Token\":\"dlb5d6w4\",\"UniqueId\":\"\"}]}").json();

			log.debug("Registered arch with nma : " + responseMap);
			JSONArray  jsonArray = responseMap.get("Users");
			Map<String, String> map=(Map<String, String>) jsonArray.get(0);
			
			if(map.get("Token")!=null) {
				NmaUser user=nmaArchRegRequest.getUsers().get(0);
				architectRegistration=new NmaArchitectRegistration();
				architectRegistration.setTenantid(user.getTenantid());
				architectRegistration.setUserid(user.getUserid());
				architectRegistration.setToken(map.get("Token"));
				architectRegistration.setUniqueid(map.get("UniqueId"));
				registrationRepo.save(architectRegistration);
			}else {
				throw new CustomException("NMA Arch reg", " Failed to Registered Arch " + map);
			}
			
		} catch (Exception se) {
			se.printStackTrace();
			throw new CustomException("NMA Arch reg", " Failed to Registered Arch " + nmaArchRegRequest);
		}
		return architectRegistration;
	}

	private StringBuilder getNmaArchRegURL() {
		// TODO Auto-generated method stub
		StringBuilder url = new StringBuilder(config.getNmaHost());
		url.append(config.getNmaContextPath());
		url.append(config.getNmaArchitectRegistration());
		return url;
	}
}

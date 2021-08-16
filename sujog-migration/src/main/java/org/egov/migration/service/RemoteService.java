package org.egov.migration.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RemoteService {
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private ObjectMapper mapper;

	/**
	 * Fetches results from a REST service using the uri and object
	 * 
	 * @return Object
	 * @author vishal
	 */
	public Map fetchResult(StringBuilder uri, Object request) {

		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		Map response = null;
		log.info("URI: "+uri.toString());
		try {
			log.info("Request: "+mapper.writeValueAsString(request));
			response = restTemplate.postForObject(uri.toString(), request, Map.class);
		} catch (HttpClientErrorException e) {
			e.printStackTrace();
			//log.error("External Service threw an Exception: ", e);
		} catch (Exception e) {
			e.printStackTrace();
			//log.error("Exception while fetching from external service: ", e);
		}
		return response;
	}
}

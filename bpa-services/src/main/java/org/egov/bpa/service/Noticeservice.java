package org.egov.bpa.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;


import org.egov.bpa.repository.ScnRepository;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service
public class Noticeservice {
	
	@Autowired
	private ScnRepository repository;
	
	@Autowired
	EnrichmentService enrichmentService;

	public Notice create(@Valid NoticeRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		enrichmentService.enrichScnCreateRequestV2(request);
		ValidateCreateRequest(request);
		repository.save(request);
		
		return request.getnotice();
	}

	private void ValidateCreateRequest(@Valid NoticeRequest request) {
		
	NoticeSearchCriteria criteria = new NoticeSearchCriteria();
	//criteria.setBusinessid(request.getnotice().getBusinessid());
	criteria.setLetterNo(request.getnotice().getLetterNo());
	List<Notice> noticeSearchResult = repository.getNoticeData(criteria);
	if (!CollectionUtils.isEmpty(noticeSearchResult)) {
		throw new CustomException("Create Error",
				"Failed to create found duplicate letter no.");
	}
		
	}

	public List<Notice> searchNotice(@Valid  NoticeSearchCriteria criteria) {
		// TODO Auto-generated method stub
		Map<String, String> errorMap = new HashMap<>();
		if(criteria.getBusinessid()==null) {
			errorMap.put("SearchError","please provide bussiness id to search a  notice.");
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
		else {
		List<Notice> noticeSearchResult = repository.getNoticeData(criteria);
		//System.out.println("result:"+noticeSearchResult);
		if(noticeSearchResult.isEmpty()) {
			return Collections.emptyList();
		}else {
		return noticeSearchResult;
		
	}
		}
	}

}

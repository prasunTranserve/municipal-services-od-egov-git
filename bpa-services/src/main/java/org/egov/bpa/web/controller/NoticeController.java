package org.egov.bpa.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.service.Noticeservice;

import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.Notice;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.NoticeResponse;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.RequestInfoWrapper;




import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notice")
public class NoticeController {
	
	
	@Autowired
	private Noticeservice noticeService;
	
	
	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	
	
	@PostMapping(value = "/_create")
	public ResponseEntity<NoticeResponse> create(
			@Valid @RequestBody NoticeRequest request) {
		bpaUtil.defaultJsonPathConfig();
		//System.out.println(request.getScnnotice().getBpaApplicationNumber());
		Notice notices = noticeService.create(request);
		List<Notice> notice = new ArrayList<Notice>();
		notice.add(notices);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(request.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping(value = "/_search")
	public ResponseEntity<NoticeResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute NoticeSearchCriteria criteria) {
		List<Notice> notice = noticeService.searchNotice(criteria);
		//System.out.println("responsed:"+notice);
		NoticeResponse response = NoticeResponse.builder().notice(notice)
				.responseInfo(responseInfoFactory
						.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		//System.out.println("response:"+response);
		return new ResponseEntity<>(response, HttpStatus.OK);
		
		
	}

}

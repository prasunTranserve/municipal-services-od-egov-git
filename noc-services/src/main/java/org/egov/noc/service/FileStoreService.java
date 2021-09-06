package org.egov.noc.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.ServiceRequestRepository;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.web.model.Document;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.ServiceCallException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;



@Service
public class FileStoreService {

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private NOCConfiguration config;

	public String getFileStorePath(String tenantId, String fileStoreIds) {
		String downloadLink = null;
		String[] t = tenantId.split("\\.");
		StringBuilder uri = new StringBuilder(config.getFilestoreHost());
		uri.append(config.getFilestoreContext());
		uri.append(config.getFilestoreFileurlPath());
		uri.append("?").append("tenantId=").append(t[0]);
		uri.append("&").append("fileStoreIds=").append(fileStoreIds);
		LinkedHashMap responseMap = null;
		try {
			responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(uri);
		} catch (ServiceCallException se) {
			throw new CustomException(NOCConstants.FILE_STORE_ERROR, "fileStoreIds Number is Invalid");
		}
		if (responseMap != null) {
			String jsonString = new JSONObject(responseMap).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			List<String> list = context.read("fileStoreIds.*.url");

			if (list != null && list.size() > 0)
				downloadLink = list.get(0);
		}
		return downloadLink;
		// return
		// "https://od-digit.s3-ap-south-1.amazonaws.com/od/undefined/August/20/1629463465165KLVsjtXZUh.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIA3CQUPMIX3BJDAIVF%2F20210825%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20210825T095058Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host&X-Amz-Signature=8640071b8286c4755c132b7e5c0488382bc0192aeac2d961ea63104b4a1ae5ea";
	}

	@Autowired
	private RestTemplate restTemplate;
	
	public List<Document> copyDocuments(String tenantId,String module,String documentType,List<String> fileUrl) {
		List<Document> documents=new ArrayList<>();
		for (String url : fileUrl) {
			try {
				String[] t = tenantId.split("\\.");
				File file=new File("tempfile-nma-update.pdf");
				FileUtils.copyURLToFile(new URL(url), file);
				StringBuilder uri = new StringBuilder(config.getFilestoreHost());
				uri.append(config.getFilestoreContext());
				uri.append(config.getFilestorefilestorepath());
				uri.append("?").append("tenantId=").append(t[0]);
				uri.append("&").append("module=").append(module);
				
	            HttpMethod requestMethod = HttpMethod.POST;
	            HttpHeaders headers = new HttpHeaders();
	            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
	            MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
	            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	            body.add("file", new FileSystemResource(file));
	            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
	            ResponseEntity<String> response = restTemplate.exchange(uri.toString(), requestMethod, requestEntity, String.class);
	            DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(response.getBody());
	            List<String> list=context.read("files.*.fileStoreId");
	           
	            String fileStoreId=null;
	            if(list!=null && list.size()>0)
	            	fileStoreId=list.get(0);
	            Document document=new Document();
	            document.setFileStoreId(fileStoreId);
	            document.setDocumentType(documentType);
	            documents.add(document);
	          				FileUtils.forceDelete(file);
			} catch (IOException  e) {
				e.printStackTrace();
				throw new CustomException(NOCConstants.FILE_STORE_ERROR, "Invalid download link");
			}catch (Exception se) {
				se.printStackTrace();
				throw new CustomException(NOCConstants.FILE_STORE_ERROR, "File copy error");
			}
		}
		return documents;
	}

}

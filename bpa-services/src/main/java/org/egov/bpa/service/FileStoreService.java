package org.egov.bpa.service;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.tracer.model.CustomException;
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
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStoreService {
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	BPAConfiguration bpaConfig;

	public File fetch(String fileStoreId, String moduleName,String localFileName, String tenantId) {

        fileStoreId = normalizeString(fileStoreId);
        moduleName = normalizeString(moduleName);
		String urls = bpaConfig.getFilestoreHost() + bpaConfig.getFilestoreFetchPath() + "?tenantId=" + tenantId
				+ "&fileStoreId=" + fileStoreId;
		log.info(String.format("fetch file from url   %s   ", urls));

        Path path = Paths.get(localFileName);
        try {
            RequestCallback requestCallback = request -> request.getHeaders()
                    .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
            ResponseExtractor<Void> responseExtractor = response -> {
                Files.copy(response.getBody(), path);
                return null;
            };
            restTemplate.execute(URI.create(urls), HttpMethod.GET, requestCallback, responseExtractor);
        } catch (RestClientException e) {
			log.error("Rest Error occurred while fetching file", e);
			throw new CustomException("Error while fetching file from filestore",
					"Error while fetching file from filestore");
		} catch (Exception ex) {
			log.error("Error occurred while fetching file", ex);
			throw new CustomException("Error while fetching file from filestore",
					"Error while fetching file from filestore");
		}

        log.info("fetch completed....   ");
        return path.toFile();

    }
	
	public Object upload(File file,String fileName,String mimeType,String moduleName,String tenantId) {
		String url = bpaConfig.getFilestoreHost() + bpaConfig.getFilestoreUploadPath();
		fileName = normalizeString(fileName);
        mimeType = normalizeString(mimeType);
        moduleName = normalizeString(moduleName);
        HttpHeaders headers = new HttpHeaders();
        log.info(String.format("Uploaded file   %s   with size  %s ", file.getName(), file.length()));

        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("file", new FileSystemResource(file.getName()));
        map.add("tenantId", tenantId);
        map.add("module", moduleName);
        ResponseEntity<Object> result = null;
        try {
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map,
                headers);
        result = restTemplate.postForEntity(url, request, Object.class);
        } catch (RestClientException e) {
            log.error("Rest Exception occurred while uploading file", e);
			throw new CustomException("Error while uploading file to filestore",
					"Error while uploading file to filestore");
		} catch (Exception ex) {
			log.error("Error occurred while uploading file", ex);
			throw new CustomException("Error while uploading file to filestore",
					"Error while uploading file to filestore");
		}
        log.info("file upload completed...");
        return result.getBody();
	}
	
	public static String normalizeString(String fileName) {
        fileName = Normalizer.normalize(fileName, Form.NFKC);
        Pattern pattern = Pattern.compile("[<>]");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            // Found blacklisted tag
            throw new IllegalStateException();
        }
        return fileName;
    }
}

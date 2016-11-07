package fi.csc.virtu.samlxmltooling;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.tomcat.util.http.fileupload.UploadContext;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;
import fi.csc.virtu.samlxmltooling.publish.PublishController;
import fi.csc.virtu.samlxmltooling.publish.PublishController.ops;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class PublishControllerTest {

	@Autowired
	private TestRestTemplate restTmpl;
	
	@LocalServerPort
	private int port;

	private final static String VIRTU_MD_URL = "https://virtu-ds.csc.fi/fed/virtu/virtu-metadata-v3.xml";
	private final static String TMP_FILESUFFIX = "saml-metadata-test.xml";
	
	private String ctrlUrl;
	private String diffUrl;
	
	@Before
	public void init() {
		ctrlUrl = "http://localhost:" + port + "/publish/ctrl";
		diffUrl = "http://localhost:" + port + "/publish/diff";
	}
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	private String[] postFile() throws IOException {
		File file = File.createTempFile(
				RandomStringUtils.randomAlphanumeric(7), 
				TMP_FILESUFFIX);
		FileUtils.copyURLToFile(new URL(VIRTU_MD_URL), file);
		
		
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add(PublishController.POSTFILE_PAR, new FileSystemResource(file));
		
		HttpHeaders hdrs = new HttpHeaders();
		hdrs.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<LinkedMultiValueMap<String, Object>> request = 
				 new HttpEntity<LinkedMultiValueMap<String,Object>>(map, hdrs);
		
	
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> resp =
				this.restTmpl.postForEntity(ctrlUrl, request, Map.class);
				
		String status = (String) resp.getBody().get(DiffController.STATUS_STR);
		
		String sessHdr = resp.getHeaders().get("Set-Cookie").stream()
			.collect(Collectors.joining(";"));
		return new String[]{sessHdr, status};
	}
	
	@Test
	public void postFileTest() throws IOException {
		String[] ret = postFile();
		String hdrs = ret[0];
		String status = ret[1];
		assertThat(status)
		.contains(DiffController.OK_STR);
		assertThat(hdrs).isNotNull();
	}
	
	@Test
	public void getDiffTest() throws IOException {
		String sessionStr = postFile()[0];
		HttpHeaders hdrs = new HttpHeaders();
		hdrs.set("Cookie", sessionStr);
		HttpEntity<Object> request = new HttpEntity<>(hdrs);
		HttpStatus status = this.restTmpl.exchange(diffUrl,
				HttpMethod.GET,
				request,
				String.class
				).getStatusCode();
		assertThat(status.value()).isEqualTo(200);
	}
	
	@Test
	public void getOpTest() throws IOException {
		String sessionStr = postFile()[0];
		HttpHeaders hdrs = new HttpHeaders();
		hdrs.set("Cookie", sessionStr);
		HttpEntity<Object> request = new HttpEntity<>(hdrs);
		for (ops op: PublishController.ops.values()) {
			if (!op.equals(PublishController.ops.prePublishChecks)) {
				log.debug("-- op: " + op.toString());
				String status = (String) this.restTmpl.exchange(
						ctrlUrl + "?op=" + op.toString(),
						HttpMethod.GET,
						request,
						Map.class
						).getBody().get(DiffController.STATUS_STR);
				log.debug("-- status: " + status);
				assertThat(
					status
					).isEqualTo(DiffController.OK_STR);
			}				
		}
		@SuppressWarnings("unchecked")
		Map<String, String> retMap = this.restTmpl.exchange(
				ctrlUrl + "?op=" + PublishController.ops.prePublishChecks.toString(),
				HttpMethod.GET,
				request,
				Map.class
				).getBody();
		for (String key: retMap.keySet()) {
			assertThat(retMap.get(key)).isEqualTo(DiffController.OK_STR);
		}
	}
	
}

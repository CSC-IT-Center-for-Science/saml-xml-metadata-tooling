package fi.csc.virtu.samlxmltooling;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class DiffControllerTest {

	@Autowired
	private TestRestTemplate restTmpl;
	
	@LocalServerPort
	private int port;

	private String ctrlUrl;
	private String baseUrl;
	
	@Before
	public void init() {
		baseUrl = "http://localhost:" + port + "/";
		ctrlUrl = baseUrl + "ctrl/";
	}
	
	public String getTask() {
		HttpHeaders hdrs = new HttpHeaders();
		HttpEntity<Object> request = new HttpEntity<>(hdrs);
		ResponseEntity<String> resp = this.restTmpl.exchange(ctrlUrl + "?op=reqTask&flavor=VIRTU",
				HttpMethod.GET,
				request,
				String.class
				);
		
		return resp.getHeaders().get("Set-Cookie").stream()
				.collect(Collectors.joining(";"));
	}
	
	@Test
	public void getTaskTest() {
		HttpHeaders hdrs = new HttpHeaders();
		HttpEntity<Object> request = new HttpEntity<>(hdrs);
		int status = this.restTmpl.exchange(ctrlUrl + "?op=reqTask",
				HttpMethod.GET,
				request,
				String.class
				).getStatusCodeValue();
		
		assertThat(status).isEqualTo(200);
	}
	
	@Test
	public void postFileTest() throws IOException {
		Map<String, String> otherPars = new HashMap<String, String>();
		otherPars.put(DiffController.USAGE_PARAM_STR, "baseFile");
		String[] retArray = 
				PublishControllerTest.postFile(baseUrl + "fileUpload",
						DiffController.FILE_PARAM_STR,
						otherPars,
						this.restTmpl);
		assertThat(retArray[1]).isEqualTo(DiffController.OK_STR);
	}
	
	
}

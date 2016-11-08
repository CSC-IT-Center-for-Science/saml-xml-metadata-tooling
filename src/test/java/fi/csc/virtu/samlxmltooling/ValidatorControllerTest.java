package fi.csc.virtu.samlxmltooling;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
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
import fi.csc.virtu.samlxmltooling.validator.ValidatorController;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ValidatorControllerTest {

	@Autowired
	private TestRestTemplate restTmpl;
	
	@LocalServerPort
	private int port;

	private String ctrlUrl;
	private String baseUrl;
	
	@Before
	public void init() {
		baseUrl = "http://localhost:" + port + "/validate/";
		ctrlUrl = baseUrl + "ctrl";
	}
	
	
	public String getTask() {
		HttpHeaders hdrs = new HttpHeaders();
		HttpEntity<Object> request = new HttpEntity<>(hdrs);
		ResponseEntity<String> resp = this.restTmpl.exchange(ctrlUrl + "?op=reqTask",
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
	public void opsTest() {
		String sessionStr = getTask();
		HttpHeaders hdrs = new HttpHeaders();
		hdrs.set("Cookie", sessionStr);
		HttpEntity<Object> request = new HttpEntity<>(hdrs);

		for(ValidatorController.ops op: ValidatorController.ops.values()) {
			if (!op.equals(ValidatorController.ops.reqTask)) {
				new HttpEntity<>(hdrs);
				String status = (String) this.restTmpl.exchange(ctrlUrl + "?op=" + op.toString(),
						HttpMethod.GET,
						request,
						Map.class
						).getBody().get(DiffController.STATUS_STR);	
				assertThat(status).isEqualTo(DiffController.OK_STR);
			}
		}
	}
	
	@Test
	public void getOpsTest() {
		String[] paths = new String[]{"getOps", "getFlavors"};
		for(String path: paths) {
			@SuppressWarnings("unchecked")
			List<String> list = this.restTmpl.getForEntity(baseUrl + path,
					List.class).getBody();
			assertThat(list.size()).isGreaterThan(0);
			
		}
	}
}

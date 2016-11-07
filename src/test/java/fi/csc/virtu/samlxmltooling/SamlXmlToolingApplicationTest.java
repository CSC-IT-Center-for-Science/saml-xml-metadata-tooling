package fi.csc.virtu.samlxmltooling;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;
import fi.csc.virtu.samlxmltooling.publish.PublishController;
import fi.csc.virtu.samlxmltooling.validator.ValidatorController;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SamlXmlToolingApplicationTest {
	
	@Autowired
	DiffController diffCtrl;
	@Autowired
	ValidatorController validCtrl;
	@Autowired
	PublishController publCtrl;

	@Test
	public void contextLoads() {
		assertNotNull(diffCtrl);
		assertNotNull(validCtrl);
		assertNotNull(publCtrl);
	}
	
	

}

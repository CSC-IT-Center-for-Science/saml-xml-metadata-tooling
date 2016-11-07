package fi.csc.virtu.samlxmltooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffController;
import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;
import fi.csc.virtu.samlxmltooling.publish.PublishController;
import fi.csc.virtu.samlxmltooling.publish.PublishTask;
import fi.csc.virtu.samlxmltooling.tools.GeneralStrings;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;
import fi.csc.virtu.samlxmltooling.validator.ValidatorController;
import fi.csc.virtu.samlxmltooling.validator.ValidatorController.ops;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PublishTaskTest {
	
	private final static String VIRTU_MD_URL = "https://virtu-ds.csc.fi/fed/virtu/virtu-metadata-v3.xml";
	private final static String TMP_FILESUFFIX = "saml-metadata-test.xml";
	private File localTmpMdFile;

	@Autowired
	SamlDocBuilder docBuilder;
	
	@Autowired
	MainConfiguration conf;
	
	private PublishTask task;
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Before
	public void init() throws IOException {
		this.task = new PublishTask(GeneralStrings.PROP_FED_FLAVOR_TEST, docBuilder);
		
		localTmpMdFile = File.createTempFile(
				RandomStringUtils.randomAlphanumeric(7), 
				TMP_FILESUFFIX);
		FileUtils.copyURLToFile(new URL(VIRTU_MD_URL), localTmpMdFile);
	}

	@Test
	public void fileIsDocifiableTest() throws IOException, ParserConfigurationException, SAXException {
		Document doc = docBuilder.getCurrent(GeneralStrings.PROP_FED_FLAVOR_TEST);
		assertNotNull(doc);
		task.fileIsDocifiable(localTmpMdFile);
	}
	
	@Test
	public void isActiveTest() {
		assertTrue(task.isActive());
	}
	
	@Test
	public void isInitTest() throws IOException, ParserConfigurationException, SAXException {
		fileIsDocifiableTest();
		assertTrue(task.isInit());
	}
	
	@Test
	public void runPrePublishChecksTest() throws IOException, ParserConfigurationException, SAXException {
		fileIsDocifiableTest();
		for (ops test: ValidatorController.ops.values()) {
			if (test != ValidatorController.ops.reqTask) {
				assertThat(task.runPrePublishChecks(conf))
				.contains(entry(test.toString(), DiffController.OK_STR));
			}
		}
	}
	
	@Test
	public void getFlavorTest() {
		assertThat(task.getFlavor())
			.contains(GeneralStrings.PROP_FED_FLAVOR_TEST)
			;
	}
	
	@Test
	public void getDocumentTest() throws IOException, ParserConfigurationException, SAXException {
		fileIsDocifiableTest();
		assertNotNull(task.getDocument());
	}
	
	@Test
	public void compareTest() throws IOException, ParserConfigurationException, SAXException {
		fileIsDocifiableTest();
		assertThat(task.compare(conf))
			.containsEntry(DiffController.STATUS_STR, DiffController.OK_STR)
			;
	}
	
	@Test
	public void execPublishTest() throws IOException, ParserConfigurationException, SAXException {
		fileIsDocifiableTest();
		task.execPublish(conf);
	}
	
	private boolean publishStatuscheck(Map<String, String> retMap) {
		String status;
		if (retMap.containsKey(PublishController.PUBLISH_STATUS)) {
			status = (String) retMap.get(PublishController.PUBLISH_STATUS);
		} else {
			return false;
		}
	return status.equals(PublishTask.publishStatus.running.toString()) ||
			status.equals(PublishTask.publishStatus.okReady.toString());
	}
	
	@Test
	public void translateExitLevel() throws IOException, ParserConfigurationException, SAXException {
		execPublishTest();
		LocalDateTime startTime = LocalDateTime.now();
		long secs = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
		String stat = task.getPublishStatus().get(PublishController.PUBLISH_STATUS);
		log.debug("-- publishStatus: " + stat);
		while (secs < 90 && stat.equals(PublishTask.publishStatus.running.toString())) {
			assertTrue(publishStatuscheck(task.getPublishStatus()));
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			secs = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
			stat = task.getPublishStatus().get(PublishController.PUBLISH_STATUS);
			log.debug("-- publishStatus: " + stat);
			log.debug("-- secs: " + secs);
		}
	}
	
}

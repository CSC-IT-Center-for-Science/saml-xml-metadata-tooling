package fi.csc.virtu.samlxmltooling.publish;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fi.csc.virtu.samlxmltooling.Task;
import fi.csc.virtu.samlxmltooling.diffservlet.MainConfiguration;
import fi.csc.virtu.samlxmltooling.tools.CertTool;
import fi.csc.virtu.samlxmltooling.tools.ControllerTools;
import fi.csc.virtu.samlxmltooling.tools.GeneralStrings;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;
import fi.csc.virtu.samlxmltooling.validator.ValidatorTask;

public class PublishTask implements Task {
	
	private LocalDateTime latestAccess = LocalDateTime.now();
	private File publishFile;
	private Document publishDoc;
	private String myFlavor;
	private SamlDocBuilder builder;
	private publishStatus myPublishStatus = publishStatus.notStarted;
	private LocalDateTime publishStarted;
	private LocalDateTime publishEnd;
	private Process myProcess;
	private String processOut;
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	public enum publishStatus {
		notInit, notStarted, running, okReady, failReady, ioException
	}

	public PublishTask(String flavor, SamlDocBuilder builder) {
		this.myFlavor = flavor;
		this.builder = builder;
	}

	@Override
	public boolean isActive() {
		long secs = ChronoUnit.SECONDS.between(latestAccess, LocalDateTime.now());
		return secs < 300;
	}
	
	private void update() {
		latestAccess = LocalDateTime.now();
	}
	
	public boolean fileIsDocifiable(File file) {
		update();
		this.publishFile = file;
		try {
			this.publishDoc = builder.getDoc(file);
			return true;
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean isInit() {
		return this.publishFile != null &&
				this.publishDoc != null;
	}
	
	public Map<String, String> runPrePublishChecks(MainConfiguration conf) {
		if (!isInit()) {
			return ControllerTools.getErrorMap("task not initialized");
		}
		update();
		try {
			ValidatorTask vTask = new ValidatorTask(
					publishDoc, 
					CertTool.getFedCheckCert(myFlavor, conf),
					conf,
					myFlavor);
			return vTask.prePublishChecks();
		} catch (Exception e) {
			return ControllerTools.putErrors(e);
		}
	}
	
	public String getFlavor() {
		return myFlavor;
	}
	
	public Document getDocument() {
		if (isInit()) {
			return publishDoc;
		} else {
			return null;
		}
	}
	
	public void translateExitLevel() {
		if (myProcess == null) {
			myPublishStatus = publishStatus.notStarted;
			return;
		}
		try {
			if (myProcess.exitValue() == 0) {
				myPublishStatus = publishStatus.okReady;
			} else {
				myPublishStatus = publishStatus.failReady;
			}
			if (this.publishEnd == null) {
				this.publishEnd = LocalDateTime.now();
				InputStream is = myProcess.getInputStream();
				this.processOut = IOUtils.toString(is);
				is.close();
			}
		} catch (IllegalThreadStateException e) {
			myPublishStatus = publishStatus.running;
		} catch (IOException e) {
			log.error("error reading command output", e);
		}
	}
	
	public Map<String, String> getPublishStatus() {
		if (!isInit()) {
			return ControllerTools.getErrorMap("not initialized");
		}
		update();
		translateExitLevel();
		Map<String, String> retMap = ControllerTools.getOkMap();
		retMap.put(PublishController.PUBLISH_STATUS,
				this.myPublishStatus.toString());
		if (myPublishStatus == publishStatus.notStarted) {
			retMap.put(PublishController.PUBLISH_RUNNING, "0");
		} else {
			retMap.put(PublishController.PUBLISH_RUNNING, 
					String.valueOf(getSecsRunning()));
		}
		retMap.put(PublishController.PUBLISH_OUT, getPublishOutput());
		return retMap;
	}
	
	public Map<String, String> getExecPublish(MainConfiguration conf) {
		execPublish(conf);
		return getPublishStatus();
	}
	
	public void execPublish (MainConfiguration conf) {
		update();
		if (!isInit()) {
			myPublishStatus = publishStatus.notInit;
			return;
		}
		publishEnd = null;
		processOut = null;
		try {
			String cmd = conf.getFedConfStr(myFlavor, 
					GeneralStrings.PROP_FED_PUBLISH_SCRIPT);
			this.myProcess = Runtime.getRuntime().exec(
					new String[]{"/bin/sh", cmd, "hhh"});
			publishStarted = LocalDateTime.now();
			translateExitLevel();
		} catch (IOException e) {
			log.error("error running publish", e);
			myPublishStatus = publishStatus.ioException;
		}
	}
	
	public long getSecsRunning() {
		LocalDateTime end;
		if (this.publishEnd == null) {
			end = LocalDateTime.now();
		} else {
			end = this.publishEnd;
		}
		long secs = ChronoUnit.SECONDS.between(publishStarted, end);
		return secs;
	}
	
	public String getPublishOutput() {
		return processOut;
	}
	
	public Map<String, String> compare(MainConfiguration conf) {
		Map<String, String> retMap = ControllerTools.getOkMap();
		final String prop = GeneralStrings.PROP_FED_PUBLISH_PUBLIST; 
		String[] urls = conf.getFedConfArray(myFlavor, prop);
		int count = 0;
		for(String str: urls) {
			retMap.put("url# " + count, str);
			count++;
		}
		return retMap;
	}

}

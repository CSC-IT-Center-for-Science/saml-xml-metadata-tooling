package fi.csc.virtu.samlxmltooling.diffservlet;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import fi.csc.virtu.samlxmltooling.diffservlet.DiffTask.TaskFlavor;
import fi.csc.virtu.samlxmltooling.tools.GeneralStrings;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;

@Configuration
@ConfigurationProperties(prefix="my.validator")
public class MainConfiguration {

	@Value("${my.validator.federationSpecs.Haka.mdUrl}")
	private String currentUrlHaka; 

	@Value("${my.validator.federationSpecs.Virtu.mdUrl}")
	private String currentUrlVirtu;
	
	private List<String> federations;
	private String schemaDir;	
	
	@Autowired
	Environment env;
	
	Logger log = LoggerFactory.getLogger(MainConfiguration.class);
	
	public void setFederations(List<String> federations) {
		this.federations = federations;
	}
	public List<String> getFederations() {
		return federations;
	}

	public final TaskFlavor DEFAULT_FLAVOR =
			TaskFlavor.HAKA;
	
	public final String FLAVOR_PARAMETER = "flavor";

	public String getCurrentUrlHaka() {
		return currentUrlHaka;
	}

	public String getCurrentUrlVirtu() {
		return currentUrlVirtu;
	}
	
	public TaskFlavor findFlavorFromRequest (HttpServletRequest req) {
		final String flavorPar = FLAVOR_PARAMETER;
		if (req.getParameter(flavorPar) != null) {
			switch (req.getParameter(flavorPar)) {
				case "VIRTU":
					return TaskFlavor.VIRTU;
				case "HAKA":
					return TaskFlavor.HAKA;
				default:
					return DEFAULT_FLAVOR;
			}
		} else {
			return DEFAULT_FLAVOR;
		}
	}
	
	public String getFedConfStr(String fed, String prop) {
		return env.getProperty(GeneralStrings.PROP_FED_PREFIX +
				fed + "." +
				prop);
	}
	public int getFedConfInt(String fed, String prop) {
		return Integer.parseInt(env.getProperty(GeneralStrings.PROP_FED_PREFIX +
				fed + "." +
				prop));
	}
	
	public String getSchemaDir() {
		return schemaDir;
	}
	public void setSchemaDir (String schemaDir) {
		this.schemaDir = schemaDir;
	}
	
	@Bean
	public SamlDocBuilder samlDocBuilderFactory() {
		return new SamlDocBuilder();
	}
	
}

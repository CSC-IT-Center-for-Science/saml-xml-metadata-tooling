package fi.csc.virtu.samlxmltooling.diffservlet;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.csc.virtu.samlxmltooling.diffservlet.Task.TaskFlavor;
import fi.csc.virtu.samlxmltooling.tools.SamlDocBuilder;

@Configuration
public class MainConfiguration {

	@Value("${my.federations.Haka.mdUrl}")
	private String currentUrlHaka; 

	@Value("${my.federations.Virtu.mdUrl}")
	private String currentUrlVirtu;


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
	
	@Bean
	public SamlDocBuilder samlDocBuilderFactory() {
		return new SamlDocBuilder();
	}
	
}

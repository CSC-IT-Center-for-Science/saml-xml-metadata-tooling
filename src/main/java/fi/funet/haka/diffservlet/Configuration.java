package fi.funet.haka.diffservlet;

import javax.servlet.http.HttpServletRequest;

import fi.funet.haka.diffservlet.Task.TaskFlavor;

public final class Configuration {

	private static final String currentUrlHaka = 
			"https://haka.funet.fi/metadata/haka-metadata.xml";
	private static final String currentUrlVirtu =
			"https://virtu-ds.csc.fi/fed/virtu/virtu-metadata-v3.xml";

	public static final TaskFlavor DEFAULT_FLAVOR =
			TaskFlavor.HAKA;
	
	public static final String FLAVOR_PARAMETER = "flavor";

	public static String getCurrentUrlHaka() {
		return currentUrlHaka;
	}

	public static String getCurrentUrlVirtu() {
		return currentUrlVirtu;
	}
	
	public static TaskFlavor findFlavorFromRequest (HttpServletRequest req) {
		final String flavorPar = FLAVOR_PARAMETER;
		if (req.getParameter(flavorPar) != null) {
			switch (req.getParameter(flavorPar)) {
				case "virtu":
					return TaskFlavor.VIRTU;
				case "haka":
					return TaskFlavor.HAKA;
				default:
					return DEFAULT_FLAVOR;
			}
		} else {
			return DEFAULT_FLAVOR;
		}
	}
	
	
}

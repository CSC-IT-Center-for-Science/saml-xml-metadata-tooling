package fi.funet.haka.diffservlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletHandler;


public class JettyExperiment {

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        
        HashSessionIdManager idmanager = new HashSessionIdManager();
        server.setSessionIdManager(idmanager);
        
        ContextHandler context = new ContextHandler("/");
        server.setHandler(context);
        
        HashSessionManager manager = new HashSessionManager();
        SessionHandler sessions = new SessionHandler(manager);
        context.setHandler(sessions);
        
        ServletHandler sh = new ServletHandler();
        sh.addServletWithMapping(JettyExperimentServlet.class, "/ctrl/*");

        sessions.setHandler(sh);
        
        String path = JettyExperiment.class.getClassLoader().getResource("html/").toExternalForm();
        
        ResourceHandler rh = new ResourceHandler();
        rh.setDirectoriesListed(true);
        rh.setWelcomeFiles(new String[] {"index.html"});
        rh.setResourceBase(path);
        
        HandlerList hl = new HandlerList();
        hl.setHandlers(new Handler[] { rh, sessions, new DefaultHandler() });
        
        server.setHandler(hl);
        server.start();
        server.join();
    }

	
}

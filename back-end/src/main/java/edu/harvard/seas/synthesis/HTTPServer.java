package edu.harvard.seas.synthesis;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

public class HTTPServer {
	public static void main(String[] args) throws Exception {
		Server server = new Server(8080);	
		ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{"index.html"});
        resource_handler.setResourceBase("../front-end/");
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {resource_handler, new DefaultHandler() });
        server.setHandler(handlers);
        server.start();
        server.join();
	}
}

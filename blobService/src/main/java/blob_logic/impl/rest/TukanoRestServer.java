package blob_logic.impl.rest;

import jakarta.ws.rs.core.Application;
import tukano.impl.rest.RestShortsResource;
import tukano.impl.rest.RestUsersResource;
import utils.IP;
import utils.Props;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


public class TukanoRestServer extends Application{
	final private static Logger Log = Logger.getLogger(TukanoRestServer.class.getName());

	static final String INETADDR_ANY = "0.0.0.0";
	static String SERVER_BASE_URI = "http://%s:%s/rest";

	private Set<Object> singletons = new HashSet<>();
	private Set<Class<?>> resources = new HashSet<>();

	public static final int PORT = 8080;

	public static String serverURI;
			
	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	public TukanoRestServer() {
		serverURI = String.format(SERVER_BASE_URI, IP.hostAddress(), PORT);
		resources.add(RestBlobsResource.class);
		resources.add(RestUsersResource.class);
		resources.add(RestShortsResource.class);
		Props.load("azure-keys.props");
		//Token.setSecret("tukano");
	}


//	protected void start() throws Exception {
//
//		ResourceConfig config = new ResourceConfig();
//
//		config.register(RestBlobsResource.class);
//		config.register(RestUsersResource.class);
//		config.register(RestShortsResource.class);
//
//		JdkHttpServerFactory.createHttpServer( URI.create(serverURI.replace(IP.hostname(), INETADDR_ANY)), config);
//
//		Log.info(String.format("Tukano Server ready @ %s\n",  serverURI));
//	}
	
	
	public static void main(String[] args) throws Exception {
//		Args.use(args);
////
		//Token.setSecret( Args.valueOf("-secret", ""));
//////		Props.load( Args.valueOf("-props", "").split(","));
////
////		new TukanoRestServer().start();
		return;
	}

	@Override
	public Set<Class<?>> getClasses() {
		return resources;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}

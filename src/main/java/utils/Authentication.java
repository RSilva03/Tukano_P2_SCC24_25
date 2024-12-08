package utils;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import tukano.api.Result;
import tukano.api.User;
import tukano.impl.JavaBlobs;
import tukano.impl.JavaUsers;
import tukano.impl.rest.RestResource;
import utils.auth.RequestCookies;
import utils.cache.Cache;

@Path(Authentication.PATH)
public class Authentication {

	private static Logger Log = Logger.getLogger(Authentication.class.getName());

	static final String PATH = "login";
	static final String USER = "username";
	static final String PWD = "password";
	public static final String COOKIE_KEY = "scc:session";
	static final String LOGIN_PAGE = "login.html";
	private static final int MAX_COOKIE_AGE = 3600;
	static final String REDIRECT_TO_AFTER_LOGIN = "/users/";

	@POST
	public Response login( @FormParam(USER) String user, @FormParam(PWD) String password ) {
		Log.info("login -> user: " + user + " + pwd:" + password );
		boolean pwdOk = JavaUsers.getInstance().getUser(user, password).isOK(); // replace with code to check user password
		if (pwdOk) {
			String uid = UUID.randomUUID().toString();
			var cookie = new NewCookie.Builder(COOKIE_KEY)
					.value(uid).path("/")
					.comment("sessionid")
					.maxAge(MAX_COOKIE_AGE)
					.secure(false) //ideally it should be true to only work for https requests
					.httpOnly(true)
					.build();

			Cache.put(uid, JSON.encode(new Session( uid, user)));

			return Response.seeOther(URI.create( REDIRECT_TO_AFTER_LOGIN + user + "?pwd=" + password ))
					.cookie(cookie)
					.build();
		} else{
			Log.info("Incorrect login");
			throw new NotAuthorizedException("Incorrect login");
		}

	}


	@GET
	@Produces(MediaType.TEXT_HTML)
	public String login() {
		try {
			var in = getClass().getClassLoader().getResourceAsStream(LOGIN_PAGE);
			return new String( in.readAllBytes() );
		} catch( Exception x ) {
			throw new WebApplicationException( Status.INTERNAL_SERVER_ERROR );
		}
	}

	static public Session validateSession(String userId) throws NotAuthorizedException {
		var cookies = RequestCookies.get();
		return validateSession( cookies.get(COOKIE_KEY ), userId );
	}

	static public Session validateSession(Cookie cookie, String userId) throws NotAuthorizedException {

		if (cookie == null )
			throw new NotAuthorizedException("No session initialized");

		var session = JSON.decode(Cache.get(cookie.getValue()), Session.class);
		if( session == null )
			throw new NotAuthorizedException("No valid session initialized");

		if (session.user() == null || session.user().length() == 0)
			throw new NotAuthorizedException("No valid session initialized");

		if (!session.user().equals(userId))
			throw new NotAuthorizedException("Invalid user : " + session.user());

		return session;
	}
}

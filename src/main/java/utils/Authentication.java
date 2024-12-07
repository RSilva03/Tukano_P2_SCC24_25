package utils;

import java.net.URI;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import tukano.api.Result;
import tukano.api.User;
import tukano.impl.JavaUsers;
import tukano.impl.rest.RestResource;
import utils.auth.RequestCookies;
import utils.cache.Cache;

@Path(Authentication.PATH)
public class Authentication extends RestResource {
	static final String PATH = "/login";
	public static final String COOKIE_KEY = "scc:session";
	static final String LOGIN_PAGE = "login.html";
	private static final int MAX_COOKIE_AGE = 3600;
	static final String REDIRECT_TO_AFTER_LOGIN = "/users";

	static final String PWD = "pwd";
	static final String QUERY = "query";
	static final String USER_ID = "userId";

	@POST
	public Result<Cookie> login(@PathParam(USER_ID) String user, @QueryParam( PWD ) String password ) {
		System.out.println("user: " + user + " pwd:" + password );
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

			Cache.put( uid, user);

            return Result.ok(cookie.toCookie());
		} else
			throw new NotAuthorizedException("Incorrect login");
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
		
		var user = Cache.get(cookie.getValue());
		if( user == null )
			throw new NotAuthorizedException("No valid session initialized");
			
		if (user == null || user.length() == 0)
			throw new NotAuthorizedException("No valid session initialized");
		
		if (user.equals(userId))
			throw new NotAuthorizedException("Invalid user : " + user);
		
		return new Session(cookie.getValue(), user);
	}
}

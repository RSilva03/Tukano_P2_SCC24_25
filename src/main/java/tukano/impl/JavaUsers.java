package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import utils.db.DBNoSql;
import utils.JSON;
import utils.cache.Cache;
import utils.db.DBHibernate;

public class JavaUsers implements Users {
	
	private static final Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private static Users instance;

	private final DBNoSql dbNoSql;

	private boolean isNoSQL;
	
	synchronized public static Users getInstance() {
		if( instance == null )
			instance = new JavaUsers();
		return instance;
	}
	
	private JavaUsers() {
		dbNoSql = DBNoSql.getInstance(User.class.getSimpleName().toLowerCase() + "s");
		isNoSQL = true;
	}
	
	@Override
	public Result<String> createUser(User user) {
		Log.info(() -> format("createUser : %s\n", user));

		if( badUserInfo( user ) )
				return error(BAD_REQUEST);

		var a = Cache.get(user.getUserId());

		if(isNoSQL){
			if(a == null) {
				Result<String> b = errorOrValue(dbNoSql.insertOne(user), user.getUserId());
				if (b.isOK())
					Cache.put(user.userId(), JSON.encode(user));
				return b;
			} else {
				return error(Result.ErrorCode.CONFLICT);
			}
		} else {
			Log.info("Is using hibernate!!!!!");
			if(a == null) {
				Result<String> b = errorOrValue(DBHibernate.insertOne( user), user.getUserId());
				Log.info("Create: " + b.isOK());
				if (b.isOK())
					Cache.put(user.userId(), JSON.encode(user));
				return b;
			} else {
				return error(Result.ErrorCode.CONFLICT);
			}
		}

	}

	@Override
	public Result<User> getUser(String userId, String pwd) {
		Log.info( () -> format("getUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null)
			return error(BAD_REQUEST);

		var a = Cache.get(userId);

		if(isNoSQL){
			if(a == null)
				return validatedUserOrError( dbNoSql.getOne( userId, User.class), pwd);
			else{
				return validatedUserOrError(Result.ok(JSON.decode(a, User.class)), pwd);
			}
		} else {
			if(a == null)
				return validatedUserOrError( DBHibernate.getOne( userId, User.class), pwd);
			else{
				return validatedUserOrError(Result.ok(JSON.decode(a, User.class)), pwd);
			}
		}
	}

	@Override
	public Result<User> updateUser(String userId, String pwd, User other) {
		Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

		if (badUpdateUserInfo(userId, pwd, other))
			return error(BAD_REQUEST);



		if(isNoSQL){
			var aux = errorOrResult( validatedUserOrError(dbNoSql.getOne( userId, User.class), pwd), user -> dbNoSql.updateOne( user.updateFrom(other)));
            if(aux.isOK())
				Cache.replace(userId, JSON.encode(aux.value()));
			return aux;
		} else {
			var aux = errorOrResult( validatedUserOrError(DBHibernate.getOne( userId, User.class), pwd), user -> DBHibernate.updateOne( user.updateFrom(other)));
			if(aux.isOK())
				Cache.replace(userId, JSON.encode(aux.value()));
			return aux;
		}
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {
		Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null || pwd == null )
			return error(BAD_REQUEST);

		if(isNoSQL){
			return errorOrResult( validatedUserOrError(dbNoSql.getOne( userId, User.class), pwd), user -> {

				Executors.defaultThreadFactory().newThread( () -> {
					JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
					JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
				}).start();
				Cache.delete(userId);
				return dbNoSql.deleteOne( user );
			});
		} else{
			return errorOrResult( validatedUserOrError(DBHibernate.getOne( userId, User.class), pwd), user -> {


				Executors.defaultThreadFactory().newThread( () -> {
					JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
					JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
				}).start();
				Cache.delete(userId);
				return DBHibernate.deleteOne( user);
			});
		}
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info(() -> format("searchUsers : patterns = %s\n", pattern));

		if (pattern == null) {
			return error(BAD_REQUEST);
		}

		if(isNoSQL){
			var query = format("SELECT * FROM c WHERE CONTAINS(UPPER(c.userId), '%s')", pattern.toUpperCase());
			var hits = dbNoSql.query(query, User.class).value()
					.stream()
					.map(User::copyWithoutPassword)
					.toList();

			return ok(hits);
		} else {
			var query = format("SELECT * FROM Users u WHERE UPPER(u.userId) LIKE '%%%s%%'", pattern.toUpperCase());
			var hits = DBHibernate.sql(query, User.class)
					.stream()
					.map(User::copyWithoutPassword)
					.toList();

			return ok(hits);
		}

	}

	
	private Result<User> validatedUserOrError( Result<User> res, String pwd ) {
		if (res.isOK()) {
			return res.value().getPwd().equals(pwd) ? res : error(FORBIDDEN);
		} else {
			return res;
		}
	}
	
	private boolean badUserInfo( User user) {
		return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}
	
	private boolean badUpdateUserInfo( String userId, String pwd, User info) {
		return (userId == null || pwd == null || info.getUserId() != null && ! userId.equals( info.getUserId()));
	}
}

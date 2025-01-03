package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.errorOrVoid;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.User;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.TukanoRestServer;
import utils.db.DBHibernate;
import utils.db.DBNoSql;
import utils.JSON;
import utils.cache.Cache;

public class JavaShorts implements Shorts {

	private static final Logger Log = Logger.getLogger(JavaShorts.class.getName());
	
	private static Shorts instance;

	//private final DBNoSql dbNoSqlShorts;
	//private final DBNoSql dbNoSqlLikes;
	//private final DBNoSql dbNoSqlFollowers;

	private boolean isNoSQL;

	synchronized public static Shorts getInstance() {
		if( instance == null )
			instance = new JavaShorts();
		return instance;
	}
	
	private JavaShorts() {
		//dbNoSqlShorts = DBNoSql.getInstance(Short.class.getSimpleName().toLowerCase() + "s");
		//dbNoSqlLikes = DBNoSql.getInstance(Likes.class.getSimpleName().toLowerCase());
		//dbNoSqlFollowers = DBNoSql.getInstance("followers");
		isNoSQL = false;
	}

	@Override
	public Result<Short> createShort(String userId, String password) {
		Log.info(() -> format("createShort : userId = %s, pwd = %s\n", userId, password));

		return errorOrResult( okUser(userId, password), user -> {

			var shortId = format("%s+%s", userId, UUID.randomUUID());
			var blobUrl = format("%s/%s/%s", TukanoRestServer.serverURI, Blobs.NAME, shortId);
			var shrt = new Short(shortId, userId, blobUrl);

			var aux = errorOrValue(DBHibernate.insertOne(shrt), s -> s.copyWithLikes_And_Token(0));
			if(aux.isOK())
				Cache.put(shortId, JSON.encode(shrt));
			return aux;

		});
	}

	@Override
	public Result<Short> getShort(String shortId) {
		Log.info(() -> format("getShort : shortId = %s\n", shortId));

		if( shortId == null )
			return error(BAD_REQUEST);

		List<Long> likes;
		var query = format("SELECT count(*) FROM Likes l WHERE l.shortId = '%s'", shortId);
		likes = DBHibernate.sql(query, Long.class);


		var aux = Cache.get(shortId);

		if(aux != null)
			return errorOrValue( Result.ok(JSON.decode(aux, Short.class)), shrt -> shrt.copyWithLikes_And_Token( likes.get(0)));

		return errorOrValue( DBHibernate.getOne(shortId, Short.class), shrt -> shrt.copyWithLikes_And_Token( likes.get(0)));
	}

	
	@Override
	public Result<Void> deleteShort(String shortId, String password) {
		Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));
		
		return errorOrResult( getShort(shortId), shrt -> {
			
			return errorOrResult( okUser( shrt.getOwnerId(), password), user -> {

				DBHibernate.deleteOne(shrt);
				var query = format("SELECT * FROM Likes WHERE shortId = '%s'", shortId);
				DBHibernate.sql(query, Likes.class).forEach(l -> DBHibernate.deleteOne(l));

				Cache.delete(shortId);

				return JavaBlobs.getInstance().delete(shrt.getShortId(), Token.get(shortId));
			});	
		});
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Log.info(() -> format("getShorts : userId = %s\n", userId));

		var query = format("SELECT * FROM Shorts WHERE ownerId = '%s'", userId);
		return errorOrValue(okUser(userId), DBHibernate.sql(query, Short.class).stream().map(Short::getShortId).toList());

	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
		Log.info(() -> format("follow : userId1 = %s, userId2 = %s, isFollowing = %s, pwd = %s\n", userId1, userId2, isFollowing, password));

		return errorOrResult( okUser(userId1, password), user -> {
			var f = new Following(userId1, userId2);
			return errorOrVoid( okUser( userId2), isFollowing ? DBHibernate.insertOne( f ) : DBHibernate.deleteOne( f ));
		});
	}

	@Override
	public Result<List<String>> followers(String userId, String password) {
		Log.info(() -> format("followers : userId = %s, pwd = %s\n", userId, password));
		//SQL

		var query = format("Select * from Following where followee = '%s'", userId);

		return errorOrValue(okUser(userId, password), DBHibernate.sql(query, Following.class).stream().map(Following::getFollower).toList());

	}

	@Override
	public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
		Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n", shortId, userId, isLiked, password));

		return errorOrResult( getShort(shortId), shrt -> {
			var l = new Likes(userId, shortId, shrt.getOwnerId());
			return errorOrVoid( okUser( userId, password), isLiked ? DBHibernate.insertOne( l ) : DBHibernate.deleteOne( l ));
		});
	}

	@Override
	public Result<List<String>> likes(String shortId, String password) {
		Log.info(() -> format("likes : shortId = %s, pwd = %s\n", shortId, password));

		return errorOrResult( getShort(shortId), shrt -> {

			var query = format("SELECT * FROM Likes WHERE shortId = '%s'", shortId);
			return errorOrValue(okUser(shrt.getOwnerId(), password), DBHibernate.sql(query, Likes.class).stream().map(Likes::getUserId).toList());
		});
	}


	@Override
	public Result<List<String>> getFeed(String userId, String password) {
		Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));


		var followeeIdsQuery = format("SELECT * FROM Following WHERE follower = '%s'", userId);
		var followeeIds = new ArrayList<>(DBHibernate.sql(followeeIdsQuery, Following.class));

		ArrayList<Short> auxS = new ArrayList<>();
		for (Following aux : followeeIds) {
			var shortsQuery = format("SELECT * FROM Shorts WHERE ownerId = '%s'", aux.getFollowee());
			var shorts = DBHibernate.sql(shortsQuery, Short.class);
			auxS.addAll(shorts);
		}

		var shortsQuery = format("SELECT * FROM Shorts WHERE ownerId = '%s'", userId);
		var shorts = DBHibernate.sql(shortsQuery, Short.class);
		auxS.addAll(shorts);

		var combinedShorts = shorts.stream()
				.sorted(Comparator.comparing(Short::getTimestamp).reversed())
				.collect(Collectors.toList());

		return errorOrValue( okUser( userId, password), combinedShorts.stream().map(Short::getShortId).toList());
	}

	protected Result<User> okUser( String userId, String pwd) {
		return JavaUsers.getInstance().getUser(userId, pwd);
	}
	
	private Result<Void> okUser( String userId ) {
		var res = okUser( userId, "");
		if( res.error() == FORBIDDEN )
			return ok();
		else
			return error( res.error() );
	}
	
	@Override
	public Result<Void> deleteAllShorts(String userId, String password, String token) {
		Log.info(() -> format("deleteAllShorts : userId = %s, password = %s, token = %s\n", userId, password, token));

		if( !okUser(userId, password).isOK())
			return error(FORBIDDEN);

		return DBHibernate.transaction( (hibernate) -> {

			var query1 = format("DELETE FROM Short WHERE ownerId = '%s'", userId);
			hibernate.createQuery(query1).executeUpdate();

			var query2 = format("DELETE FROM Following WHERE follower = '%s' OR followee = '%s'", userId, userId);
			hibernate.createQuery(query2).executeUpdate();

			var query3 = format("DELETE FROM Likes WHERE ownerId = '%s'", userId, userId);
			hibernate.createQuery(query3).executeUpdate();
		});
	}

	@Override
	public Result<Void> addView(String shortId){
		Short aux = getShort(shortId).value();

		aux.addView();

		return Result.ok();
	}
}
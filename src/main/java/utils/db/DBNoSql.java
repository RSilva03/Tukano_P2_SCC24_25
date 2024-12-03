package utils.db;

import java.util.List;
import java.util.function.Supplier;

import static tukano.api.Result.*;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import tukano.api.Result;
import utils.Hibernate;
import utils.Props;

public class DBNoSql {

	/*private static String CONNECTION_URL = "https://scc242570567.documents.azure.com:443/"; // replace with your own
	private static String DB_KEY = "aUuUHB3qSdC92YG3iJ9nEgfV6nVnK4cfefsapqEvXQq0T3QnJPz8OA3dmgKkxesviNOs5nY9Fc46ACDbHKQ29w==";
	private static String DB_NAME = "tukano";

	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer container;

	private DBNoSql(CosmosClient client, String containerName) {
		this.client = client;
		//CONNECTION_URL = Props.get("COSMOSDB_URL", String.class);
		//DB_KEY = Props.get("COSMOSDB_KEY", String.class);
		//DB_NAME = Props.get("COSMOSDB_DATABASE", String.class);
		this.container = client.getDatabase(DB_NAME).getContainer(containerName);
	}

	public static synchronized DBNoSql getInstance(String containerName) {
		return new DBNoSql(createClient(), containerName);
	}

	private static CosmosClient createClient() {
		return new CosmosClientBuilder()
				.endpoint(CONNECTION_URL)
				.key(DB_KEY)
				.directMode()
				.consistencyLevel(ConsistencyLevel.SESSION)
				.connectionSharingAcrossClientsEnabled(true)
				.contentResponseOnWriteEnabled(true)
				.buildClient();
	}

	public void close() {
		client.close();
	}

	public static <T> List<T> sql(Class<T> clazz, String fmt, Object... args) {
		return Hibernate.getInstance().sql(String.format(fmt, args), clazz);
	}

	public <T> Result<T> getOne(String id, Class<T> clazz) {
		return tryCatch(() -> container.readItem(id, new PartitionKey(id), clazz).getItem());
	}

	public <T> Result<T> deleteOne(T obj) {
		return (Result<T>) tryCatch(() -> container.deleteItem(obj, new CosmosItemRequestOptions()).getItem());
	}

	public <T> Result<T> updateOne(T obj) {
		return tryCatch(() -> container.upsertItem(obj).getItem());
	}

	public <T> Result<T> insertOne(T obj) {
		return tryCatch(() -> container.createItem(obj).getItem());
	}

	public <T> Result<List<T>> query(String queryStr, Class<T> clazz) {
		return tryCatch(() -> {
			var res = container.queryItems(queryStr, new CosmosQueryRequestOptions(), clazz);
			return res.stream().toList();
		});
	}

	<T> Result<T> tryCatch(Supplier<T> supplierFunc) {
		try {
			return Result.ok(supplierFunc.get());
		} catch (CosmosException ce) {
			ce.printStackTrace();
			return Result.error(errorCodeFromStatus(ce.getStatusCode()));
		} catch (Exception x) {
			x.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	static Result.ErrorCode errorCodeFromStatus(int status) {
		return switch (status) {
			case 200 -> ErrorCode.OK;
			case 404 -> ErrorCode.NOT_FOUND;
			case 409 -> ErrorCode.CONFLICT;
			default -> ErrorCode.INTERNAL_ERROR;
		};
	}*/
}
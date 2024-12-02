package tukano.impl.storage;


import static tukano.api.Result.error;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.CONFLICT;
import static tukano.api.Result.ErrorCode.INTERNAL_ERROR;
import static tukano.api.Result.ErrorCode.NOT_FOUND;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;
import utils.Hash;
import utils.IO;

import com.azure.storage.blob.BlobClient;
import utils.Props;

public class FilesystemStorage implements BlobStorage {
	private final String rootDir;
	private static final int CHUNK_SIZE = 4096;
	private static final String DEFAULT_ROOT_DIR = "/tmp/";

	private static final String BLOBS_CONTAINER_NAME = "images";

	String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=scc242570567;AccountKey=nR6zM1Z5EqU4HWBtnY31TBvzXhqzaGXgZAOLbB7Xvrvqsj5ZadwNFW3GUViCv7hzsjX3BfjnI95w+AStwiHEhA==;EndpointSuffix=core.windows.net";

	public FilesystemStorage() {
		this.rootDir = DEFAULT_ROOT_DIR;
		//storageConnectionString = Props.get("BlobStoreConnection", String.class);
	}

	@Override
	public Result<Void> write(String path, byte[] bytes) {
		try {
			BinaryData data = BinaryData.fromBytes( bytes );

			// Get container client
			BlobContainerClient containerClient = new BlobContainerClientBuilder()
					.connectionString(storageConnectionString)
					.containerName(BLOBS_CONTAINER_NAME)
					.buildClient();

			// Get client to blob
			BlobClient blob = containerClient.getBlobClient( path );

			// See if blob exists
			if (blob.exists()) {
				System.out.println( "Blob exists: " + path);

				byte[] file = blob.downloadContent().toBytes();
				if (Arrays.equals(Hash.sha256(bytes), Hash.sha256(file)))
					return ok();
				else
					return error(CONFLICT);
			}

			// Upload contents from BinaryData (check documentation for other alternatives)
			blob.upload(data);

			System.out.println( "File uploaded : " + path);

		} catch( Exception e) {
			e.printStackTrace();
		}
		return ok();
	}

	@Override
	public Result<byte[]> read(String path) {
		if (path == null)
			return error(BAD_REQUEST);

		try {
			// Get container client
			BlobContainerClient containerClient = new BlobContainerClientBuilder()
					.connectionString(storageConnectionString)
					.containerName(BLOBS_CONTAINER_NAME)
					.buildClient();

			// Get client to blob
			BlobClient blob = containerClient.getBlobClient( path );

			// Download contents to BinaryData (check documentation for other alternatives)
			BinaryData data = blob.downloadContent();

			byte[] arr = data.toBytes();

			System.out.println( "Blob size : " + arr.length);

			return arr != null ? ok( arr ) : error( INTERNAL_ERROR );

		} catch( Exception e) {
			e.printStackTrace();
			return error(NOT_FOUND);
		}
	}

	@Override
	public Result<Void> read(String path, Consumer<byte[]> sink) {
		if (path == null)
			return error(BAD_REQUEST);

		try {
			// Get container client
			BlobContainerClient containerClient = new BlobContainerClientBuilder()
					.connectionString(storageConnectionString)
					.containerName(BLOBS_CONTAINER_NAME)
					.buildClient();

			// Get client to blob
			BlobClient blob = containerClient.getBlobClient( path );

			// Download contents to BinaryData (check documentation for other alternatives)
			BinaryData data = blob.downloadContent();

			byte[] arr = data.toBytes();

			System.out.println( "Blob size : " + arr.length);

			sink.accept(arr);

			return arr != null ? ok() : error( INTERNAL_ERROR );

		} catch( Exception e) {
			e.printStackTrace();
			return error(NOT_FOUND);
		}

	}

	@Override
	public Result<Void> delete(String path) {
		if (path == null)
			return error(BAD_REQUEST);

		try {
			// Get container client
			BlobContainerClient containerClient = new BlobContainerClientBuilder()
					.connectionString(storageConnectionString)
					.containerName(BLOBS_CONTAINER_NAME)
					.buildClient();

			// Get client to blob
			BlobClient blob = containerClient.getBlobClient( path );

			// Download contents to BinaryData (check documentation for other alternatives)
			boolean data = blob.deleteIfExists();

			System.out.println( "Blob exists? : " + data);

			return ok();
		} catch( Exception e) {
			e.printStackTrace();
			return error(NOT_FOUND);
		}
	}

	private File toFile(String path) {
		var res = new File( rootDir + path );

		var parent = res.getParentFile();
		if( ! parent.exists() )
			parent.mkdirs();

		return res;
	}


}

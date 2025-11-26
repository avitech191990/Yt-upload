package com.main.yt;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

@SpringBootApplication
@EnableScheduling
public class YtUploadApplication {

	private static final Logger logger = LoggerFactory.getLogger(YtUploadApplication.class);


	private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	public static void main(String[] args) throws Exception {

		SpringApplication.run(YtUploadApplication.class, args);
		new YtUploadApplication().runUploader();
		// For local scheduled run, uncomment below:
		//SpringApplication.run(YtUploadApplication.class, args);
	}

	// Extracted uploader logic
	public void runUploader() throws Exception {

		logger.info("=== Starting Drive -> YouTube Upload Task ===");

		String clientId = System.getenv("CLIENT_ID");
		String clientSecret = System.getenv("CLIENT_SECRET");
		String refreshToken = System.getenv("REFRESH_TOKEN");
		String contentFolderId = System.getenv("CONTENT_FOLDER_ID");
		String postedFolderId = System.getenv("POSTED_FOLDER_ID");
		String applicationName = System.getenv("APPLICATION_NAME");
		String title = System.getenv("VIDEO_TITLE");
		String description = System.getenv("VIDEO_DESC");
		String tagsCsv = System.getenv("VIDEO_TAGS");

		if (clientId == null || clientSecret == null || refreshToken == null
				|| contentFolderId == null || postedFolderId == null || applicationName == null) {
			logger.error("Missing required environment variables. Exiting...");
			return;
		}

		// Build Google credential
		GoogleCredential credential = new GoogleCredential.Builder()
				.setTransport(HTTP_TRANSPORT)
				.setJsonFactory(JSON_FACTORY)
				.setClientSecrets(clientId, clientSecret)
				.build()
				.setRefreshToken(refreshToken);
		credential.refreshToken();
		logger.info("Google credentials initialized.");

		// Build Drive client
		Drive drive = new Drive.Builder(HTTP_TRANSPORT,
				JSON_FACTORY,
				credential)
				.setApplicationName(applicationName)
				.build();

		// List video files in content folder
		String query = String.format("'%s' in parents and mimeType contains 'video/' and trashed = false",
				contentFolderId);
		FileList fileList = drive.files().list()
				.setQ(query)
				.setFields("files(id,name)")
				.execute();

		List<File> files = fileList.getFiles();
		if (files == null || files.isEmpty()) {
			logger.info("No files found in content folder. Exiting...");
			return;
		}

		// Pick random file
		File selectedFile = files.get(new Random().nextInt(files.size()));
		logger.info("Selected file: {}", selectedFile.getName());

		// Download to local folder
		String localDir = Paths.get(System.getProperty("user.dir"), "downloads").toString();
		java.io.File localFile = Paths.get(localDir, selectedFile.getName()).toFile();
		localFile.getParentFile().mkdirs();

		try (OutputStream out = new FileOutputStream(localFile)) {
			drive.files().get(selectedFile.getId()).executeMediaAndDownloadTo(out);
		}
		logger.info("Downloaded file to {}", localFile.getAbsolutePath());

		// Build YouTube client
		YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT,
				JSON_FACTORY,
				credential)
				.setApplicationName(applicationName)
				.build();

		VideoSnippet snippet = new VideoSnippet();
		snippet.setTitle(title != null ? title : "Auto short");
		snippet.setDescription(description != null ? description : "");
		if (tagsCsv != null && !tagsCsv.isEmpty()) {
			snippet.setTags(List.of(tagsCsv.split(",")));
		}
		snippet.setCategoryId("22");

		VideoStatus status = new VideoStatus();
		status.setPrivacyStatus("public");

		Video videoMetadata = new Video();
		videoMetadata.setSnippet(snippet);
		videoMetadata.setStatus(status);

		InputStreamContent mediaContent = new InputStreamContent(
				"video/*",
				new java.io.FileInputStream(localFile)
		);
		mediaContent.setLength(localFile.length());

		YouTube.Videos.Insert request = youtube.videos()
				.insert("snippet,status", videoMetadata, mediaContent);

		MediaHttpUploader uploader = request.getMediaHttpUploader();
		uploader.setDirectUploadEnabled(false);
		uploader.setProgressListener((MediaHttpUploaderProgressListener) u ->
				logger.info("Upload state: {} progress: {}", u.getUploadState(), u.getProgress()));

        boolean uploadSuccess = false;

        try {
            // Upload to YouTube
            Video returnedVideo = request.execute();
            logger.info("Upload completed! Video ID: {}", returnedVideo.getId());
            uploadSuccess = true;

        } catch (Exception e) {
            logger.error("Upload failed! File will NOT be moved.", e);
        }

        if (uploadSuccess) {
            try {
                // Move file in Drive only after successful upload
                drive.files().update(selectedFile.getId(), null)
                        .setAddParents(postedFolderId)
                        .setRemoveParents(contentFolderId)
                        .execute();
                logger.info("Moved file to posted folder in Drive.");

            } catch (Exception e) {
                logger.error("Failed to move file in Drive after successful upload.", e);
            }
        }

        // Delete local file
		if (localFile.exists() && localFile.delete()) {
			logger.info("Deleted local file: {}", localFile.getAbsolutePath());
		} else {
			logger.warn("Failed to delete local file: {}", localFile.getAbsolutePath());
		}

		logger.info("=== Task Completed ===");
	}
}

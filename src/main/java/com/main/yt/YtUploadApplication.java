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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SpringBootApplication
@EnableScheduling
public class YtUploadApplication {

	private static final Logger logger = LoggerFactory.getLogger(YtUploadApplication.class);


	private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    // Channel-specific envs
    private static String clientId;
    private static String clientSecret;
    private static String refreshToken;
    private static String contentFolderId;
    private static String postedFolderId;
    private static String applicationName;
    private static String videoTitle;
    private static String videoDesc;
    private static String videoTags;

	public static void main(String[] args) throws Exception {

		SpringApplication.run(YtUploadApplication.class, args);
		new YtUploadApplication().runUploader(args);
		// For local scheduled run, uncomment below:
		//SpringApplication.run(YtUploadApplication.class, args);
	}

	// Extracted uploader logic
	public void runUploader(String[] args) throws Exception {

        // -------------------------------
        // 1. Read channel argument
        // -------------------------------
        String channel = "1"; // default

        for (String arg : args) {
            if (arg.startsWith("--channel=")) {
                channel = arg.substring("--channel=".length());
            }
        }

        logger.info("=================================================");
        logger.info("      Upload Task Starting for CHANNEL: " + channel);
        logger.info("=================================================");

        // Load env for selected channel
        loadEnv(channel);

        // Validate env
        if (!validateEnv()) {
            logger.info("❌ Missing environment variables. Exiting...");
            return;
        }

        try {
            startUploadProcess();
        } catch (Exception e) {
            logger.error("❌ Error during upload process: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================================
    // Load Channel-Specific Environment Variables
    // ==========================================================
    private static void loadEnv(String channel) {

        if (channel.equals("1")) {
            clientId = System.getenv("YT_CLIENT_ID_CH1");
            clientSecret = System.getenv("YT_CLIENT_SECRET_CH1");
            refreshToken = System.getenv("YT_REFRESH_TOKEN_CH1");
            contentFolderId = System.getenv("CONTENT_FOLDER_ID_CH1");
            postedFolderId = System.getenv("POSTED_FOLDER_ID_CH1");
            videoTitle = System.getenv("VIDEO_TITLE_CH1");
            videoDesc = System.getenv("VIDEO_DESC_CH1");
            videoTags = System.getenv("VIDEO_TAGS_CH1");
            applicationName = "YT-Uploader-Factvibes19";

        } else {
            clientId = System.getenv("YT_CLIENT_ID_CH2");
            clientSecret = System.getenv("YT_CLIENT_SECRET_CH2");
            refreshToken = System.getenv("YT_REFRESH_TOKEN_CH2");
            contentFolderId = System.getenv("CONTENT_FOLDER_ID_CH2");
            postedFolderId = System.getenv("POSTED_FOLDER_ID_CH2");
            videoTitle = System.getenv("VIDEO_TITLE_CH2");
            videoDesc = System.getenv("VIDEO_DESC_CH2");
            videoTags = System.getenv("VIDEO_TAGS_CH2");
            applicationName = "YT-Uploader-AMotivations19";
        }

        logger.info("Loaded env for channel: " + channel);
    }

    // ==========================================================
    // Validate env
    // ==========================================================
    private static boolean validateEnv() {
        return clientId != null &&
                clientSecret != null &&
                refreshToken != null &&
                contentFolderId != null &&
                postedFolderId != null;
    }

    // ==========================================================
    // Main upload process
    // ==========================================================
    private static void startUploadProcess() throws Exception {

        // Build Google credential
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        credential.refreshToken();
        logger.info("✔ Google credentials initialized.");

        // Create Drive client
        Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();

        // Search videos in folder
        String query = String.format("'%s' in parents and mimeType contains 'video/' and trashed = false", contentFolderId);

        FileList fileList = drive.files().list()
                .setQ(query)
                .setFields("files(id,name)")
                .execute();

        List<File> files = fileList.getFiles();

        if (files == null || files.isEmpty()) {
            logger.info("⚠ No videos found. Exiting...");
            return;
        }

        // Pick random file
        File selectedFile = files.get(new Random().nextInt(files.size()));
        logger.info("🎬 Selected: " + selectedFile.getName());

        // Download file
        String localDir = Paths.get(System.getProperty("user.dir"), "downloads").toString();
        java.io.File localFile = Paths.get(localDir, selectedFile.getName()).toFile();
        localFile.getParentFile().mkdirs();

        try (OutputStream out = new FileOutputStream(localFile)) {
            drive.files().get(selectedFile.getId()).executeMediaAndDownloadTo(out);
        }

        logger.info("⬇ Downloaded: " + localFile.getAbsolutePath());

        // YouTube client
        YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();

        // Metadata
        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(videoTitle);
        snippet.setDescription(videoDesc);
        snippet.setCategoryId("22");

        if (videoTags != null && !videoTags.isEmpty()) {
            snippet.setTags(Arrays.asList(videoTags.split(",")));
        }

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("public");

        Video video = new Video();
        video.setSnippet(snippet);
        video.setStatus(status);

        InputStreamContent mediaContent =
                new InputStreamContent("video/*", new FileInputStream(localFile));

        mediaContent.setLength(localFile.length());

        YouTube.Videos.Insert request =
                youtube.videos().insert("snippet,status", video, mediaContent);

        MediaHttpUploader uploader = request.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(false);

        uploader.setProgressListener((MediaHttpUploaderProgressListener) u -> {
            logger.info("Upload: " + u.getUploadState() + " (" + u.getProgress() + ")");
        });

        boolean uploadSuccess = false;

        try {
            Video uploaded = request.execute();
            logger.info("✔ Upload complete! Video ID: " + uploaded.getId());
            uploadSuccess = true;

        } catch (Exception e) {
            logger.error("❌ Upload failed!");
            e.printStackTrace();
        }

        // Move file only after success
        if (uploadSuccess) {
            drive.files().update(selectedFile.getId(), null)
                    .setAddParents(postedFolderId)
                    .setRemoveParents(contentFolderId)
                    .execute();

            logger.info("📁 File moved to posted folder.");
        }

        // Delete local file
        localFile.delete();
        logger.info("🗑 Deleted local temp file.");

        logger.info("✅ Task Completed.");
    }
}

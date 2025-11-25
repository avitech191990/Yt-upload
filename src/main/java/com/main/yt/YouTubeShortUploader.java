/*
package com.main.yt;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.api.services.drive.Drive;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;

public class YouTubeShortUploader {
    private static final String APPLICATION_NAME = "AviTech";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public static void main(String[] args) throws Exception {
        // Load from env (GitHub Secrets mapped to env vars in GH Actions)
        String clientId = "487847793152-nodvl3scbq9phfdjjeqfse34efhopo1q.apps.googleusercontent.com";
        String clientSecret = "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";
        String refreshToken = "1//0gWGj6IVRq9R-CgYIARAAGBASNwF-L9Irg_iCEiRpYYlmJBnyPgVguNOy4v4ps0PDXbr503sTooKlIzZ4mWRQuALTsh8UE_JUH4Q";
        String driveFileId = "1aWBiH0-PLF1d0hjUHPWj1km5iUg64-qj"; // single file id; you can loop or rotate
        String title = "Testing title";
        String description = "Testing description";
        String tagsCsv = "tag1,tag2,tag3";

        if (clientId == null || clientSecret == null || refreshToken == null || driveFileId == null) {
            System.err.println("Missing required env vars.");
            System.exit(1);
        }

        // 1) Build credential from refresh token
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);
        credential.refreshToken(); // obtains access token using refresh token

        // 2) Build Drive client and download file
        Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();

        // Ensure downloads folder exists
        String downloadsDir = Paths.get(System.getProperty("user.dir"), "downloads").toString();
        java.io.File folder = new java.io.File(downloadsDir);
        if (!folder.exists()) {
            folder.mkdirs(); // creates the folder (and any parent folders if needed)
        }

        // Save the file with a fixed name
        String localFilePath = Paths.get(downloadsDir, "video.mp4").toString();
        try (OutputStream out = new FileOutputStream(localFilePath)) {
            drive.files().get(driveFileId).executeMediaAndDownloadTo(out);
        }
        System.out.println("Downloaded file to: " + localFilePath);

        // 3) Build YouTube client
        // 3) Build YouTube client
        YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();

// 4) Create Video metadata
        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(title != null ? title : "Auto short");
        snippet.setDescription(description != null ? description : "");
        if (tagsCsv != null && !tagsCsv.isEmpty()) {
            snippet.setTags(List.of(tagsCsv.split(",")));
        }
        snippet.setCategoryId("22"); // People & Blogs

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("public"); // or "unlisted"

        Video videoObjectDefiningMetadata = new Video();
        videoObjectDefiningMetadata.setSnippet(snippet);
        videoObjectDefiningMetadata.setStatus(status);

        // 5) Upload the file (resumable) using InputStreamContent without content length
        java.io.File mediaFile = new java.io.File(localFilePath);
        InputStreamContent mediaContent = new InputStreamContent(
                "video/*",
                new java.io.FileInputStream(mediaFile)
        );
// Do NOT set length to keep it unknown

        YouTube.Videos.Insert request = youtube.videos()
                .insert("snippet,status", videoObjectDefiningMetadata, mediaContent);

        MediaHttpUploader uploader = request.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(false); // use resumable upload
        uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
            public void progressChanged(MediaHttpUploader uploader) throws IOException {
                switch (uploader.getUploadState()) {
                    case INITIATION_STARTED:
                        System.out.println("Upload Initiation Started");
                        break;
                    case INITIATION_COMPLETE:
                        System.out.println("Upload Initiation Completed");
                        break;
                    case MEDIA_IN_PROGRESS:
                        System.out.println("Uploading... Bytes uploaded: " + uploader.getNumBytesUploaded());
                        break;
                    case MEDIA_COMPLETE:
                        System.out.println("Upload Completed!");
                        break;
                    case NOT_STARTED:
                        System.out.println("Upload Not Started");
                        break;
                }
            }
        });

// 6) Execute upload
        Video returnedVideo = request.execute();
        System.out.println("Uploaded video ID: " + returnedVideo.getId());
    }
}


*/

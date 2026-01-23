package com.main.yt.youtube;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.List;

@Component
public class YouTubeClient {

    private final YouTube youtube;

    public YouTubeClient(YouTube youtube) {
        this.youtube = youtube;
    }

    public String upload(
            java.io.File videoFile,
            String title,
            String description,
            List<String> tags
    ) throws Exception {

        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(title);
        snippet.setDescription(description);
        snippet.setTags(tags);
        snippet.setCategoryId("22");

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("public");

        Video video = new Video();
        video.setSnippet(snippet);
        video.setStatus(status);

        InputStreamContent media = new InputStreamContent(
                "video/*",
                new FileInputStream(videoFile)
        );
        media.setLength(videoFile.length());

        YouTube.Videos.Insert request =
                youtube.videos().insert("snippet,status", video, media);

        MediaHttpUploader uploader = request.getMediaHttpUploader();
        uploader.setDirectUploadEnabled(false);

        Video response = request.execute();
        return response.getId();
    }
}


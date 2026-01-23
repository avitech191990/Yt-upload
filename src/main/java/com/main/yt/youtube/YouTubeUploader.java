package com.main.yt.youtube;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.List;

@Component
public class YouTubeUploader {

    private final YouTube youtube;

    public YouTubeUploader(YouTube youtube) {
        this.youtube = youtube;
    }

    public void upload(java.io.File videoFile) throws Exception {

        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(System.getenv("VIDEO_TITLE"));
        snippet.setDescription(System.getenv("VIDEO_DESC"));
        snippet.setTags(List.of("shorts", "automation", "quotes"));
        snippet.setCategoryId("22");

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("public");

        Video video = new Video();
        video.setSnippet(snippet);
        video.setStatus(status);

        InputStreamContent media =
                new InputStreamContent("video/*", new FileInputStream(videoFile));
        media.setLength(videoFile.length());

        youtube.videos()
                .insert("snippet,status", video, media)
                .execute();
    }
}


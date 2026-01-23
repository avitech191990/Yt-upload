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


        String title = System.getenv("VIDEO_TITLE");
        String description = System.getenv("VIDEO_DESC");
        String tagsCsv = System.getenv("VIDEO_TAGS");

        System.out.println("🎬 TITLE = [" + title + "]");
        System.out.println("📝 DESC  = [" + description + "]");
        System.out.println("🏷 TAGS  = [" + tagsCsv + "]");

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

        InputStreamContent media =
                new InputStreamContent("video/*", new FileInputStream(videoFile));
        media.setLength(videoFile.length());

        youtube.videos()
                .insert("snippet,status", videoMetadata, media)
                .execute();
    }
}


package com.main.yt.drive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

@Component
public class DriveClient {

    private final Drive drive;

    public DriveClient(Drive drive) {
        this.drive = drive;
    }

    public Drive getDrive() {
        return this.drive;
    }


    public List<File> listVideos(String folderId) throws Exception {
        String q = "'" + folderId + "' in parents and mimeType contains 'video/' and trashed=false";

        FileList list = drive.files().list()
                .setQ(q)
                .setOrderBy("createdTime asc") // OLD → NEW
                .setFields("files(id,name)")
                .execute();

        return list.getFiles();
    }

    public List<File> listFiles(String folderId) throws Exception {
        FileList list = drive.files().list()
                .setQ("'" + folderId + "' in parents and trashed=false")
                .setFields("files(id,name)")
                .execute();
        return list.getFiles();
    }

    public java.io.File download(File driveFile, Path targetDir) throws Exception {
        java.io.File local = targetDir.resolve(driveFile.getName()).toFile();
        local.getParentFile().mkdirs();

        try (OutputStream out = new FileOutputStream(local)) {
            drive.files().get(driveFile.getId())
                    .executeMediaAndDownloadTo(out);
        }
        return local;
    }

    public void move(String fileId, String fromFolder, String toFolder) throws Exception {
        drive.files().update(fileId, null)
                .setAddParents(toFolder)
                .setRemoveParents(fromFolder)
                .execute();
    }

    public File uploadVideo(
            java.io.File localFile,
            String targetFolderId
    ) throws Exception {

        System.out.println("Uploading video to Drive: " + localFile.getName());
        File metadata = new File();
        metadata.setName(localFile.getName());
        metadata.setParents(List.of(targetFolderId));

        FileContent media = new FileContent("video/mp4", localFile);

        return drive.files()
                .create(metadata, media)
                .setFields("id,name")
                .execute();
    }

    public File pickRandomVideo(String folderId) throws Exception {

        String q = "'" + folderId + "' in parents and mimeType contains 'video/' and trashed=false";

        FileList list = drive.files().list()
                .setQ(q)
                .setFields("files(id,name)")
                .execute();

        List<File> files = list.getFiles();

        if (files == null || files.isEmpty()) {
            throw new IllegalStateException("❌ No videos found in Drive folder: " + folderId);
        }

        File selected = files.get(new Random().nextInt(files.size()));

        System.out.println("🎲 Random video selected: " + selected.getName());

        return selected;
    }

}



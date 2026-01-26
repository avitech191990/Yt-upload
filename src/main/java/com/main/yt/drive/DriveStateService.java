package com.main.yt.drive;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DriveStateService {

    private static final String INDEX_FILE_NAME = "quote_index.txt";

    private final Drive drive;
    private final String stateFolderId;

    public DriveStateService(Drive drive, String stateFolderId) {
        if (stateFolderId == null || stateFolderId.isBlank()) {
            throw new IllegalArgumentException("STATE_FOLDER_ID is not set");
        }
        this.drive = drive;
        this.stateFolderId = stateFolderId;
    }

    // ============================
    // 🔹 READ QUOTE INDEX
    // ============================
    public int readQuoteIndex() {
        try {
            FileList result = drive.files().list()
                    .setQ("'" + stateFolderId + "' in parents and name='" + INDEX_FILE_NAME + "'")
                    .setFields("files(id)")
                    .execute();

            // First run → file does not exist
            if (result.getFiles().isEmpty()) {
                return 0;
            }

            String fileId = result.getFiles().get(0).getId();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            drive.files().get(fileId).executeMediaAndDownloadTo(output);

            return Integer.parseInt(output.toString().trim());

        } catch (Exception e) {
            System.out.println("⚠️ Failed to read quote index, defaulting to 0");
            return 0;
        }
    }

    // ============================
    // 🔹 SAVE QUOTE INDEX
    // ============================
    public void saveQuoteIndex(int index) {
        try {
            FileList result = drive.files().list()
                    .setQ("'" + stateFolderId + "' in parents and name='" + INDEX_FILE_NAME + "'")
                    .setFields("files(id)")
                    .execute();

            ByteArrayContent content = new ByteArrayContent(
                    "text/plain",
                    String.valueOf(index).getBytes(StandardCharsets.UTF_8)
            );

            if (result.getFiles().isEmpty()) {
                // Create new file
                File metadata = new File();
                metadata.setName(INDEX_FILE_NAME);
                metadata.setParents(List.of(stateFolderId));

                drive.files()
                        .create(metadata, content)
                        .setFields("id")
                        .execute();
            } else {
                // Update existing file
                String fileId = result.getFiles().get(0).getId();

                drive.files()
                        .update(fileId, null, content)
                        .execute();
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to save quote index to Drive", e);
        }
    }
}


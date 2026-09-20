package com.webar.app.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDirectory =
            Paths.get("uploads");

    public FileStorageService() {

        try {

            Files.createDirectories(uploadDirectory);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not create upload directory",
                    e
            );
        }
    }

    public String saveFile(
            MultipartFile file,
            String folder,
            String[] allowedExtensions
    ) {

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "File cannot be empty"
            );
        }

        String originalName =
                file.getOriginalFilename();

        if (originalName == null) {

            throw new RuntimeException(
                    "Invalid file name"
            );
        }

        String extension =
                getExtension(originalName);

        boolean allowed = false;

        for (String x : allowedExtensions) {

            if (extension.equalsIgnoreCase(x)) {

                allowed = true;
                break;
            }
        }

        if (!allowed) {

            throw new RuntimeException(
                    "File type not supported: "
                            + extension
            );
        }

        try {

            Path folderPath =
                    uploadDirectory.resolve(folder);

            Files.createDirectories(folderPath);

            String fileName =
                    UUID.randomUUID()
                            + "."
                            + extension;

            Path target =
                    folderPath.resolve(fileName);

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "/uploads/"
                    + folder
                    + "/"
                    + fileName;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not save file",
                    e
            );
        }
    }

    public void deleteFile(String url) {
        if (url == null || url.isBlank()) return;
        String prefix = "/uploads/";
        if (!url.startsWith(prefix)) return;
        String relative = url.substring(prefix.length());
        Path target = uploadDirectory.resolve(relative).normalize();
        if (!target.startsWith(uploadDirectory.normalize())) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file", e);
        }
    }

    private String getExtension(
            String fileName
    ) {

        int index =
                fileName.lastIndexOf(".");

        if (index == -1) {

            return "";
        }

        return fileName
                .substring(index + 1)
                .toLowerCase();
    }
}
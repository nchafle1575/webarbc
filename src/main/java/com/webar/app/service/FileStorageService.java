package com.webar.app.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path uploadDirectory = Paths.get("uploads");

    public FileStorageService() {
        try { Files.createDirectories(uploadDirectory); }
        catch (IOException e) { throw new RuntimeException("Could not create upload directory", e); }
    }

    public String saveFile(MultipartFile file,String folder,String[] allowedExtensions) {
        if(file==null||file.isEmpty()) throw new RuntimeException("File cannot be empty");
        String originalName=file.getOriginalFilename();
        if(originalName==null) throw new RuntimeException("Invalid file name");
        String extension=getExtension(originalName);
        boolean allowed=false;
        for(String x:allowedExtensions) if(extension.equalsIgnoreCase(x)){allowed=true;break;}
        if(!allowed) throw new RuntimeException("File type not supported: "+extension);
        try{
            Path folderPath=uploadDirectory.resolve(folder);
            Files.createDirectories(folderPath);
            String fileName=UUID.randomUUID()+"."+extension;
            Files.copy(file.getInputStream(),folderPath.resolve(fileName),StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/"+folder+"/"+fileName;
        }catch(IOException e){throw new RuntimeException("Could not save file",e);}
    }

    public String saveBytes(byte[] bytes,String folder,String fileName) {
        if(bytes==null||bytes.length==0) throw new RuntimeException("File content cannot be empty");
        try{
            Path folderPath=uploadDirectory.resolve(folder);
            Files.createDirectories(folderPath);
            Path target=folderPath.resolve(fileName).normalize();
            if(!target.startsWith(uploadDirectory.resolve(folder).normalize())) throw new RuntimeException("Invalid file path");
            Files.write(target,bytes,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
            return "/uploads/"+folder+"/"+fileName;
        }catch(IOException e){throw new RuntimeException("Could not save file",e);}
    }

    public byte[] readFile(String url) {
        Path path=resolveLocalPath(url);
        if(path==null||!Files.exists(path)) throw new RuntimeException("File not found: "+url);
        try{return Files.readAllBytes(path);}catch(IOException e){throw new RuntimeException("Could not read file",e);}
    }

    public Path resolveLocalPath(String url) {
        if(url==null||url.isBlank()||!url.startsWith("/uploads/")) return null;
        String relative=url.substring("/uploads/".length());
        Path target=uploadDirectory.resolve(relative).normalize();
        if(!target.startsWith(uploadDirectory.normalize())) return null;
        return target;
    }

    public String toDataUri(String url,String fileName) {
        byte[] bytes=readFile(url);
        String mime=mimeType(fileName);
        return "data:"+mime+";base64,"+Base64.getEncoder().encodeToString(bytes);
    }

    public void deleteFile(String url) {
        if(url==null||url.isBlank()) return;
        Path target=resolveLocalPath(url);
        if(target==null) return;
        try{Files.deleteIfExists(target);}catch(IOException e){throw new RuntimeException("Could not delete file",e);}
    }

    private String mimeType(String fileName) {
        String ext=getExtension(fileName);
        return switch(ext){
            case "jpg","jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String getExtension(String fileName) {
        int index=fileName.lastIndexOf(".");
        return index==-1?"":fileName.substring(index+1).toLowerCase();
    }
}

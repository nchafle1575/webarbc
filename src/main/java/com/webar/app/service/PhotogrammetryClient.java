package com.webar.app.service;

import com.webar.app.config.PhotogrammetryConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class PhotogrammetryClient {
    private final RestClient client;

    public PhotogrammetryClient(RestClient.Builder builder, PhotogrammetryConfig config) {
        this.client=builder.baseUrl(config.workerUrl()).build();
    }

    public WorkerJobResponse createJob(List<MultipartFile> files) {
        LinkedMultiValueMap<String,Object> body=new LinkedMultiValueMap<>();
        for(MultipartFile file:files) body.add("files",new FileResource(file));
        return client.post().uri("/jobs").contentType(MediaType.MULTIPART_FORM_DATA).body(body)
                .retrieve().body(WorkerJobResponse.class);
    }

    public WorkerJobResponse getJob(String id) {
        return client.get().uri("/jobs/{id}",id).retrieve().body(WorkerJobResponse.class);
    }

    public record WorkerJobResponse(String id,String status,Integer progress,Integer source_image_count,
                                     String error,String glb,String usdz) {}

    private static final class FileResource extends ByteArrayResource {
        private final String filename;
        FileResource(MultipartFile file) {
            super(bytes(file));
            filename=file.getOriginalFilename()==null?"media":file.getOriginalFilename();
        }
        @Override public String getFilename(){return filename;}
        private static byte[] bytes(MultipartFile file){
            try{return file.getBytes();}catch(Exception e){throw new IllegalStateException("Could not read uploaded media",e);}
        }
    }
}

package com.webar.app.service;

import com.webar.app.config.PhotogrammetryConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.util.List;

@Service
public class PhotogrammetryClient {
    private final RestClient client;

    public PhotogrammetryClient(RestClient.Builder builder, PhotogrammetryConfig config) {
        this.client=builder.baseUrl(config.workerUrl()).build();
    }

    public WorkerJobResponse createJob(List<SourceFile> files) {
        LinkedMultiValueMap<String,Object> body=new LinkedMultiValueMap<>();
        for(SourceFile file:files) {
            body.add("files",new NamedBytesResource(file.bytes(),file.fileName()));
        }
        return client.post().uri("/jobs").contentType(MediaType.MULTIPART_FORM_DATA).body(body)
                .retrieve().body(WorkerJobResponse.class);
    }

    public WorkerJobResponse getJob(String id) {
        return client.get().uri("/jobs/{id}",id).retrieve().body(WorkerJobResponse.class);
    }

    public byte[] downloadArtifact(String id,String artifact) {
        return client.get().uri("/jobs/{id}/artifacts/{artifact}",id,artifact)
                .retrieve().body(byte[].class);
    }

    public record SourceFile(String fileName, byte[] bytes) {}
    public record WorkerJobResponse(String id,String status,Integer progress,Integer source_image_count,
                                     String error,String glb,String usdz) {}

    private static final class NamedBytesResource extends ByteArrayResource {
        private final String filename;
        NamedBytesResource(byte[] bytes,String filename){super(bytes);this.filename=filename;}
        @Override public String getFilename(){return filename==null?"media":filename;}
    }
}

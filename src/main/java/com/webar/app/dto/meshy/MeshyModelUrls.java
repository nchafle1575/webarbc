package com.webar.app.dto.meshy;
import com.fasterxml.jackson.annotation.JsonProperty;
public record MeshyModelUrls(
        String glb,
        String usdz,
        @JsonProperty("pre_remeshed_glb") String preRemeshedGlb
) {}

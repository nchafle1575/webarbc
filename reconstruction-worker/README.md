# WebAR Real 3D Reconstruction Worker

This worker is the Phase 3 reconstruction engine.

Input:
- Many real photos of one food item
- One or more videos of the same food item

Pipeline:
1. Save all uploaded source media.
2. Extract one video frame per second.
3. Prepare a broad set of views and discard unusable frames.
4. COLMAP Structure-from-Motion + Multi-View Stereo.
5. Dense point cloud -> Poisson mesh.
6. Mesh simplification for mobile/WebAR.
7. COLMAP multi-view texture baking.
8. Blender export to GLB and USDZ.

The worker does **not** invent the food shape. Geometry is reconstructed from the supplied photographs/video.

For production, this service should run separately from the Spring Boot API on GPU-capable compute. COLMAP's official Docker image is CUDA-enabled. The Spring Boot Render service should remain responsible for API/database/job metadata while this worker performs the expensive reconstruction.

The current MVP passes source media from Spring Boot to the worker. The next storage step is Cloudflare R2 so both services can access durable source/output objects without depending on Render's ephemeral filesystem.

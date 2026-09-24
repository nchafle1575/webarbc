# Phase 3 — Image/Video to 3D Model

This branch adds automated 3D generation to the existing WebAR restaurant backend.

## Flow

1. Upload food photos and/or a video using the existing media endpoint.
2. Call POST /api/admin/menu-items/{menuItemId}/3d-model/generate
3. The backend uses up to 4 JPG/JPEG/PNG photos. If fewer than 4 photos are available, it extracts representative JPEG frames from uploaded video.
4. One image uses Meshy Image-to-3D; 2–4 images use Meshy Multi-Image-to-3D.
5. GLB and USDZ are requested.
6. A persistent generation job is stored in PostgreSQL.
7. A scheduled poll checks Meshy until completion.
8. On success, generated GLB/USDZ are downloaded and stored through the existing FileStorageService.
9. ThreeDAsset is updated to READY.
10. Check status with GET /api/admin/menu-items/{menuItemId}/3d-model/status.

## Render environment variables

MESHY_API_KEY=<your Meshy API key>
MESHY_BASE_URL=https://api.meshy.ai
MESHY_AI_MODEL=latest
MESHY_POLL_INTERVAL_MS=10000

Spring Boot relaxed binding maps MESHY_API_KEY to meshy.api-key.

## Existing upload endpoint

POST /api/admin/menu-items/{id}/media

The existing endpoint accepts jpg, jpeg, png, webp, mp4 and mov.
Meshy image generation uses JPG/JPEG/PNG directly. WEBP is not sent directly because Meshy's documented image inputs are JPG/JPEG/PNG. Video is converted to JPEG frames.

## Important deployment note

This Phase 3 implementation uses the existing local FileStorageService so it fits the current repository without forcing an immediate storage migration.
Render local disk is ephemeral. Generated GLB/USDZ files will not be durable across a service restart/redeploy until FileStorageService is migrated to Cloudflare R2 or another persistent object store.

## Video support

The Docker runtime installs ffmpeg. The backend extracts up to the number of frames needed to reach four input images using approximately one frame every two seconds.

## Status values

Generation job: PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELED.
Existing ThreeDAsset: NOT_STARTED, SOURCE_UPLOADED, PROCESSING, READY, FAILED.

Do not commit the Meshy API key to GitHub. Configure it as a Render environment variable.
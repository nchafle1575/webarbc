from __future__ import annotations

import os
import shutil
import subprocess
import threading
import uuid
from pathlib import Path
from typing import List

import cv2
from fastapi import BackgroundTasks, FastAPI, File, UploadFile
from fastapi.responses import FileResponse, JSONResponse

APP = FastAPI(title="WebAR Photogrammetry Worker", version="1.0.0")
ROOT = Path(os.getenv("WORK_ROOT", "/tmp/webar-reconstruction"))
MAX_INPUT_FILES = int(os.getenv("MAX_INPUT_FILES", "2000"))
MAX_VIDEO_SECONDS = int(os.getenv("MAX_VIDEO_SECONDS", "600"))

jobs: dict[str, dict] = {}
lock = threading.Lock()


def set_job(job_id: str, **values):
    with lock:
        jobs[job_id].update(values)


def run(cmd: list[str], cwd: Path | None = None):
    result = subprocess.run(
        cmd,
        cwd=str(cwd) if cwd else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stdout[-12000:])
    return result.stdout


def safe_name(name: str) -> str:
    return Path(name or "media").name.replace(" ", "_")


def save_upload(upload: UploadFile, target: Path):
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("wb") as out:
        shutil.copyfileobj(upload.file, out)


def sharpness(path: Path) -> float:
    image = cv2.imread(str(path), cv2.IMREAD_GRAYSCALE)
    if image is None:
        return 0.0
    return float(cv2.Laplacian(image, cv2.CV_64F).var())


def extract_video_frames(video: Path, output_dir: Path, start_index: int) -> int:
    probe = run([
        "ffprobe", "-v", "error", "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1", str(video)
    ]).strip()
    try:
        duration = min(float(probe), MAX_VIDEO_SECONDS)
    except ValueError:
        duration = MAX_VIDEO_SECONDS

    # One frame per second gives broad viewpoint coverage without flooding COLMAP
    # with near-identical adjacent video frames.
    count = max(1, int(duration))
    pattern = str(output_dir / f"video-{start_index:05d}-%05d.jpg")
    run([
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
        "-i", str(video),
        "-t", str(duration),
        "-vf", "fps=1,scale=1600:-2",
        "-q:v", "2",
        "-frames:v", str(count),
        pattern
    ])
    return len(list(output_dir.glob(f"video-{start_index:05d}-*.jpg")))


def prepare_images(job_dir: Path, uploaded: List[Path]) -> Path:
    image_dir = job_dir / "images"
    image_dir.mkdir(parents=True, exist_ok=True)

    candidates: list[Path] = []
    video_index = 0

    for source in uploaded:
        ext = source.suffix.lower()
        if ext in {".jpg", ".jpeg", ".png", ".webp"}:
            candidates.append(source)
        elif ext in {".mp4", ".mov", ".m4v", ".avi"}:
            before = set(image_dir.glob("*.jpg"))
            extract_video_frames(source, image_dir, video_index)
            after = sorted(image_dir.glob("*.jpg"))
            video_index += 1
            candidates.extend([p for p in after if p not in before])

    if not candidates:
        raise RuntimeError("No supported image/video frames were found")

    # Quality filtering: keep the sharpest frame from each small temporal/spatial
    # bucket instead of blindly sending every video frame.
    scored = [(sharpness(p), p) for p in candidates if p.exists()]
    scored.sort(key=lambda x: x[0], reverse=True)

    # Preserve coverage by selecting candidates in filename order with a quality
    # threshold, then fill remaining slots by sharpness.
    selected: list[Path] = []
    seen = set()
    for _, path in sorted(scored, key=lambda x: x[1].name):
        if len(selected) >= MAX_INPUT_FILES:
            break
        if path not in seen:
            selected.append(path)
            seen.add(path)

    # Copy/resize into a stable numbered sequence for COLMAP.
    final_paths: list[Path] = []
    for index, source in enumerate(selected, start=1):
        destination = image_dir / f"{index:05d}.jpg"
        if source.suffix.lower() == ".jpg" and source.parent == image_dir:
            source.rename(destination)
        else:
            image = cv2.imread(str(source))
            if image is None:
                continue
            cv2.imwrite(str(destination), image, [int(cv2.IMWRITE_JPEG_QUALITY), 95])
        final_paths.append(destination)

    if len(final_paths) < 8:
        raise RuntimeError(
            f"Only {len(final_paths)} usable images were prepared. "
            "Upload a fuller 360-degree capture; at least 8 views are required."
        )

    return image_dir


def reconstruct(job_id: str):
    job_dir = ROOT / job_id
    try:
        set_job(job_id, status="PROCESSING", progress=10)

        image_dir = prepare_images(job_dir, list((job_dir / "uploads").iterdir()))
        set_job(job_id, progress=20, source_image_count=len(list(image_dir.glob("*.jpg"))))

        run([
            "colmap", "automatic_reconstructor",
            "--workspace_path", str(job_dir / "colmap"),
            "--image_path", str(image_dir),
            "--quality", os.getenv("COLMAP_QUALITY", "MEDIUM"),
            "--data_type", "INDIVIDUAL",
            "--mesher", "POISSON",
        ])
        set_job(job_id, progress=75)

        dense = job_dir / "colmap" / "dense" / "0"
        mesh = dense / "meshed-poisson.ply"
        if not mesh.exists():
            raise RuntimeError("COLMAP did not produce a mesh. The images may not have enough overlap.")

        simplified = dense / "mesh-simplified.ply"
        run([
            "colmap", "mesh_simplifier",
            "--input_path", str(mesh),
            "--output_path", str(simplified),
            "--MeshSimplification.target_face_ratio",
            os.getenv("MESH_FACE_RATIO", "0.25"),
        ])

        textured_dir = dense / "textured"
        run([
            "colmap", "mesh_texturer",
            "--workspace_path", str(dense),
            "--input_path", str(simplified),
            "--output_path", str(textured_dir),
        ])
        set_job(job_id, progress=85)

        textured_mesh = textured_dir / "mesh.ply"
        texture = textured_dir / "texture.png"
        if not textured_mesh.exists():
            raise RuntimeError("COLMAP texturing did not produce mesh.ply")

        glb = job_dir / "model.glb"
        usdz = job_dir / "model.usdz"
        blender_script = Path("/worker/app/export_model.py")
        run([
            "blender", "--background", "--python", str(blender_script),
            "--", str(textured_mesh), str(texture), str(glb), str(usdz)
        ])
        if not glb.exists() or not usdz.exists():
            raise RuntimeError("Blender did not produce both GLB and USDZ outputs")

        set_job(job_id, status="SUCCEEDED", progress=100,
                glb=str(glb), usdz=str(usdz))
    except Exception as exc:
        set_job(job_id, status="FAILED", progress=100, error=str(exc))


@APP.get("/health")
def health():
    return {"status": "UP", "worker": "photogrammetry"}


@APP.post("/jobs")
async def create_job(background_tasks: BackgroundTasks, files: List[UploadFile] = File(...)):
    if not files:
        return JSONResponse(status_code=400, content={"error": "At least one file is required"})
    if len(files) > MAX_INPUT_FILES:
        return JSONResponse(
            status_code=400,
            content={"error": f"Maximum {MAX_INPUT_FILES} input files per job"}
        )

    job_id = uuid.uuid4().hex
    job_dir = ROOT / job_id
    upload_dir = job_dir / "uploads"
    upload_dir.mkdir(parents=True, exist_ok=True)

    for upload in files:
        save_upload(upload, upload_dir / safe_name(upload.filename))

    with lock:
        jobs[job_id] = {
            "id": job_id,
            "status": "PENDING",
            "progress": 0,
            "source_image_count": 0,
            "error": None,
        }

    background_tasks.add_task(reconstruct, job_id)
    return JSONResponse(status_code=202, content=jobs[job_id])


@APP.get("/jobs/{job_id}")
def get_job(job_id: str):
    job = jobs.get(job_id)
    if not job:
        return JSONResponse(status_code=404, content={"error": "Job not found"})
    return job


@APP.get("/jobs/{job_id}/artifacts/{artifact}")
def artifact(job_id: str, artifact: str):
    job = jobs.get(job_id)
    if not job or job.get("status") != "SUCCEEDED":
        return JSONResponse(status_code=404, content={"error": "Artifact not ready"})

    if artifact == "glb":
        return FileResponse(job["glb"], media_type="model/gltf-binary", filename="model.glb")
    if artifact == "usdz":
        return FileResponse(job["usdz"], media_type="model/vnd.usdz+zip", filename="model.usdz")
    return JSONResponse(status_code=404, content={"error": "Unknown artifact"})


@APP.delete("/jobs/{job_id}")
def delete_job(job_id: str):
    job_dir = ROOT / job_id
    if job_dir.exists():
        shutil.rmtree(job_dir, ignore_errors=True)
    jobs.pop(job_id, None)
    return {"deleted": True}

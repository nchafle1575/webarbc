FROM colmap/colmap:latest

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    blender \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /worker

COPY requirements.txt .
RUN python3 -m venv /opt/venv \
    && /opt/venv/bin/pip install --no-cache-dir -r requirements.txt

COPY app ./app

ENV PATH="/opt/venv/bin:$PATH"
ENV WORK_ROOT=/tmp/webar-reconstruction

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

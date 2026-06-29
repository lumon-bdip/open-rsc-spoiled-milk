#!/usr/bin/env sh
set -eu

GAME_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

if [ -f "$GAME_DIR/update-spoiled-milk.sh" ]; then
  if ! sh "$GAME_DIR/update-spoiled-milk.sh"; then
    printf 'Update check failed; launching installed Spoiled Milk client.\n' >&2
  fi
fi

exec java \
  -Xms512m \
  -Xmx2g \
  -Dsun.java2d.opengl=true \
  -Dspoiledmilk.directFramebuffer=true \
  -Dspoiledmilk.openglPresenter=true \
  -Dspoiledmilk.openglInput=true \
  -Dspoiledmilk.openglPrimaryWindow=true \
  -Dspoiledmilk.openglVsync=false \
  -Dspoiledmilk.renderer3DGeometryCapture=true \
  -Dspoiledmilk.openglWorldMesh=true \
  -Dspoiledmilk.openglWorldMeshTexturedVisible=true \
  -Dspoiledmilk.openglWorldMeshTexturedStaticVisible=true \
  -Dspoiledmilk.openglWorldStaticTextures=true \
  -Dspoiledmilk.openglWorldTexturedAlpha=1.0 \
  -Dspoiledmilk.openglWorldChunksTexturedVisible=true \
  -Dspoiledmilk.openglWorldChunksReplacementComposite=true \
  -Dspoiledmilk.openglWorldChunksTrustedReplacement=true \
  -Dspoiledmilk.openglWorldChunksResidentObjects=true \
  -Dspoiledmilk.openglWorldChunksSpatialCull=true \
  -Dspoiledmilk.openglWorldChunkUploadBudgetMs=3.0 \
  -Dspoiledmilk.openglWorldChunksRemasterLightingShader=true \
  -Dspoiledmilk.openglWorldSpritesVisible=true \
  -Dspoiledmilk.skipLegacyWorldRaster=true \
  -Dspoiledmilk.modernClientLoop=true \
  -jar "$GAME_DIR/Spoiled_Milk_Client.jar"

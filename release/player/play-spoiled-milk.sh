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
  -Dspoiledmilk.directFramebuffer=true \
  -Dspoiledmilk.openglPresenter=true \
  -Dspoiledmilk.openglInput=true \
  -Dspoiledmilk.openglPrimaryWindow=true \
  -Dspoiledmilk.renderer3DGeometryCapture=true \
  -Dspoiledmilk.openglWorldMesh=true \
  -Dspoiledmilk.openglWorldMeshTexturedVisible=true \
  -Dspoiledmilk.openglWorldMeshTexturedStaticVisible=true \
  -Dspoiledmilk.openglWorldStaticTextures=true \
  -Dspoiledmilk.openglWorldTexturedAlpha=1.0 \
  -Dspoiledmilk.openglWorldSpritesVisible=true \
  -jar "$GAME_DIR/Spoiled_Milk_Client.jar"

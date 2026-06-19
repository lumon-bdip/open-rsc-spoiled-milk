@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0update-spoiled-milk.ps1"
if errorlevel 1 pause & exit /b 1
java ^
  -Dspoiledmilk.directFramebuffer=true ^
  -Dspoiledmilk.openglPresenter=true ^
  -Dspoiledmilk.openglInput=true ^
  -Dspoiledmilk.openglPrimaryWindow=true ^
  -Dspoiledmilk.renderer3DGeometryCapture=true ^
  -Dspoiledmilk.openglWorldMesh=true ^
  -Dspoiledmilk.openglWorldMeshTexturedVisible=true ^
  -Dspoiledmilk.openglWorldMeshTexturedStaticVisible=true ^
  -Dspoiledmilk.openglWorldStaticTextures=true ^
  -Dspoiledmilk.openglWorldTexturedAlpha=1.0 ^
  -Dspoiledmilk.openglWorldSpritesVisible=true ^
  -jar Spoiled_Milk_Client.jar
if errorlevel 1 pause

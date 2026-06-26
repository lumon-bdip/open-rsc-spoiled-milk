#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OPENGL_PRESENTER = ROOT / "PC_Client/src/orsc/OpenGLFramePresenter.java"
GEOMETRY_SETTINGS = ROOT / "Client_Base/src/orsc/RendererGeometrySettings.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        raise AssertionError(f"missing {description}: {needle}")


def main() -> None:
    opengl_presenter = OPENGL_PRESENTER.read_text(encoding="utf-8")
    geometry_settings = GEOMETRY_SETTINGS.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")

    require(
        geometry_settings,
        'SMOOTH("smooth", "@gre@Smooth")',
        "smooth geometry mode",
    )
    require(
        geometry_settings,
        'FACETED("faceted", "@yel@Faceted")',
        "faceted geometry mode",
    )
    require(
        geometry_settings,
        'WIRE("wire", "@cya@Wire")',
        "wire geometry mode",
    )
    require(
        mudclient,
        '"@whi@Geometry - " + RendererGeometrySettings.getMode().label',
        "player-facing geometry setting row",
    )
    require(
        opengl_presenter,
        "boolean flatGeometryLighting = usesTriangleFlatWorldMeshLighting();",
        "active world mesh geometry light mode capture",
    )
    require(
        opengl_presenter,
        "private boolean usesTriangleFlatWorldMeshLighting()",
        "active world mesh faceted geometry helper",
    )
    require(
        opengl_presenter,
        "return mode == RendererGeometrySettings.Mode.FACETED\n"
        "\t\t\t\t|| mode == RendererGeometrySettings.Mode.WIRE;",
        "faceted and wire modes flatten active world mesh lighting",
    )
    require(
        opengl_presenter,
        "cachedFlatLegacyLight = triangleAverageLegacyLight(",
        "faceted active world mesh legacy light bake",
    )
    require(
        opengl_presenter,
        "cachedFlatBaseLegacyLight = triangleAverageLegacyLight(",
        "faceted active world mesh base light bake",
    )
    require(
        opengl_presenter,
        "private int triangleAverageLegacyLight(float[] vertices, int vertexCount, int triangle, int lightOffset)",
        "active world mesh per-triangle light calculation",
    )
    require(
        opengl_presenter,
        "int flatGeometryMaterialColor = flatGeometryLighting && textureRegion == null",
        "faceted active world mesh flat material re-shading",
    )
    require(
        opengl_presenter,
        "private int shadedWorldMeshMaterialColor(int color, int textureId, int legacyLight)",
        "active world mesh faceted fallback material shading",
    )
    require(
        opengl_presenter,
        "gl.glPolygonMode(gl.GL_FRONT_AND_BACK, wireGeometry ? gl.GL_LINE : gl.GL_FILL);",
        "active world mesh wire geometry draw mode",
    )
    require(
        opengl_presenter,
        "if (wireGeometry) {\n"
        "\t\t\t\t\tgl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);\n"
        "\t\t\t\t}",
        "active world mesh wire geometry restores filled draw mode",
    )

    print("PASS: OpenGL geometry modes affect the active world mesh renderer")


if __name__ == "__main__":
    main()

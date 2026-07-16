#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORLD_MESH_RENDERER = ROOT / "PC_Client/src/orsc/OpenGLWorldMeshRenderer.java"
GEOMETRY_SETTINGS = ROOT / "Client_Base/src/orsc/RendererGeometrySettings.java"
RENDERER_SETTINGS_PANEL = ROOT / "Client_Base/src/orsc/RendererSettingsPanel.java"


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        raise AssertionError(f"missing {description}: {needle}")


def main() -> None:
    world_mesh_renderer = WORLD_MESH_RENDERER.read_text(encoding="utf-8")
    geometry_settings = GEOMETRY_SETTINGS.read_text(encoding="utf-8")
    settings_panel = RENDERER_SETTINGS_PANEL.read_text(encoding="utf-8")

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
        settings_panel,
        '"@whi@Geometry - " + state.geometryLabel',
        "player-facing geometry setting row",
    )
    require(
        world_mesh_renderer,
        "boolean flatGeometryLighting = usesTriangleFlatWorldMeshLighting();",
        "active world mesh geometry light mode capture",
    )
    require(
        world_mesh_renderer,
        "private boolean usesTriangleFlatWorldMeshLighting()",
        "active world mesh faceted geometry helper",
    )
    require(
        world_mesh_renderer,
        "return mode == RendererGeometrySettings.Mode.FACETED\n"
        "\t\t\t|| mode == RendererGeometrySettings.Mode.WIRE;",
        "faceted and wire modes flatten active world mesh lighting",
    )
    require(
        world_mesh_renderer,
        "cachedFlatLegacyLight = triangleAverageLegacyLight(",
        "faceted active world mesh legacy light bake",
    )
    require(
        world_mesh_renderer,
        "cachedFlatBaseLegacyLight = triangleAverageLegacyLight(",
        "faceted active world mesh base light bake",
    )
    require(
        world_mesh_renderer,
        "private int triangleAverageLegacyLight(float[] vertices, int vertexCount, int triangle, int lightOffset)",
        "active world mesh per-triangle light calculation",
    )
    require(
        world_mesh_renderer,
        "int flatGeometryMaterialColor = flatGeometryLighting && textureRegion == null",
        "faceted active world mesh flat material re-shading",
    )
    require(
        world_mesh_renderer,
        "private int shadedWorldMeshMaterialColor(int color, int textureId, int legacyLight)",
        "active world mesh faceted fallback material shading",
    )
    require(
        world_mesh_renderer,
        "gl.glPolygonMode(gl.GL_FRONT_AND_BACK, wireGeometry ? gl.GL_LINE : gl.GL_FILL);",
        "active world mesh wire geometry draw mode",
    )
    require(
        world_mesh_renderer,
        "if (wireGeometry) {\n"
        "\t\t\t\tgl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);\n"
        "\t\t\t}",
        "active world mesh wire geometry restores filled draw mode",
    )

    print("PASS: OpenGL geometry modes affect the active world mesh renderer")


if __name__ == "__main__":
    main()

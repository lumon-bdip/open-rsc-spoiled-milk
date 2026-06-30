#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
MODEL = ROOT / "Client_Base/src/orsc/graphics/three/RSModel.java"


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        raise SystemExit(f"FAIL: missing {description}: {needle}")


def main() -> None:
    client = CLIENT.read_text(encoding="utf-8")
    model = MODEL.read_text(encoding="utf-8")

    require(
        client,
        'private static final String FISHING_SPOT_MODEL_NAME = "fishing";',
        "fishing spot model constant",
    )
    require(
        client,
        "modelCache[j] = normalizeLoadedModel(modelName, modelCache[j]);",
        "loaded model normalization return path",
    )
    require(
        client,
        "if (FISHING_SPOT_MODEL_NAME.equals(modelName)) {\n\t\t\treturn model.withFishingSpotClarityOverlay();\n\t\t}",
        "fishing spot clarity overlay hook",
    )
    require(
        model,
        "public RSModel withFishingSpotClarityOverlay()",
        "public fishing spot overlay builder",
    )
    require(
        model,
        "private static RSModel createFishingSpotClarityOverlay()",
        "fishing spot overlay model",
    )
    require(
        model,
        "int white = GenUtil.colorToResource(245, 255, 255);",
        "high-contrast white bubble color",
    )
    require(
        model,
        "int cyan = GenUtil.colorToResource(112, 220, 255);",
        "high-contrast cyan ripple color",
    )
    require(
        model,
        "markFishingSpotRippleVertices(model, addHorizontalQuad(model, -34, -38, 34, -38, 3, -3, cyan), 0);",
        "animated outer ripple geometry",
    )
    require(
        model,
        "public boolean animateFishingSpotClarityOverlay(int animationFrame, int phaseOffset)",
        "fishing spot ripple animation method",
    )
    require(
        model,
        "this.renderer3DTransformVersion++;",
        "animated ripple chunk invalidation",
    )
    require(
        client,
        "model.animateFishingSpotClarityOverlay(fishingSpotRippleFrame, phaseOffset);",
        "scenery animation hook for fishing ripples",
    )
    require(
        client,
        '("portal".equals(def.getObjectModel()) || FISHING_SPOT_MODEL_NAME.equals(def.getObjectModel()))',
        "fishing spots marked as animated resident objects",
    )
    require(
        model,
        "addBubblePyramid(model, 0, 0, 8, -8, 15, white);",
        "raised bubble geometry",
    )
    require(
        model,
        "RSModel combined = new RSModel(new RSModel[] {this, overlay}, 2);",
        "combined base and clarity overlay model",
    )


if __name__ == "__main__":
    main()

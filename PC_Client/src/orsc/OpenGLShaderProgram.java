package orsc;

import orsc.graphics.three.Renderer3DFrame;

import java.nio.FloatBuffer;

/*
 * RENDERER-V2 OWNER: GLSL source, shader compilation, attribute bindings, and
 * uniforms. Legacy-named light inputs are compatibility data until material and
 * lighting ownership are fully shader-native.
 */
final class OpenGLShaderProgram implements AutoCloseable {
	private static final int POSITION_ATTRIBUTE_LOCATION = 0;
	private static final int TEXTURE_COORD_ATTRIBUTE_LOCATION = 1;
	private static final int MATERIAL_COLOR_ATTRIBUTE_LOCATION = 2;
	private static final int LEGACY_LIGHT_ATTRIBUTE_LOCATION = 3;
	private static final int RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION = 4;
	private static final int BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION = 5;
	private static final int NORMAL_ATTRIBUTE_LOCATION = 6;
	private static final int MODEL_KIND_ATTRIBUTE_LOCATION = 7;
	private static final String FIXED_PIPELINE_VERTEX_SHADER =
		"#version 120\n"
			+ "uniform mat4 uProjectionMatrix;\n"
			+ "attribute vec3 aPosition;\n"
			+ "attribute vec2 aTexCoord;\n"
			+ "attribute vec4 aMaterialColor;\n"
			+ "attribute float aLegacyLight;\n"
			+ "attribute float aBaseLegacyLight;\n"
			+ "attribute vec3 aRawMaterialColor;\n"
			+ "varying vec2 vTexCoord;\n"
			+ "varying vec4 vMaterialColor;\n"
			+ "varying float vLegacyLight;\n"
			+ "varying float vBaseLegacyLight;\n"
			+ "varying vec3 vRawMaterialColor;\n"
			+ "void main() {\n"
			+ "\tgl_Position = uProjectionMatrix * vec4(aPosition, 1.0);\n"
			+ "\tvTexCoord = aTexCoord;\n"
			+ "\tvMaterialColor = aMaterialColor;\n"
			+ "\tvLegacyLight = aLegacyLight;\n"
			+ "\tvBaseLegacyLight = aBaseLegacyLight;\n"
			+ "\tvRawMaterialColor = aRawMaterialColor;\n"
			+ "}\n";
	private static final String FIXED_PIPELINE_FRAGMENT_SHADER =
		"#version 120\n"
		+ "uniform sampler2D uTexture;\n"
		+ "uniform int uTextureEnabled;\n"
		+ "uniform int uLightingMode;\n"
		+ "uniform float uBrightness;\n"
		+ "uniform float uFogStrength;\n"
		+ "uniform float uToneRed;\n"
		+ "uniform float uToneGreen;\n"
		+ "uniform float uToneBlue;\n"
		+ "uniform float uToneBlend;\n"
		+ "varying vec2 vTexCoord;\n"
		+ "varying vec4 vMaterialColor;\n"
		+ "varying float vLegacyLight;\n"
		+ "varying float vBaseLegacyLight;\n"
		+ "varying vec3 vRawMaterialColor;\n"
		+ "float effectiveLegacyLight(float baseLight, float combinedLight) {\n"
		+ "\tfloat fogDelta = max(0.0, combinedLight - baseLight);\n"
		+ "\treturn clamp(baseLight + fogDelta * clamp(uFogStrength, 0.0, 1.0), 0.0, 255.0);\n"
		+ "}\n"
		+ "float textureLightFactor(float light) {\n"
		+ "\tfloat clamped = clamp(light, 0.0, 255.0);\n"
		+ "\tfloat band = floor(clamped / 64.0);\n"
		+ "\tif (band < 1.0) {\n"
		+ "\t\treturn 1.0;\n"
		+ "\t}\n"
		+ "\tif (band < 2.0) {\n"
		+ "\t\treturn 216.0 / 248.0;\n"
		+ "\t}\n"
		+ "\tif (band < 3.0) {\n"
		+ "\t\treturn 184.0 / 248.0;\n"
		+ "\t}\n"
		+ "\treturn 152.0 / 248.0;\n"
		+ "}\n"
		+ "vec3 legacyFlatMaterialColor(vec3 color, float light) {\n"
		+ "\tfloat shade = 255.0 - clamp(light, 0.0, 255.0);\n"
		+ "\tvec3 channel = floor(clamp(color, 0.0, 1.0) * 255.0 + 0.0001);\n"
		+ "\tvec3 shaded = floor(channel * shade * shade / 65536.0) / 255.0;\n"
		+ "\treturn clamp(shaded * uBrightness, 0.0, 1.0);\n"
		+ "}\n"
		+ "float legacyTextureLightFactor(float light) {\n"
		+ "\tfloat band = floor(clamp(light, 0.0, 255.0) / 64.0);\n"
		+ "\tif (band < 1.0) {\n"
		+ "\t\treturn 1.0;\n"
		+ "\t}\n"
		+ "\tif (band < 2.0) {\n"
		+ "\t\treturn 216.0 / 248.0;\n"
		+ "\t}\n"
		+ "\tif (band < 3.0) {\n"
		+ "\t\treturn 184.0 / 248.0;\n"
		+ "\t}\n"
		+ "\treturn 152.0 / 248.0;\n"
		+ "}\n"
		+ "vec3 applyTone(vec3 color) {\n"
		+ "\tvec3 toned = clamp(color * vec3(uToneRed, uToneGreen, uToneBlue), 0.0, 1.0);\n"
		+ "\treturn mix(color, toned, clamp(uToneBlend, 0.0, 1.0));\n"
		+ "}\n"
		+ "void main() {\n"
		+ "\tfloat effectiveLight = effectiveLegacyLight(vBaseLegacyLight, vLegacyLight);\n"
		+ "\tvec4 color = uTextureEnabled != 0\n"
		+ "\t\t? vec4(vec3(textureLightFactor(effectiveLight) * uBrightness), vMaterialColor.a) * texture2D(uTexture, vTexCoord)\n"
		+ "\t\t: vec4(legacyFlatMaterialColor(vRawMaterialColor, effectiveLight), vMaterialColor.a);\n"
		+ "\tcolor.rgb = applyTone(color.rgb);\n"
		+ "\tgl_FragColor = color;\n"
		+ "}\n";
	private static final String RESIDENT_CHUNK_PARITY_VERTEX_SHADER =
		"#version 120\n"
			+ "uniform mat4 uProjectionMatrix;\n"
			+ "uniform mat4 uWorldViewMatrix;\n"
			+ "attribute vec3 aPosition;\n"
			+ "attribute vec2 aTexCoord;\n"
			+ "attribute vec4 aMaterialColor;\n"
			+ "attribute vec3 aRawMaterialColor;\n"
			+ "attribute float aBaseLegacyLight;\n"
			+ "attribute vec3 aNormal;\n"
			+ "attribute float aModelKind;\n"
			+ "varying vec2 vTexCoord;\n"
			+ "varying vec4 vMaterialColor;\n"
			+ "varying vec3 vRawMaterialColor;\n"
			+ "varying float vBaseLegacyLight;\n"
			+ "varying vec3 vNormal;\n"
			+ "varying float vModelKind;\n"
			+ "varying float vCameraDepth;\n"
			+ "void main() {\n"
			+ "\tvec4 worldPosition = vec4(aPosition, 1.0);\n"
			+ "\tgl_Position = uProjectionMatrix * worldPosition;\n"
			+ "\tvTexCoord = aTexCoord;\n"
			+ "\tvMaterialColor = aMaterialColor;\n"
			+ "\tvRawMaterialColor = aRawMaterialColor;\n"
			+ "\tvBaseLegacyLight = aBaseLegacyLight;\n"
			+ "\tvNormal = aNormal;\n"
			+ "\tvModelKind = aModelKind;\n"
			+ "\tvCameraDepth = (uWorldViewMatrix * worldPosition).z;\n"
			+ "}\n";
	private static final String RESIDENT_CHUNK_PARITY_FRAGMENT_SHADER =
		"#version 120\n"
			+ "uniform sampler2D uTexture;\n"
			+ "uniform int uTextureEnabled;\n"
			+ "uniform int uRawMaterialMode;\n"
			+ "uniform int uRemasterLightingEnabled;\n"
			+ "uniform float uLightDirectionX;\n"
			+ "uniform float uLightDirectionY;\n"
			+ "uniform float uLightDirectionZ;\n"
			+ "uniform float uLightAmbient;\n"
			+ "uniform float uLightIntensity;\n"
			+ "uniform int uFogEnabled;\n"
			+ "uniform float uFogStart;\n"
			+ "uniform float uFogEnd;\n"
			+ "uniform float uToneRed;\n"
			+ "uniform float uToneGreen;\n"
			+ "uniform float uToneBlue;\n"
			+ "uniform float uToneBlend;\n"
			+ "uniform float uReliefStrength;\n"
			+ "varying vec2 vTexCoord;\n"
			+ "varying vec4 vMaterialColor;\n"
			+ "varying vec3 vRawMaterialColor;\n"
			+ "varying float vBaseLegacyLight;\n"
			+ "varying vec3 vNormal;\n"
			+ "varying float vModelKind;\n"
			+ "varying float vCameraDepth;\n"
			+ "vec3 remasterNormal() {\n"
			+ "\tfloat normalLengthSquared = dot(vNormal, vNormal);\n"
			+ "\tif (normalLengthSquared <= 0.0001) {\n"
			+ "\t\treturn vModelKind > 1.5 && vModelKind < 2.5 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);\n"
			+ "\t}\n"
			+ "\treturn normalize(vNormal);\n"
			+ "}\n"
			+ "float wrappedDiffuse(vec3 normal, vec3 lightDirection, float wrap) {\n"
			+ "\treturn clamp((dot(normal, lightDirection) + wrap) / (1.0 + wrap), 0.0, 1.0);\n"
			+ "}\n"
			+ "float remasterDiffuse(vec3 lightDirection) {\n"
			+ "\tvec3 normal = remasterNormal();\n"
			+ "\tif (vModelKind > 0.5 && vModelKind < 1.5) {\n"
			+ "\t\tvec3 terrainNormal = normalize(mix(vec3(0.0, 1.0, 0.0), normal, 0.85));\n"
			+ "\t\tfloat terrainDiffuse = wrappedDiffuse(terrainNormal, lightDirection, 0.08);\n"
			+ "\t\treturn smoothstep(0.08, 0.92, terrainDiffuse);\n"
			+ "\t}\n"
			+ "\tfloat wrapped = wrappedDiffuse(normal, lightDirection, 0.65);\n"
			+ "\tfloat twoSided = abs(dot(normal, lightDirection)) * 0.45;\n"
			+ "\tfloat skyFill = max(lightDirection.y, 0.0) * 0.20;\n"
			+ "\treturn clamp(max(wrapped, twoSided) + skyFill, 0.0, 1.0);\n"
			+ "}\n"
			+ "float remasterClassicShadeFactor(float diffuse) {\n"
			+ "\tfloat directLight = clamp(uLightAmbient + diffuse * uLightIntensity, 0.0, 1.0);\n"
			+ "\tfloat shaped = smoothstep(0.18, 0.92, directLight);\n"
			+ "\tshaped = shaped * shaped * (3.0 - 2.0 * shaped);\n"
			+ "\tfloat materialFloor = uTextureEnabled != 0 ? 0.50 : 0.40;\n"
			+ "\tfloat materialCeiling = uTextureEnabled != 0 ? 0.90 : 0.82;\n"
			+ "\treturn mix(materialFloor, materialCeiling, shaped);\n"
			+ "}\n"
			+ "float remasterLocalReliefFactor() {\n"
			+ "\tfloat legacyDetail = smoothstep(28.0, 196.0, clamp(vBaseLegacyLight, 0.0, 255.0));\n"
			+ "\tfloat detailFloor = vModelKind > 0.5 && vModelKind < 1.5 ? 0.84 : 0.89;\n"
			+ "\tfloat detailCeiling = vModelKind > 0.5 && vModelKind < 1.5 ? 1.08 : 1.04;\n"
			+ "\tfloat reliefFactor = mix(detailCeiling, detailFloor, legacyDetail);\n"
			+ "\treturn clamp(mix(1.0, reliefFactor, clamp(uReliefStrength, 0.0, 2.5)), 0.35, 1.25);\n"
			+ "}\n"
			+ "vec3 applyTone(vec3 color) {\n"
			+ "\tvec3 toned = clamp(color * vec3(uToneRed, uToneGreen, uToneBlue), 0.0, 1.0);\n"
			+ "\treturn mix(color, toned, clamp(uToneBlend, 0.0, 1.0));\n"
			+ "}\n"
			+ "void main() {\n"
			+ "\tvec4 color;\n"
			+ "\tif (uRawMaterialMode != 0) {\n"
			+ "\t\tcolor = uTextureEnabled != 0\n"
			+ "\t\t\t? texture2D(uTexture, vTexCoord)\n"
			+ "\t\t\t: vec4(vRawMaterialColor, vMaterialColor.a);\n"
			+ "\t} else {\n"
			+ "\t\tcolor = uTextureEnabled != 0\n"
			+ "\t\t\t? texture2D(uTexture, vTexCoord) * vMaterialColor\n"
			+ "\t\t\t: vMaterialColor;\n"
			+ "\t}\n"
			+ "\tif (uRemasterLightingEnabled != 0) {\n"
			+ "\t\tvec3 lightDirection = normalize(vec3(uLightDirectionX, uLightDirectionY, uLightDirectionZ));\n"
			+ "\t\tfloat diffuse = remasterDiffuse(lightDirection);\n"
			+ "\t\tcolor.rgb *= remasterClassicShadeFactor(diffuse) * remasterLocalReliefFactor();\n"
			+ "\t}\n"
			+ "\tcolor.rgb = applyTone(color.rgb);\n"
			+ "\tif (uFogEnabled != 0) {\n"
			+ "\t\tfloat fogRange = max(1.0, uFogEnd - uFogStart);\n"
			+ "\t\tfloat fogFactor = clamp((uFogEnd - vCameraDepth) / fogRange, 0.0, 1.0);\n"
			+ "\t\tcolor.rgb = mix(vec3(0.0, 0.0, 0.0), color.rgb, fogFactor);\n"
			+ "\t}\n"
			+ "\tgl_FragColor = color;\n"
			+ "}\n";

	private final LwjglBindings gl;
	private final int programId;
	private final int projectionMatrixUniformLocation;
	private final int worldViewMatrixUniformLocation;
	private final int textureUniformLocation;
	private final int textureEnabledUniformLocation;
	private final int rawMaterialModeUniformLocation;
	private final int remasterLightingEnabledUniformLocation;
	private final int lightDirectionXUniformLocation;
	private final int lightDirectionYUniformLocation;
	private final int lightDirectionZUniformLocation;
	private final int lightAmbientUniformLocation;
	private final int lightIntensityUniformLocation;
	private final int lightingModeUniformLocation;
	private final int brightnessUniformLocation;
	private final int fogStrengthUniformLocation;
	private final int toneRedUniformLocation;
	private final int toneGreenUniformLocation;
	private final int toneBlueUniformLocation;
	private final int toneBlendUniformLocation;
	private final int reliefStrengthUniformLocation;
	private final int fogEnabledUniformLocation;
	private final int fogStartUniformLocation;
	private final int fogEndUniformLocation;
	private boolean closed;

	private OpenGLShaderProgram(
		LwjglBindings gl,
		int programId,
		int projectionMatrixUniformLocation,
		int worldViewMatrixUniformLocation,
		int textureUniformLocation,
		int textureEnabledUniformLocation,
		int rawMaterialModeUniformLocation,
		int remasterLightingEnabledUniformLocation,
		int lightDirectionXUniformLocation,
		int lightDirectionYUniformLocation,
		int lightDirectionZUniformLocation,
		int lightAmbientUniformLocation,
		int lightIntensityUniformLocation,
		int lightingModeUniformLocation,
		int brightnessUniformLocation,
		int fogStrengthUniformLocation,
		int toneRedUniformLocation,
		int toneGreenUniformLocation,
		int toneBlueUniformLocation,
		int toneBlendUniformLocation,
		int reliefStrengthUniformLocation,
		int fogEnabledUniformLocation,
		int fogStartUniformLocation,
		int fogEndUniformLocation) {
		this.gl = gl;
		this.programId = programId;
		this.projectionMatrixUniformLocation = projectionMatrixUniformLocation;
		this.worldViewMatrixUniformLocation = worldViewMatrixUniformLocation;
		this.textureUniformLocation = textureUniformLocation;
		this.textureEnabledUniformLocation = textureEnabledUniformLocation;
		this.rawMaterialModeUniformLocation = rawMaterialModeUniformLocation;
		this.remasterLightingEnabledUniformLocation = remasterLightingEnabledUniformLocation;
		this.lightDirectionXUniformLocation = lightDirectionXUniformLocation;
		this.lightDirectionYUniformLocation = lightDirectionYUniformLocation;
		this.lightDirectionZUniformLocation = lightDirectionZUniformLocation;
		this.lightAmbientUniformLocation = lightAmbientUniformLocation;
		this.lightIntensityUniformLocation = lightIntensityUniformLocation;
		this.lightingModeUniformLocation = lightingModeUniformLocation;
		this.brightnessUniformLocation = brightnessUniformLocation;
		this.fogStrengthUniformLocation = fogStrengthUniformLocation;
		this.toneRedUniformLocation = toneRedUniformLocation;
		this.toneGreenUniformLocation = toneGreenUniformLocation;
		this.toneBlueUniformLocation = toneBlueUniformLocation;
		this.toneBlendUniformLocation = toneBlendUniformLocation;
		this.reliefStrengthUniformLocation = reliefStrengthUniformLocation;
		this.fogEnabledUniformLocation = fogEnabledUniformLocation;
		this.fogStartUniformLocation = fogStartUniformLocation;
		this.fogEndUniformLocation = fogEndUniformLocation;
	}

	static OpenGLShaderProgram createProjectedWorld(LwjglBindings gl) throws Exception {
		int vertexShader = compileShader(gl, gl.GL_VERTEX_SHADER, FIXED_PIPELINE_VERTEX_SHADER);
		int fragmentShader = 0;
		int program = 0;
		try {
			fragmentShader = compileShader(gl, gl.GL_FRAGMENT_SHADER, FIXED_PIPELINE_FRAGMENT_SHADER);
			program = gl.glCreateProgram();
			gl.glAttachShader(program, vertexShader);
			gl.glAttachShader(program, fragmentShader);
			gl.glBindAttribLocation(program, POSITION_ATTRIBUTE_LOCATION, "aPosition");
			gl.glBindAttribLocation(program, TEXTURE_COORD_ATTRIBUTE_LOCATION, "aTexCoord");
			gl.glBindAttribLocation(program, MATERIAL_COLOR_ATTRIBUTE_LOCATION, "aMaterialColor");
			gl.glBindAttribLocation(program, LEGACY_LIGHT_ATTRIBUTE_LOCATION, "aLegacyLight");
			gl.glBindAttribLocation(program, RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION, "aRawMaterialColor");
			gl.glBindAttribLocation(program, BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION, "aBaseLegacyLight");
			gl.glLinkProgram(program);
			if (gl.glGetProgrami(program, gl.GL_LINK_STATUS) == 0) {
				String log = gl.glGetProgramInfoLog(program);
				throw new IllegalStateException("shader link failed: " + log);
			}
			OpenGLShaderProgram shaderProgram =
				new OpenGLShaderProgram(
					gl,
					program,
					gl.glGetUniformLocation(program, "uProjectionMatrix"),
					-1,
					gl.glGetUniformLocation(program, "uTexture"),
					gl.glGetUniformLocation(program, "uTextureEnabled"),
					-1,
					-1,
					-1,
					-1,
					-1,
					-1,
					-1,
					gl.glGetUniformLocation(program, "uLightingMode"),
					gl.glGetUniformLocation(program, "uBrightness"),
					gl.glGetUniformLocation(program, "uFogStrength"),
					gl.glGetUniformLocation(program, "uToneRed"),
					gl.glGetUniformLocation(program, "uToneGreen"),
					gl.glGetUniformLocation(program, "uToneBlue"),
					gl.glGetUniformLocation(program, "uToneBlend"),
					-1,
					-1,
					-1,
					-1);
			program = 0;
			return shaderProgram;
		} finally {
			if (program != 0) {
				gl.glDeleteProgram(program);
			}
			if (fragmentShader != 0) {
				gl.glDeleteShader(fragmentShader);
			}
			if (vertexShader != 0) {
				gl.glDeleteShader(vertexShader);
			}
		}
	}

	static OpenGLShaderProgram createResidentChunkParity(LwjglBindings gl) throws Exception {
		int vertexShader = compileShader(gl, gl.GL_VERTEX_SHADER, RESIDENT_CHUNK_PARITY_VERTEX_SHADER);
		int fragmentShader = 0;
		int program = 0;
		try {
			fragmentShader = compileShader(gl, gl.GL_FRAGMENT_SHADER, RESIDENT_CHUNK_PARITY_FRAGMENT_SHADER);
			program = gl.glCreateProgram();
			gl.glAttachShader(program, vertexShader);
			gl.glAttachShader(program, fragmentShader);
			gl.glBindAttribLocation(program, POSITION_ATTRIBUTE_LOCATION, "aPosition");
			gl.glBindAttribLocation(program, TEXTURE_COORD_ATTRIBUTE_LOCATION, "aTexCoord");
			gl.glBindAttribLocation(program, MATERIAL_COLOR_ATTRIBUTE_LOCATION, "aMaterialColor");
			gl.glBindAttribLocation(program, RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION, "aRawMaterialColor");
			gl.glBindAttribLocation(program, BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION, "aBaseLegacyLight");
			gl.glBindAttribLocation(program, NORMAL_ATTRIBUTE_LOCATION, "aNormal");
			gl.glBindAttribLocation(program, MODEL_KIND_ATTRIBUTE_LOCATION, "aModelKind");
			gl.glLinkProgram(program);
			if (gl.glGetProgrami(program, gl.GL_LINK_STATUS) == 0) {
				String log = gl.glGetProgramInfoLog(program);
				throw new IllegalStateException("shader link failed: " + log);
			}
			OpenGLShaderProgram shaderProgram =
				new OpenGLShaderProgram(
					gl,
					program,
					gl.glGetUniformLocation(program, "uProjectionMatrix"),
					gl.glGetUniformLocation(program, "uWorldViewMatrix"),
					gl.glGetUniformLocation(program, "uTexture"),
					gl.glGetUniformLocation(program, "uTextureEnabled"),
					gl.glGetUniformLocation(program, "uRawMaterialMode"),
					gl.glGetUniformLocation(program, "uRemasterLightingEnabled"),
					gl.glGetUniformLocation(program, "uLightDirectionX"),
					gl.glGetUniformLocation(program, "uLightDirectionY"),
					gl.glGetUniformLocation(program, "uLightDirectionZ"),
					gl.glGetUniformLocation(program, "uLightAmbient"),
					gl.glGetUniformLocation(program, "uLightIntensity"),
					-1,
					-1,
					-1,
					gl.glGetUniformLocation(program, "uToneRed"),
					gl.glGetUniformLocation(program, "uToneGreen"),
					gl.glGetUniformLocation(program, "uToneBlue"),
					gl.glGetUniformLocation(program, "uToneBlend"),
					gl.glGetUniformLocation(program, "uReliefStrength"),
					gl.glGetUniformLocation(program, "uFogEnabled"),
					gl.glGetUniformLocation(program, "uFogStart"),
					gl.glGetUniformLocation(program, "uFogEnd"));
			program = 0;
			return shaderProgram;
		} finally {
			if (program != 0) {
				gl.glDeleteProgram(program);
			}
			if (fragmentShader != 0) {
				gl.glDeleteShader(fragmentShader);
			}
			if (vertexShader != 0) {
				gl.glDeleteShader(vertexShader);
			}
		}
	}

	private static int compileShader(LwjglBindings gl, int type, String source) throws Exception {
		int shader = gl.glCreateShader(type);
		try {
			gl.glShaderSource(shader, source);
			gl.glCompileShader(shader);
			if (gl.glGetShaderi(shader, gl.GL_COMPILE_STATUS) == 0) {
				String log = gl.glGetShaderInfoLog(shader);
				throw new IllegalStateException("shader compile failed: " + log);
			}
			int compiledShader = shader;
			shader = 0;
			return compiledShader;
		} finally {
			if (shader != 0) {
				gl.glDeleteShader(shader);
			}
		}
	}

	void useWorld(FloatBuffer projectionMatrix, boolean textureEnabled) throws Exception {
		if (projectionMatrix == null) {
			throw new IllegalArgumentException("world shader requires an explicit projection matrix");
		}
		gl.glUseProgram(programId);
		if (projectionMatrixUniformLocation >= 0) {
			gl.glUniformMatrix4fv(projectionMatrixUniformLocation, false, projectionMatrix);
		}
		if (textureUniformLocation >= 0) {
			gl.glUniform1i(textureUniformLocation, 0);
		}
		if (textureEnabledUniformLocation >= 0) {
			gl.glUniform1i(textureEnabledUniformLocation, textureEnabled ? 1 : 0);
		}
		if (lightingModeUniformLocation >= 0) {
			gl.glUniform1i(lightingModeUniformLocation, RendererLightingSettings.getMode().ordinal());
		}
		if (brightnessUniformLocation >= 0) {
			gl.glUniform1f(brightnessUniformLocation, RendererDayNightCycle.currentBrightnessMultiplier());
		}
		if (fogStrengthUniformLocation >= 0) {
			gl.glUniform1f(fogStrengthUniformLocation, RendererFogSettings.getMode().multiplier);
		}
		RendererDayNightCycle.Presentation presentation = RendererDayNightCycle.currentPresentation();
		if (toneRedUniformLocation >= 0) {
			gl.glUniform1f(toneRedUniformLocation, presentation.redMultiplier);
		}
		if (toneGreenUniformLocation >= 0) {
			gl.glUniform1f(toneGreenUniformLocation, presentation.greenMultiplier);
		}
		if (toneBlueUniformLocation >= 0) {
			gl.glUniform1f(toneBlueUniformLocation, presentation.blueMultiplier);
		}
		if (toneBlendUniformLocation >= 0) {
			gl.glUniform1f(toneBlendUniformLocation, presentation.toneBlend);
		}
		if (reliefStrengthUniformLocation >= 0) {
			gl.glUniform1f(reliefStrengthUniformLocation, RendererReliefSettings.getStrength());
		}
	}

	void useResidentChunk(
		FloatBuffer worldToClipMatrix,
		FloatBuffer worldViewMatrix,
		boolean textureEnabled,
		boolean rawMaterialMode,
		boolean remasterLightingEnabled,
		Renderer3DFrame frame) throws Exception {
		if (worldViewMatrix == null) {
			throw new IllegalArgumentException("resident chunk shader requires an explicit world-view matrix");
		}
		useWorld(worldToClipMatrix, textureEnabled);
		if (worldViewMatrixUniformLocation >= 0) {
			gl.glUniformMatrix4fv(worldViewMatrixUniformLocation, false, worldViewMatrix);
		}
		if (rawMaterialModeUniformLocation >= 0) {
			gl.glUniform1i(rawMaterialModeUniformLocation, rawMaterialMode ? 1 : 0);
		}
		if (remasterLightingEnabledUniformLocation >= 0) {
			gl.glUniform1i(remasterLightingEnabledUniformLocation, remasterLightingEnabled ? 1 : 0);
		}
		if (lightDirectionXUniformLocation >= 0) {
			gl.glUniform1f(lightDirectionXUniformLocation, RendererRemasterLightSettings.getLightDirectionX());
		}
		if (lightDirectionYUniformLocation >= 0) {
			gl.glUniform1f(lightDirectionYUniformLocation, RendererRemasterLightSettings.getLightDirectionY());
		}
		if (lightDirectionZUniformLocation >= 0) {
			gl.glUniform1f(lightDirectionZUniformLocation, RendererRemasterLightSettings.getLightDirectionZ());
		}
		if (lightAmbientUniformLocation >= 0) {
			gl.glUniform1f(lightAmbientUniformLocation, RendererRemasterLightSettings.getAmbient());
		}
		if (lightIntensityUniformLocation >= 0) {
			gl.glUniform1f(lightIntensityUniformLocation, RendererRemasterLightSettings.getIntensity());
		}
		boolean fogEnabled =
			!rawMaterialMode
				&& !remasterLightingEnabled
				&& frame != null
				&& RendererFogSettings.getMode() != RendererFogSettings.Mode.OFF;
		if (fogEnabledUniformLocation >= 0) {
			gl.glUniform1i(fogEnabledUniformLocation, fogEnabled ? 1 : 0);
		}
		if (fogStartUniformLocation >= 0) {
			gl.glUniform1f(fogStartUniformLocation, fogEnabled ? frame.getFogStartDistance() : 0.0f);
		}
		if (fogEndUniformLocation >= 0) {
			gl.glUniform1f(fogEndUniformLocation, fogEnabled ? frame.getFogDistance() : 1.0f);
		}
	}

	void bindWorldParityAttributes(
		int positionComponents,
		int textureCoordComponents,
		int materialColorComponents,
		int rawMaterialColorComponents,
		int baseLegacyLightComponents,
		int normalComponents,
		int modelKindComponents,
		int strideBytes,
		long positionOffsetBytes,
		long textureCoordOffsetBytes,
		long materialColorOffsetBytes,
		long rawMaterialColorOffsetBytes,
		long baseLegacyLightOffsetBytes,
		long normalOffsetBytes,
		long modelKindOffsetBytes) throws Exception {
		gl.glDisableClientState(gl.GL_COLOR_ARRAY);
		gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glEnableVertexAttribArray(POSITION_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(TEXTURE_COORD_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(MATERIAL_COLOR_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(NORMAL_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(MODEL_KIND_ATTRIBUTE_LOCATION);
		gl.glVertexAttribPointer(
			POSITION_ATTRIBUTE_LOCATION,
			positionComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			positionOffsetBytes);
		gl.glVertexAttribPointer(
			TEXTURE_COORD_ATTRIBUTE_LOCATION,
			textureCoordComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			textureCoordOffsetBytes);
		gl.glVertexAttribPointer(
			MATERIAL_COLOR_ATTRIBUTE_LOCATION,
			materialColorComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			materialColorOffsetBytes);
		gl.glVertexAttribPointer(
			RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION,
			rawMaterialColorComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			rawMaterialColorOffsetBytes);
		gl.glVertexAttribPointer(
			BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION,
			baseLegacyLightComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			baseLegacyLightOffsetBytes);
		gl.glVertexAttribPointer(
			NORMAL_ATTRIBUTE_LOCATION,
			normalComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			normalOffsetBytes);
		gl.glVertexAttribPointer(
			MODEL_KIND_ATTRIBUTE_LOCATION,
			modelKindComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			modelKindOffsetBytes);
	}

	void bindWorldTextureAttributes(
		int positionComponents,
		int textureCoordComponents,
		int materialColorComponents,
		int legacyLightComponents,
		int baseLegacyLightComponents,
		int rawMaterialColorComponents,
		int strideBytes,
		long positionOffsetBytes,
		long textureCoordOffsetBytes,
		long materialColorOffsetBytes,
		long legacyLightOffsetBytes,
		long baseLegacyLightOffsetBytes,
		long rawMaterialColorOffsetBytes) throws Exception {
		gl.glDisableClientState(gl.GL_COLOR_ARRAY);
		gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glEnableVertexAttribArray(POSITION_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(TEXTURE_COORD_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(MATERIAL_COLOR_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(LEGACY_LIGHT_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION);
		gl.glEnableVertexAttribArray(RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION);
		gl.glVertexAttribPointer(
			POSITION_ATTRIBUTE_LOCATION,
			positionComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			positionOffsetBytes);
		gl.glVertexAttribPointer(
			TEXTURE_COORD_ATTRIBUTE_LOCATION,
			textureCoordComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			textureCoordOffsetBytes);
		gl.glVertexAttribPointer(
			MATERIAL_COLOR_ATTRIBUTE_LOCATION,
			materialColorComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			materialColorOffsetBytes);
		gl.glVertexAttribPointer(
			LEGACY_LIGHT_ATTRIBUTE_LOCATION,
			legacyLightComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			legacyLightOffsetBytes);
		gl.glVertexAttribPointer(
			BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION,
			baseLegacyLightComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			baseLegacyLightOffsetBytes);
		gl.glVertexAttribPointer(
			RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION,
			rawMaterialColorComponents,
			gl.GL_FLOAT,
			false,
			strideBytes,
			rawMaterialColorOffsetBytes);
	}

	void bindWorldFlatColorAttributes(
		int positionComponents,
		int textureCoordComponents,
		int materialColorComponents,
		int strideBytes,
		long positionOffsetBytes,
		long textureCoordOffsetBytes,
		long materialColorOffsetBytes,
		long legacyLightOffsetBytes,
		long baseLegacyLightOffsetBytes,
		long rawMaterialColorOffsetBytes) throws Exception {
		bindWorldTextureAttributes(
			positionComponents,
			textureCoordComponents,
			materialColorComponents,
			1,
			1,
			3,
			strideBytes,
			positionOffsetBytes,
			textureCoordOffsetBytes,
			materialColorOffsetBytes,
			legacyLightOffsetBytes,
			baseLegacyLightOffsetBytes,
			rawMaterialColorOffsetBytes);
	}

	void unbindWorldTextureAttributes() throws Exception {
		gl.glDisableVertexAttribArray(MODEL_KIND_ATTRIBUTE_LOCATION);
		gl.glDisableVertexAttribArray(NORMAL_ATTRIBUTE_LOCATION);
		gl.glDisableVertexAttribArray(RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION);
		gl.glDisableVertexAttribArray(BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION);
		gl.glDisableVertexAttribArray(LEGACY_LIGHT_ATTRIBUTE_LOCATION);
		gl.glDisableVertexAttribArray(MATERIAL_COLOR_ATTRIBUTE_LOCATION);
		gl.glDisableVertexAttribArray(TEXTURE_COORD_ATTRIBUTE_LOCATION);
		gl.glDisableVertexAttribArray(POSITION_ATTRIBUTE_LOCATION);
	}

	@Override
	public void close() throws Exception {
		if (closed) {
			return;
		}
		closed = true;
		if (programId != 0) {
			gl.glDeleteProgram(programId);
		}
	}
}

package net.coderbot.iris.mixin;

import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.CoreWorldRenderingPipeline;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.render.Shader;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.GameRenderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public class MixinGameRenderer {
	// TODO: This probably won't be compatible with mods that directly mess with the GL projection matrix.
	// https://github.com/jellysquid3/sodium-fabric/blob/1df506fd39dac56bb410725c245e6e51208ec732/src/main/java/me/jellysquid/mods/sodium/client/render/chunk/shader/ChunkProgram.java#L56
	@Inject(method = "loadProjectionMatrix(Lnet/minecraft/util/math/Matrix4f;)V", at = @At("HEAD"))
	private void iris$captureProjectionMatrix(Matrix4f projectionMatrix, CallbackInfo callback) {
		CapturedRenderingState.INSTANCE.setGbufferProjection(projectionMatrix);
	}

	@Inject(method = "render(FJZ)V", at = @At("HEAD"))
	private void iris$beginFrame(float tickDelta, long startTime, boolean tick, CallbackInfo callback) {
		SystemTimeUniforms.TIMER.beginFrame(startTime);
	}

	@Inject(method = "getRenderTypeSolidShader()Lnet/minecraft/client/render/Shader;", at = @At("HEAD"), cancellable = true)
	private static void iris$overrideSolidShader(CallbackInfoReturnable<Shader> cir) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipeline();

		if (pipeline instanceof CoreWorldRenderingPipeline) {
			Shader override = ((CoreWorldRenderingPipeline) pipeline).getTextured();

			if (override != null) {
				cir.setReturnValue(override);
			}
		}
	}
}

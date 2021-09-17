package net.coderbot.iris.mixin.shadows;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.coderbot.iris.pipeline.ShadowRenderer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent nearby chunks from being rebuilt on the main thread in the shadow pass. Aside from causing  FPS to tank,
 * this also causes weird chunk corruption! It's critical to make sure that it's disabled as a result.
 *
 * This patch is not relevant with Sodium installed since Sodium has a completely different build path for terrain
 * setup.
 */
@Mixin(WorldRenderer.class)
public abstract class MixinPreventRebuildNearInShadowPass {
	@Shadow
	@Final
	private ObjectArrayList<WorldRenderer.ChunkInfo> field_34807;

	@Shadow
	protected abstract void method_38551(Frustum frustum);

	private static final String PROFILER_SWAP = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V";

	@Inject(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;get()Ljava/lang/Object;"), cancellable = true)
	private void iris$preventRebuildNearInShadowPass(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean bl, CallbackInfo ci) {
		if (ShadowRenderer.ACTIVE) {
			for (WorldRenderer.ChunkInfo chunk : this.field_34807) {
				for (BlockEntity entity : ((ChunkInfoAccessor) chunk).getChunk().getData().getBlockEntities()) {
					ShadowRenderer.visibleBlockEntities.add(entity);
				}
			}
			MinecraftClient.getInstance().getProfiler().pop();
			this.method_38551(frustum);
			ci.cancel();
		}
	}

	//@Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;method_38551(Lnet/minecraft/client/render/Frustum;)V"))
	private void dontupdatefrustum(WorldRenderer worldRenderer, Frustum frustum) {
		if (!ShadowRenderer.ACTIVE) {
			this.method_38551(frustum);
		}
	}
}

package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class GrapplingHookRopeRenderUtil {
    private static final int LEASH_RENDER_STEPS = 24;
    private static final float LEASH_WIDTH = 0.08F;
    private static final float LEASH_SAG = 0.18F;

    private GrapplingHookRopeRenderUtil() {
    }

    public static void renderLocal(Vec3 vector, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float x = (float) vector.x;
        float y = (float) vector.y;
        float z = (float) vector.z;
        float horizontalLengthSqr = x * x + z * z;
        float sideX;
        float sideZ;
        if (horizontalLengthSqr < 1.0E-6F) {
            sideX = LEASH_WIDTH * 0.5F;
            sideZ = 0.0F;
        } else {
            float invHorizontalLength = Mth.invSqrt(horizontalLengthSqr) * LEASH_WIDTH * 0.5F;
            sideX = z * invHorizontalLength;
            sideZ = -x * invHorizontalLength;
        }

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();
        for (int step = 0; step <= LEASH_RENDER_STEPS; step++) {
            addLeashVertexPair(lineConsumer, matrix, x, y, z, sideX, sideZ, packedLight, step, false);
        }
        for (int step = LEASH_RENDER_STEPS; step >= 0; step--) {
            addLeashVertexPair(lineConsumer, matrix, x, y, z, sideX, sideZ, packedLight, step, true);
        }
    }

    private static void addLeashVertexPair(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float sideX,
            float sideZ,
            int packedLight,
            int step,
            boolean reverse) {
        float t = (float) step / LEASH_RENDER_STEPS;
        float shade = (step % 2 == (reverse ? 1 : 0)) ? 0.7F : 1.0F;
        float px = x * t;
        float py = y * t - LEASH_SAG * t * (1.0F - t);
        float pz = z * t;
        float firstYOffset = reverse ? 0.0F : LEASH_WIDTH;
        float secondYOffset = LEASH_WIDTH - firstYOffset;
        float red = GrapplingHookLineDebugControls.red(step) * shade;
        float green = GrapplingHookLineDebugControls.green(step) * shade;
        float blue = GrapplingHookLineDebugControls.blue(step) * shade;

        consumer.vertex(matrix, px + sideX, py + firstYOffset, pz + sideZ)
                .color(red, green, blue, 1.0F)
                .uv2(packedLight)
                .endVertex();
        consumer.vertex(matrix, px - sideX, py + secondYOffset, pz - sideZ)
                .color(red, green, blue, 1.0F)
                .uv2(packedLight)
                .endVertex();
    }
}

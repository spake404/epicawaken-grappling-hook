package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;

public final class GrapplingHookArmModelRenderer {
    public static final ResourceLocation ARM_MODEL = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "item/grappling_hook_arm");
    public static final ResourceLocation ARM_PULL_MODEL = ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "item/grappling_hook_arm_pull");
    public static final ItemDisplayContext WORN = ItemDisplayContext.create(
            "EPICAWAKEN_GRAPPLING_HOOK_WORN",
            ResourceLocation.fromNamespaceAndPath(Epicawaken_grappling_hook.MODID, "worn"),
            ItemDisplayContext.FIXED);

    private GrapplingHookArmModelRenderer() {
    }

    public static void render(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        render(ARM_MODEL, stack, poseStack, bufferSource, packedLight);
    }

    public static void render(
            ResourceLocation modelLocation,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(modelLocation);
        ItemTransform transform = model.getTransforms().getTransform(WORN);
        GrapplingHookRenderPathDebug.logModelRender(modelLocation, model, WORN, transform);
        minecraft.getItemRenderer().render(
                stack,
                WORN,
                false,
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                model);
    }
}

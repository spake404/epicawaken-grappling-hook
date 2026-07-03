package org.com.epicawaken_grappling_hook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

public class EpicFightGrapplingHookFirstPersonItemInHandLayer extends PatchedLayer<LocalPlayer, LocalPlayerPatch, PlayerModel<LocalPlayer>, PlayerItemInHandLayer<LocalPlayer, PlayerModel<LocalPlayer>>> {
    @Override
    protected void renderLayer(
            LocalPlayerPatch entityPatch,
            LocalPlayer player,
            PlayerItemInHandLayer<LocalPlayer, PlayerModel<LocalPlayer>> originalRenderer,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            OpenMatrix4f[] poses,
            float bob,
            float yRot,
            float xRot,
            float partialTicks) {
        RenderEngine renderEngine = ClientEngine.getInstance().renderEngine;

        ItemStack mainHandStack = player.getMainHandItem();
        if (!mainHandStack.isEmpty()) {
            renderEngine.getItemRenderer(mainHandStack).renderItemInHand(
                    mainHandStack,
                    entityPatch,
                    InteractionHand.MAIN_HAND,
                    poses,
                    bufferSource,
                    poseStack,
                    packedLight,
                    partialTicks);
        }

        ItemStack offhandStack = player.getOffhandItem();
        if (!entityPatch.isOffhandItemValid()
                || offhandStack.isEmpty()
                || GrapplingHookEquipmentLookup.isGrapplingHookStack(offhandStack)) {
            return;
        }

        renderEngine.getItemRenderer(offhandStack).renderItemInHand(
                offhandStack,
                entityPatch,
                InteractionHand.OFF_HAND,
                poses,
                bufferSource,
                poseStack,
                packedLight,
                partialTicks);
    }
}

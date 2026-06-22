package org.com.epicawaken_grappling_hook.mixin.client;

import net.minecraft.client.Timer;
import org.com.epicawaken_grappling_hook.client.ClientSlowMotionDebugControls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Timer.class)
public class TimerMixin {
    @Shadow
    private float msPerTick;

    @Shadow
    private long lastMs;

    @Shadow
    public float partialTick;

    @Shadow
    public float tickDelta;

    @Inject(method = "advanceTime", at = @At("HEAD"), cancellable = true)
    private void epicawaken_grappling_hook$advanceTime(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        if (!ClientSlowMotionDebugControls.isSlowed()) {
            return;
        }

        this.tickDelta = (float) (timeMillis - this.lastMs) / this.msPerTick * ClientSlowMotionDebugControls.speedMultiplier();
        this.lastMs = timeMillis;
        this.partialTick += this.tickDelta;
        int ticks = (int) this.partialTick;
        this.partialTick -= ticks;
        cir.setReturnValue(ticks);
    }
}

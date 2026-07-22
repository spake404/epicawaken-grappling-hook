package org.com.epicawaken_grappling_hook.item;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.com.epicawaken_grappling_hook.client.PhantomGrapplingHookItemRenderer;

public class PhantomGrapplingHookCurioItem extends GrapplingHookCurioItem {
    public PhantomGrapplingHookCurioItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new PhantomGrapplingHookItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}

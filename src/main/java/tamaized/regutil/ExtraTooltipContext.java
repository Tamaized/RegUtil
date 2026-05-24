package tamaized.regutil;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public record ExtraTooltipContext(ItemStack stack, @Nullable Level worldIn, Consumer<Component> tooltip, TooltipFlag flagIn) {
	public static final Consumer<ExtraTooltipContext> EMPTY = _ -> {};
}

package tamaized.regutil.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import tamaized.beanification.Autowired;
import tamaized.regutil.RegUtilModIdProvider;
import tamaized.regutil.ExtraTooltipContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

@tamaized.beanification.Component
public class BreakableHelper {

	@Autowired
	private RegUtilModIdProvider modIdProvider;

	public boolean isBroken(ItemStack stack) {
		return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
	}

	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag, Consumer<ExtraTooltipContext> tooltipConsumer) {
		if (isBroken(stack))
			builder.accept(Component.translatable(modIdProvider.getModId().orElseThrow() + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
		tooltipConsumer.accept(new ExtraTooltipContext(stack, context.level(), builder, tooltipFlag));
	}

	public int damageItem(ItemStack stack, int amount, Consumer<Item> onBroken) {
		int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
		if (amount >= remaining)
			onBroken.accept(stack.getItem());
		return Math.min(remaining, amount);
	}

	public float getDestroySpeed(ItemStack stack, Supplier<Float> superCall) {
		return isBroken(stack) ? 0 : superCall.get();
	}

	public void hurtEnemy(ItemStack stack, Runnable superCall) {
		if(isBroken(stack))
			return;
		superCall.run();
	}

	public InteractionResult use(Player playerIn, InteractionHand handIn, Supplier<InteractionResult> superCall) {
		final ItemStack stack = playerIn.getItemInHand(handIn);
		return isBroken(stack) ? InteractionResult.FAIL : superCall.get();
	}

	public InteractionResult useOn(UseOnContext context, Supplier<InteractionResult> superCall) {
		return isBroken(context.getItemInHand()) ? InteractionResult.FAIL : superCall.get();
	}

}

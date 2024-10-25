package tamaized.regutil.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import tamaized.regutil.RegUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BreakableHelper {

	public static boolean isBroken(ItemStack stack) {
		return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
	}

	static void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag, Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer) {
		if (isBroken(stack))
			tooltipComponents.add(Component.translatable(RegUtil.getModID() + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
		tooltipConsumer.accept(new RegUtil.ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
	}

	static int damageItem(ItemStack stack, int amount, Consumer<Item> onBroken) {
		int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
		if (amount >= remaining)
			onBroken.accept(stack.getItem());
		return Math.min(remaining, amount);
	}

	static float getDestroySpeed(ItemStack stack, Supplier<Float> superCall) {
		return isBroken(stack) ? 0 : superCall.get();
	}

	static boolean hurtEnemy(ItemStack stack, Supplier<Boolean> superCall) {
		return !isBroken(stack) && superCall.get();
	}

	static InteractionResultHolder<ItemStack> use(Player playerIn, InteractionHand handIn, Supplier<InteractionResultHolder<ItemStack>> superCall) {
		final ItemStack stack = playerIn.getItemInHand(handIn);
		return isBroken(stack) ? InteractionResultHolder.fail(stack) : superCall.get();
	}

	static InteractionResult useOn(UseOnContext context, Supplier<InteractionResult> superCall) {
		return isBroken(context.getItemInHand()) ? InteractionResult.FAIL : superCall.get();
	}

}

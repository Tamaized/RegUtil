package tamaized.regutil.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tamaized.regutil.RegUtil;

import java.util.function.Consumer;

public class BreakableShield extends ShieldItem {

	private final Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer;

	public BreakableShield(Properties properties, Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer) {
		super(properties);
		this.tooltipConsumer = tooltipConsumer;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
		BreakableHelper.appendHoverText(stack, context, display, builder, tooltipFlag, tooltipConsumer);
		super.appendHoverText(stack, context, display, builder, tooltipFlag);
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
		return BreakableHelper.damageItem(stack, Math.min(amount, 6), item -> {
			onBroken.accept(item);
			if (entity != null)
				entity.stopUsingItem();
		});
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return BreakableHelper.getDestroySpeed(stack, () -> super.getDestroySpeed(stack, state));
	}

	@Override
	public void postHurtEnemy(ItemStack itemStack, LivingEntity mob, LivingEntity attacker) {
		super.postHurtEnemy(itemStack, mob, attacker);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		BreakableHelper.hurtEnemy(stack, () -> super.hurtEnemy(stack, target, attacker));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return BreakableHelper.use(player, hand, () -> super.use(level, player, hand));
	}

}

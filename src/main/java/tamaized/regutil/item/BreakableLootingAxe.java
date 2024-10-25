package tamaized.regutil.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import tamaized.regutil.RegUtil;

import java.util.List;
import java.util.function.Consumer;

public class BreakableLootingAxe extends LootingAxe {

	private final Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer;

	public BreakableLootingAxe(Tier tier, Properties properties, Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer) {
		super(tier, properties.attributes(AxeItem.createAttributes(tier, 5F, -3.0F)));
		this.tooltipConsumer = tooltipConsumer;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		BreakableHelper.appendHoverText(stack, context, tooltipComponents, tooltipFlag, tooltipConsumer);
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
		return BreakableHelper.damageItem(stack, amount, onBroken);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return BreakableHelper.getDestroySpeed(stack, () -> super.getDestroySpeed(stack, state));
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		return BreakableHelper.hurtEnemy(stack, () -> super.hurtEnemy(stack, target, attacker));
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return BreakableHelper.useOn(context, () -> super.useOn(context));
	}

}

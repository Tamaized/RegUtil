package tamaized.regutil.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tamaized.regutil.RegUtil;

import java.util.List;
import java.util.function.Consumer;

public class BreakableShield extends ShieldItem {

	private final Tier tier;
	private final Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer;

	public BreakableShield(Tier tier, Properties properties, Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer) {
		super(properties.durability(tier.getUses()));
		this.tier = tier;
		this.tooltipConsumer = tooltipConsumer;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		BreakableHelper.appendHoverText(stack, context, tooltipComponents, tooltipFlag, tooltipConsumer);
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
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
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		return BreakableHelper.use(playerIn, handIn, () -> super.use(worldIn, playerIn, handIn));
	}

	@Override
	public int getEnchantmentValue(ItemStack stack) {
		return tier.getEnchantmentValue();
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
		return tier.getRepairIngredient().test(repair);
	}

}

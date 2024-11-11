package tamaized.regutil.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tamaized.regutil.ArmorData;
import tamaized.regutil.RegUtil;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class BreakableArmor extends ArmorItem {

	private final ArmorData data;
	private final BiPredicate<ItemStack, Boolean> elytra;
	private final Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer;

	public BreakableArmor(ArmorData data, BiPredicate<ItemStack, Boolean> elytra, Type type, Properties properties, Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer) {
		super(data.material(), type, properties);
		this.data = data;
		this.elytra = elytra;
		this.tooltipConsumer = tooltipConsumer;
	}

	@Override
	public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
		boolean flag = !BreakableHelper.isBroken(stack) && (elytra.test(stack, true) || super.elytraFlightTick(stack, entity, flightTicks));
		if (flag && !entity.level().isClientSide && (flightTicks + 1) % 20 == 0) {
			stack.hurtAndBreak(1, entity, EquipmentSlot.CHEST);
		}
		return flag;
	}

	@Override
	public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
		return !BreakableHelper.isBroken(stack) && (elytra.test(stack, false) || super.canElytraFly(stack, entity));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		if (elytra.test(stack, false))
			tooltipComponents.add(Component.translatable(RegUtil.getModID() + ".tooltip.elytra").withStyle(ChatFormatting.DARK_AQUA));
		BreakableHelper.appendHoverText(stack, context, tooltipComponents, tooltipFlag, tooltipConsumer);
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
		return BreakableHelper.damageItem(stack, amount, onBroken);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);
		if (RegUtil.isSlotAnArmorSlot(slotId) && BreakableHelper.isBroken(stack)) {
			if (!(entity instanceof Player player) || !player.addItem(stack))
				Containers.dropItemStack(level, entity.position().x(), entity.position().y(), entity.position().z(), stack);
			else
				stack.shrink(1);
		}
	}

	@Override
	public @org.jetbrains.annotations.Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		return data.model().getArmorTexture(stack, entity, slot, innerModel).orElseGet(() -> super.getArmorTexture(stack, entity, slot, layer, innerModel));
	}

}

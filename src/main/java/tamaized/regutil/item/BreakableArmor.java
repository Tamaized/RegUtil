package tamaized.regutil.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import org.jspecify.annotations.Nullable;
import tamaized.regutil.ArmorData;
import tamaized.regutil.RegUtil;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class BreakableArmor extends Item {

	private final BiPredicate<ItemStack, Boolean> elytra;
	private final Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer;

	public BreakableArmor(ArmorData data, BiPredicate<ItemStack, Boolean> elytra, ArmorType type, Properties properties, Consumer<RegUtil.ToolAndArmorHelper.TooltipContext> tooltipConsumer) {
		super(properties.humanoidArmor(data.material().value(), type).durability(type.getDurability(data.durabilityFactor())));
		this.elytra = elytra;
		this.tooltipConsumer = tooltipConsumer;
	}

	private boolean canElytraFly(ItemStack stack) {
		return !BreakableHelper.isBroken(stack) && (elytra.test(stack, false));
	}

	@Override
	public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		super.inventoryTick(itemStack, level, owner, slot);

		final boolean glider = canElytraFly(itemStack);
		if (glider && !itemStack.has(DataComponents.GLIDER))
			itemStack.set(DataComponents.GLIDER, Unit.INSTANCE);
		else if (!glider && itemStack.has(DataComponents.GLIDER))
			itemStack.remove(DataComponents.GLIDER);

		if (slot != null && BreakableHelper.isBroken(itemStack)) {
			if (!(owner instanceof Player player) || !player.addItem(itemStack))
				Containers.dropItemStack(level, owner.position().x(), owner.position().y(), owner.position().z(), itemStack);
			else
				itemStack.shrink(1);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
		if (elytra.test(itemStack, false))
			builder.accept(Component.translatable(RegUtil.getModID() + ".tooltip.elytra").withStyle(ChatFormatting.DARK_AQUA));
		BreakableHelper.appendHoverText(itemStack, context, display, builder, tooltipFlag, tooltipConsumer);
		super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
		return BreakableHelper.damageItem(stack, amount, onBroken);
	}

}

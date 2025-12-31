package tamaized.regutil;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public record AttributeData(Predicate<ItemStack> test, Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {

	public static AttributeData make(Holder<Attribute> attribute, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
		return make(stack -> true, attribute, op, value, slot);
	}

	public static AttributeData make(Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
		return make(stack -> true, attribute, id, op, value, slot);
	}

	public static AttributeData make(Predicate<ItemStack> test, Holder<Attribute> attribute, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
		return make(test, attribute, attribute.unwrapKey().orElseThrow().identifier().getPath(), op, value, slot);
	}

	public static AttributeData make(Predicate<ItemStack> test, Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
		return new AttributeData(test, attribute, id, op, value, slot);
	}

}

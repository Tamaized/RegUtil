package tamaized.regutil;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import tamaized.pkginfoutil.PublicApi;

import java.util.function.Predicate;

@PublicApi
public record AttributeData(Predicate<ItemStack> test, Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {

	@PublicApi
	public static AttributeData make(Holder<Attribute> attribute, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
		return make(_ -> true, attribute, op, value, slot);
	}

	@PublicApi
	public static AttributeData make(Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
		return make(_ -> true, attribute, id, op, value, slot);
	}

	@PublicApi
	public static AttributeData make(Predicate<ItemStack> test, Holder<Attribute> attribute, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
		return make(test, attribute, attribute.unwrapKey().orElseThrow().identifier().getPath(), op, value, slot);
	}

	@PublicApi
	public static AttributeData make(Predicate<ItemStack> test, Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
		return new AttributeData(test, attribute, id, op, value, slot);
	}

}

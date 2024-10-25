package tamaized.regutil;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

public interface AttributeFactory extends Function<ItemStack, Stream<ItemAttributeModifiers.Entry>> {

	static AttributeFactory make(AttributeData... data) {
		return stack -> Arrays.stream(data)
			.filter(a -> a.test().test(stack))
			.map(d -> new ItemAttributeModifiers.Entry(d.attribute(), new AttributeModifier(ResourceLocation.fromNamespaceAndPath(RegUtil.getModID(), d.id()), d.value(), d.op()), d.slot()));
	}

}

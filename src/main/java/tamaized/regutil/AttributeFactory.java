package tamaized.regutil;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface AttributeFactory extends Function<ItemStack, Stream<ItemAttributeModifiers.Entry>> {

	@SafeVarargs
	@SuppressWarnings({"UseBulkOperation", "ManualArrayToCollectionCopy"})
	static AttributeFactory make(Supplier<AttributeData>... data) {
		List<Supplier<AttributeData>> list = new ArrayList<>();
		for (Supplier<AttributeData> o : data)
			list.add(o);
		return stack -> list.stream()
			.map(Supplier::get)
			.filter(a -> a.test().test(stack))
			.map(d -> new ItemAttributeModifiers.Entry(d.attribute(), new AttributeModifier(ResourceLocation.fromNamespaceAndPath(RegUtil.getModID(), d.id() + "_" + d.slot().getSerializedName()), d.value(), d.op()), d.slot()));
	}

}

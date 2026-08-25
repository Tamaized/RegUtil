package tamaized.regutil;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import tamaized.beanification.Autowired;
import tamaized.beanification.Component;
import tamaized.pkginfoutil.PublicApi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
public class AttributeFactoryProvider {

	private final RegUtilModIdProvider modIdProvider;

	public AttributeFactoryProvider(@Autowired RegUtilModIdProvider modIdProvider) {
		this.modIdProvider = modIdProvider;
	}

	@PublicApi
	@SafeVarargs
	@SuppressWarnings({"UseBulkOperation", "ManualArrayToCollectionCopy"})
	public final AttributeFactory make(Supplier<AttributeData>... data) {
		List<Supplier<AttributeData>> list = new ArrayList<>();
		for (Supplier<AttributeData> o : data)
			list.add(o);
		return stack -> list.stream()
			.map(Supplier::get)
			.filter(a -> a.test().test(stack))
			.map(d -> new ItemAttributeModifiers.Entry(
				d.attribute(),
				new AttributeModifier(
					Identifier.fromNamespaceAndPath(modIdProvider.getModId().orElseThrow(), d.id() + "_" + d.slot().getSerializedName()),
					d.value(),
					d.op()
				), d.slot()
			));
	}

}

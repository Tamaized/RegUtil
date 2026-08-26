package tamaized.regutil;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.commons.lang3.tuple.Pair;
import tamaized.beanification.Autowired;
import tamaized.beanification.Component;
import tamaized.beanification.PostConstruct;
import tamaized.regutil.item.BreakableHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
public class GearItemHandler {

	@Autowired
	private BreakableHelper breakableHelper;

	private final List<Pair<DeferredHolder<Item, Item>, AttributeFactory>> gearItems = new ArrayList<>();
	private final List<Pair<DeferredHolder<Item, Item>, Supplier<ArmorData>>> armorItems = new ArrayList<>();

	@PostConstruct
	private void setup(IEventBus modBus, IEventBus gameBus) {
		gameBus.addListener(ItemAttributeModifierEvent.class, event -> gearItems.stream()
			.filter(p -> event.getItemStack().is(p.getKey().get()) && !breakableHelper.isBroken(event.getItemStack()))
			.forEach(p -> p.getValue().apply(event.getItemStack())
				.forEach(e -> event.addModifier(e.attribute(), e.modifier(), e.slot())))
		);

		modBus.addListener(RegisterClientExtensionsEvent.class, event -> armorItems.forEach(p -> {
			@Nullable IClientItemExtensions clientExtensions = p.getValue().get().clientExtensions();
			if (clientExtensions != null)
				event.registerItem(clientExtensions, p.getKey());
		}));
	}

	public void addGearFactory(DeferredHolder<Item, Item> item, AttributeFactory factory) {
		gearItems.add(Pair.of(item, factory));
	}

	public void addArmorFactory(DeferredHolder<Item, Item> item, Supplier<ArmorData> factory) {
		armorItems.add(Pair.of(item, factory));
	}

}

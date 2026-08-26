package tamaized.regutil;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.commons.lang3.tuple.Pair;
import tamaized.beanification.Autowired;
import tamaized.beanification.Component;
import tamaized.beanification.PostConstruct;
import tamaized.regutil.item.BreakableHelper;

import java.util.ArrayList;
import java.util.List;

@Component
public class GearItemHandler {

	@Autowired
	private BreakableHelper breakableHelper;

	private final List<Pair<DeferredHolder<Item, Item>, AttributeFactory>> gearItems = new ArrayList<>();

	@PostConstruct(PostConstruct.Bus.GAME)
	private void setup(IEventBus gameBus) {
		gameBus.addListener(ItemAttributeModifierEvent.class, event -> gearItems.stream()
			.filter(p -> event.getItemStack().is(p.getKey().get()) && !breakableHelper.isBroken(event.getItemStack()))
			.forEach(p -> p.getValue().apply(event.getItemStack())
				.forEach(e -> event.addModifier(e.attribute(), e.modifier(), e.slot())))
		);
	}

	public void addGearFactory(DeferredHolder<Item, Item> item, AttributeFactory factory) {
		gearItems.add(Pair.of(item, factory));
	}

}

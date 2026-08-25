package tamaized.regutil;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import tamaized.beanification.Autowired;
import tamaized.beanification.Component;
import tamaized.pkginfoutil.PublicApi;
import tamaized.regutil.item.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class ToolAndArmorHelper {

	private final Registers registers;

	private final GearItemHandler gearItemHandler;

	public ToolAndArmorHelper(@Autowired Registers registers, @Autowired GearItemHandler gearItemHandler) {
		this.registers = registers;
		this.gearItemHandler = gearItemHandler;
	}

	public enum BowType {
		BOW,
		CROSSBOW;

		@PublicApi
		public static Optional<BowType> getTypeFromItem(Item item) {
			if (item == Items.BOW)
				return Optional.of(BOW);
			else if (item == Items.CROSSBOW)
				return Optional.of(CROSSBOW);
			else
				return Optional.empty();
		}
	}

	private final EnumMap<BowType, List<DeferredHolder<Item, Item>>> registeredBows = new EnumMap<>(BowType.class) {{
		put(BowType.BOW, new ArrayList<>());
		put(BowType.CROSSBOW, new ArrayList<>());
	}};

	@PublicApi
	public boolean isMyBow(ItemStack stack, Item check) {
		return BowType.getTypeFromItem(check)
			.map(registeredBows::get)
			.map(list -> {
				for (DeferredHolder<Item, Item> o : list) {
					if (o.isBound() && stack.is(o.get()))
						return true;
				}
				return false;
			}).orElse(false);
	}

	@PublicApi
	public DeferredHolder<Item, Item> sword(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("sword", baseName, factory, () -> new BreakableTool(properties.get().sword(tier.get(), 3, -2.4F), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> shield(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("shield", baseName, factory, () -> new BreakableShield(properties.get().durability(tier.get().durability()), tooltipConsumer));
	}

	private DeferredHolder<Item, Item> registerBow(Item item, DeferredHolder<Item, Item> o) {
		BowType.getTypeFromItem(item).map(registeredBows::get).ifPresent(list -> list.add(o));
		return o;
	}

	@PublicApi
	public DeferredHolder<Item, Item> bow(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return registerBow(Items.BOW, gear("bow", baseName, factory, () -> new BreakableBow(properties.get().durability(tier.get().durability()), tooltipConsumer)));
	}

	@PublicApi
	public DeferredHolder<Item, Item> xbow(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return registerBow(Items.CROSSBOW, gear("xbow", baseName, factory, () -> new BreakableCrossbow(properties.get().durability(tier.get().durability()), tooltipConsumer)));
	}

	@PublicApi
	public DeferredHolder<Item, Item> axe(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("axe", baseName, factory, () -> new BreakableLootingAxe(tier.get(), properties.get(), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> pickaxe(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("pickaxe", baseName, factory, () -> new BreakableTool(properties.get().pickaxe(tier.get(), 1, -2.8F), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> shovel(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("shovel", baseName, factory, () -> new BreakableShovel(tier.get(), properties.get(), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> hoe(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("hoe", baseName, factory, () -> new BreakableHoe(tier.get(), properties.get(), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> helmet(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("helmet", baseName, factory, data, () -> new BreakableArmor(data.get(), (stack, tick) -> false, ArmorType.HELMET, properties.get(), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> chest(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("chest", baseName, factory, data, () -> new BreakableArmor(data.get(), (stack, tick) -> false, ArmorType.CHESTPLATE, properties.get(), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> chest(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, BiPredicate<ItemStack, Boolean> elytra, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("chest", baseName, factory, data, () -> new BreakableArmor(data.get(), elytra, ArmorType.CHESTPLATE, properties.get(), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> legs(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("legs", baseName, factory, data, () -> new BreakableArmor(data.get(), (stack, tick) -> false, ArmorType.LEGGINGS, properties.get(), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> boots(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<ExtraTooltipContext> tooltipConsumer) {
		return gear("boots", baseName, factory, data, () -> new BreakableArmor(data.get(), (stack, tick) -> false, ArmorType.BOOTS, properties.get(), tooltipConsumer));
	}

	@PublicApi
	public DeferredHolder<Item, Item> gear(String type, String baseName, AttributeFactory factory, Supplier<Item> itemInit) {
		return gear(type, baseName, factory, null, itemInit);
	}

	@PublicApi
	public DeferredHolder<Item, Item> gear(String type, String baseName, AttributeFactory factory, @Nullable Supplier<ArmorData> armorData, Supplier<Item> itemInit) {
		DeferredHolder<Item, Item> object = registers.create(Registries.ITEM).register(baseName.concat("_").concat(type), itemInit);
		gearItemHandler.addGearFactory(object, factory);
		if (armorData != null)
			gearItemHandler.addArmorFactory(object, armorData);
		return object;
	}

}

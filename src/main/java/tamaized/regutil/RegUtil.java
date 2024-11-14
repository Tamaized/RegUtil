package tamaized.regutil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import tamaized.regutil.item.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.*;

@SuppressWarnings({"unused", "DuplicatedCode", "UnusedReturnValue"})
public class RegUtil {

	private static String MODID = null;
	private static String BROKEN_STATE_NAME;

	private static final Map<ResourceKey<?>, DeferredRegister<?>> REGISTERS = new HashMap<>();
	private static final Map<Item, List<DeferredHolder<Item, Item>>> BOWS = new HashMap<>() {{
		put(Items.BOW, new ArrayList<>());
		put(Items.CROSSBOW, new ArrayList<>());
	}};
	private static final List<Pair<DeferredHolder<Item, Item>, AttributeFactory>> GEAR_ITEMS = new ArrayList<>();
	private static final List<Pair<DeferredHolder<Item, Item>, ArmorData>> ARMOR_ITEMS = new ArrayList<>();
	public static boolean renderingArmorOverlay = false;

	public static String getModID() {
		return MODID;
	}

	public static boolean isMyBow(ItemStack stack, Item check) {
		List<DeferredHolder<Item, Item>> list = BOWS.get(check);
		if (list == null)
			return false;
		for (DeferredHolder<Item, Item> o : list) {
			if (o.isBound() && stack.is(o.get()))
				return true;
		}
		return false;
	}

	public static boolean isArmorOverlay(ItemStack stack) {
		return ARMOR_ITEMS.stream().anyMatch(o -> o.getValue().model().hasOverlay() && o.getKey().isBound() && stack.is(o.getKey().get()));
	}

	public static boolean isSlotAnArmorSlot(int slot) {
		return slot >= Inventory.INVENTORY_SIZE && slot < Inventory.INVENTORY_SIZE + Inventory.ALL_ARMOR_SLOTS.length;
	}

	private static void initModID() {
		if (MODID == null)
			MODID = ModLoadingContext.get().getActiveNamespace();
	}

	@SafeVarargs
	public static void setup(Supplier<RegistryClass>... inits) {
		initModID();
		@NotNull IEventBus bus = Objects.requireNonNull(ModLoadingContext.get().getActiveContainer().getEventBus());
		RegUtil.BROKEN_STATE_NAME = ResourceLocation.fromNamespaceAndPath(MODID, "broken_state_attributes").toString();
		create(Registries.ITEM); // Pre-Bake the Item DeferredRegister for ToolAndArmorHelper
		for (Supplier<RegistryClass> init : inits)
			init.get().init(bus);
		// Looks like smithing templates merge data now instead of overwrite, TODO: double check this behavior
		/*class FixedUpgradeRecipe extends SmithingTransformRecipe {
			final Ingredient template;
			final Ingredient base;
			final Ingredient addition;
			final ItemStack result;
			public FixedUpgradeRecipe(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, ItemStack pResult) {
				super(pTemplate, pBase, pAddition, pResult);
				template = pTemplate;
				base = pBase;
				addition = pAddition;
				result = pResult;
			}

			@Override
			public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
				ItemStack itemstack = input.base().transmuteCopy(this.result.getItem(), this.result.getCount());
				itemstack.applyComponents(this.result.getComponentsPatch());
				return itemstack;

				CompoundTag compoundtag = pContainer.getItem(1).getTag();
				if (compoundtag != null)
					itemstack.getOrCreateTag().merge(compoundtag.copy());
				return itemstack;
			}
		}
		create(Registries.RECIPE_SERIALIZER).register("smithing", () -> new SmithingTransformRecipe.Serializer() {
			private static final Codec<SmithingTransformRecipe> CODEC = RecordCodecBuilder.create(
					p_301062_ -> p_301062_.group(
									Ingredient.CODEC.fieldOf("template").forGetter(p_301310_ -> ((FixedUpgradeRecipe) p_301310_).template),
									Ingredient.CODEC.fieldOf("base").forGetter(p_300938_ -> ((FixedUpgradeRecipe) p_300938_).base),
									Ingredient.CODEC.fieldOf("addition").forGetter(p_301153_ -> ((FixedUpgradeRecipe) p_301153_).addition),
									ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter(p_300935_ -> ((FixedUpgradeRecipe) p_300935_).result)
							)
							.apply(p_301062_, (template, base, addition, result) -> (SmithingTransformRecipe) new FixedUpgradeRecipe(template, base, addition, result))
			);

			@Override
			public Codec<SmithingTransformRecipe> codec() {
				return CODEC;
			}

			@Override
			public SmithingTransformRecipe fromNetwork(FriendlyByteBuf p_267316_) {
				Ingredient ingredient = Ingredient.fromNetwork(p_267316_);
				Ingredient ingredient1 = Ingredient.fromNetwork(p_267316_);
				Ingredient ingredient2 = Ingredient.fromNetwork(p_267316_);
				ItemStack itemstack = p_267316_.readItem();
				return new FixedUpgradeRecipe(ingredient, ingredient1, ingredient2, itemstack);
			}

		});*/
		for (DeferredRegister<?> register : REGISTERS.values())
			register.register(bus);

		NeoForge.EVENT_BUS.addListener(ItemAttributeModifierEvent.class, event -> GEAR_ITEMS.stream()
			.filter(p -> event.getItemStack().is(p.getKey().get()) && !BreakableHelper.isBroken(event.getItemStack()))
			.forEach(p -> p.getValue().apply(event.getItemStack())
				.forEach(e -> event.addModifier(e.attribute(), e.modifier(), e.slot())))
		);

		bus.addListener(RegisterClientExtensionsEvent.class, event -> ARMOR_ITEMS.forEach(p -> event.registerItem(new IClientItemExtensions() {
			@Override
			public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
				HumanoidModel<?> model = p.getValue().model().getArmorModel(entityLiving, itemStack, armorSlot, _default);
				if (model != null)
					return model;
				if (!p.getValue().model().isFullbright() && !p.getValue().model().hasOverlay())
					return IClientItemExtensions.super.getHumanoidArmorModel(entityLiving, itemStack, armorSlot, _default);
				ModelLayerLocation layer = armorSlot == ArmorItem.Type.LEGGINGS.getSlot() ? ModelLayers.PLAYER_INNER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR;
				return new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(layer)) {
					@Override
					public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
						final boolean fullbright = p.getValue().model().isFullbright() || (p.getValue().model().isOverlayFullbright() && RegUtil.renderingArmorOverlay);
						super.renderToBuffer(poseStack, buffer, fullbright ? 0xF000F0 : packedLight, packedOverlay, color);
					}
				};
			}
		}, p.getKey())));
	}

	@SuppressWarnings("unchecked")
	public static <R> DeferredRegister<R> create(ResourceKey<Registry<R>> type) {
		initModID();
		DeferredRegister<?> value = REGISTERS.get(type);
		if (value != null)
			return (DeferredRegister<R>) value;
		DeferredRegister<R> def = DeferredRegister.create(type, RegUtil.MODID);
		REGISTERS.put(type, def);
		if (type.equals(Registries.ITEM))
			ToolAndArmorHelper.REGISTRY = (DeferredRegister<Item>) def;
		return def;
	}

	public static class ToolAndArmorHelper {

		private static DeferredRegister<Item> REGISTRY;

		public record TooltipContext(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
			public static final Consumer<TooltipContext> EMPTY = context -> {
			};
		}

		public static DeferredHolder<Item, Item> sword(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(
				factory,
				REGISTRY.register(
					tier.name().toLowerCase(Locale.US).concat("_sword"),
					() -> new BreakableSword(tier, properties, tooltipConsumer)
				)
			);
		}

		public static DeferredHolder<Item, Item> shield(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(
				factory,
				REGISTRY.register(
					tier.name().toLowerCase(Locale.US).concat("_shield"),
					() -> new BreakableShield(tier, properties, tooltipConsumer)
				)
			);
		}

		private static DeferredHolder<Item, Item> registerBow(Item item, DeferredHolder<Item, Item> o) {
			if (BOWS.containsKey(item))
				BOWS.get(item).add(o);
			return o;
		}

		public static DeferredHolder<Item, Item> bow(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(
				factory,
				registerBow(
					Items.BOW,
					REGISTRY.register(
						tier.name().toLowerCase(Locale.US).concat("_bow"),
						() -> new BreakableBow(tier, properties, tooltipConsumer)
					)
				)
			);
		}

		public static DeferredHolder<Item, Item> xbow(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(
				factory,
				registerBow(
					Items.CROSSBOW,
					REGISTRY.register(
						tier.name().toLowerCase(Locale.US).concat("_xbow"),
						() -> new BreakableCrossbow(tier, properties, tooltipConsumer)
					)
				)
			);
		}

		public static DeferredHolder<Item, Item> axe(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(
				factory,
				REGISTRY.register(
					tier.name().toLowerCase(Locale.US).concat("_axe"),
					() -> new BreakableLootingAxe(tier, properties, tooltipConsumer)
				)
			);
		}

		public static DeferredHolder<Item, Item> pickaxe(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(
				factory,
				REGISTRY.register(
					tier.name().toLowerCase(Locale.US).concat("_pickaxe"),
					() -> new BreakablePickaxe(tier, properties, tooltipConsumer)
				)
			);
		}

		public static DeferredHolder<Item, Item> shovel(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(
				factory,
				REGISTRY.register(
					tier.name().toLowerCase(Locale.US).concat("_shovel"),
					() -> new BreakableShovel(tier, properties, tooltipConsumer)
				)
			);
		}

		public static DeferredHolder<Item, Item> hoe(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(
				factory,
				REGISTRY.register(
					tier.name().toLowerCase(Locale.US).concat("_hoe"),
					() -> new BreakableHoe(tier, properties, tooltipConsumer)
				)
			);
		}

		public static DeferredHolder<Item, Item> helmet(ArmorData data, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapArmorItemRegistration(data, factory, data.register(REGISTRY, "_helmet", armorFactory(data, ArmorItem.Type.HELMET, properties, factory, tooltipConsumer)));
		}

		public static DeferredHolder<Item, Item> chest(ArmorData data, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return chest(data, properties, factory, (stack, tick) -> false, tooltipConsumer);
		}

		public static DeferredHolder<Item, Item> chest(ArmorData data, Item.Properties properties, AttributeFactory factory, BiPredicate<ItemStack, Boolean> elytra, Consumer<TooltipContext> tooltipConsumer) {
			return wrapArmorItemRegistration(data, factory, data.register(REGISTRY, "_chest", armorFactory(data, ArmorItem.Type.CHESTPLATE, properties, factory, elytra, tooltipConsumer)));
		}

		public static DeferredHolder<Item, Item> legs(ArmorData data, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapArmorItemRegistration(data, factory, data.register(REGISTRY, "_legs", armorFactory(data, ArmorItem.Type.LEGGINGS, properties, factory, tooltipConsumer)));
		}

		public static DeferredHolder<Item, Item> boots(ArmorData data, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapArmorItemRegistration(data, factory, data.register(REGISTRY, "_boots", armorFactory(data, ArmorItem.Type.BOOTS, properties, factory, tooltipConsumer)));
		}

		private static DeferredHolder<Item, Item> wrapGearItemRegistration(AttributeFactory data, DeferredHolder<Item, Item> object) {
			GEAR_ITEMS.add(Pair.of(object, data));
			return object;
		}

		private static DeferredHolder<Item, Item> wrapArmorItemRegistration(ArmorData data, AttributeFactory factory, DeferredHolder<Item, Item> object) {
			ARMOR_ITEMS.add(Pair.of(object, data));
			return wrapGearItemRegistration(factory, object);
		}

		private static Supplier<ArmorItem> armorFactory(ArmorData data, ArmorItem.Type slot, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return armorFactory(data, slot, properties, factory, (stack, tick) -> false, tooltipConsumer);
		}

		private static Supplier<ArmorItem> armorFactory(ArmorData data, ArmorItem.Type slot, Item.Properties properties, AttributeFactory factory, BiPredicate<ItemStack, Boolean> elytra, Consumer<TooltipContext> tooltipConsumer) {
			return () -> new BreakableArmor(data, elytra, slot, properties, tooltipConsumer);
		}

	}

}

package tamaized.regutil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.ArmorType;
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

	@Nullable
	private static String MODID = null;
	@Nullable
	private static String BROKEN_STATE_NAME;

	private static final Map<ResourceKey<?>, DeferredRegister<?>> REGISTERS = new HashMap<>();
	private static final Map<Item, List<DeferredHolder<Item, Item>>> BOWS = new HashMap<>() {{ // TODO: use an EnumMap instead
		put(Items.BOW, new ArrayList<>());
		put(Items.CROSSBOW, new ArrayList<>());
	}};
	private static final List<Pair<DeferredHolder<Item, Item>, AttributeFactory>> GEAR_ITEMS = new ArrayList<>();
	private static final List<Pair<DeferredHolder<Item, Item>, Supplier<ArmorData>>> ARMOR_ITEMS = new ArrayList<>();
	public static boolean renderingArmorOverlay = false;

	public static String getModID() {
		if (MODID == null)
			initModID();
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
		return ARMOR_ITEMS.stream().anyMatch(o -> o.getValue().get().model().hasOverlay() && o.getKey().isBound() && stack.is(o.getKey().get()));
	}

	private static void initModID() {
		if (MODID == null)
			MODID = ModLoadingContext.get().getActiveNamespace();
	}

	@SafeVarargs
	public static void setup(Supplier<RegistryClass>... inits) {
		@NotNull IEventBus bus = Objects.requireNonNull(ModLoadingContext.get().getActiveContainer().getEventBus());
		RegUtil.BROKEN_STATE_NAME = Identifier.fromNamespaceAndPath(getModID(), "broken_state_attributes").toString();
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
			@SuppressWarnings({"rawtypes", "RedundantSuppression"})
			public Model<?> getHumanoidArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original) {
				Model<?> model = p.getValue().get().model().getArmorModel(itemStack, layerType, original);
				if (model != null)
					return model;
				if (!p.getValue().get().model().isFullbright() && !p.getValue().get().model().hasOverlay())
					return IClientItemExtensions.super.getHumanoidArmorModel(itemStack, layerType, original);
				ModelLayerLocation layer = layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS ? ModelLayers.PLAYER_ARMOR.legs() : ModelLayers.PLAYER_ARMOR.head();
				return new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(layer)) {
					@Override
					public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
						final boolean fullbright = p.getValue().get().model().isFullbright() || (p.getValue().get().model().isOverlayFullbright() && RegUtil.renderingArmorOverlay);
						super.renderToBuffer(poseStack, buffer, fullbright ? 0xF000F0 : packedLight, packedOverlay, color);
					}
				};
			}

			@Override
			@SuppressWarnings({"rawtypes", "RedundantSuppression"})
			public void setupModelAnimations(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, Model model, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
				p.getValue().get().model().setupModelAnimations(livingEntity, itemStack, equipmentSlot, model, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
			}

			@Override
			public @org.jspecify.annotations.Nullable Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier _default) {
				return p.getValue().get().model().getArmorTexture(stack, type, layer).orElse(_default);
			}
		}, p.getKey())));
	}

	@SuppressWarnings("unchecked")
	public static <R> DeferredRegister<R> create(ResourceKey<Registry<R>> type) {
		initModID();
		DeferredRegister<?> value = REGISTERS.get(type);
		if (value != null)
			return (DeferredRegister<R>) value;
		DeferredRegister<R> def = DeferredRegister.create(type, RegUtil.getModID());
		REGISTERS.put(type, def);
		if (type.equals(Registries.ITEM))
			ToolAndArmorHelper.REGISTRY = (DeferredRegister<Item>) def;
		return def;
	}

	public static class ToolAndArmorHelper {

		private static DeferredRegister<Item> REGISTRY;

		public record TooltipContext(ItemStack stack, @Nullable Level worldIn, Consumer<Component> tooltip, TooltipFlag flagIn) {
			public static final Consumer<TooltipContext> EMPTY = context -> {
			};
		}

		public static DeferredHolder<Item, Item> sword(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("sword", baseName, factory, () -> new BreakableTool(properties.get().sword(tier.get(), 3, -2.4F), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> shield(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("shield", baseName, factory, () -> new BreakableShield(properties.get().durability(tier.get().durability()), tooltipConsumer));
		}

		private static DeferredHolder<Item, Item> registerBow(Item item, DeferredHolder<Item, Item> o) {
			if (BOWS.containsKey(item))
				BOWS.get(item).add(o);
			return o;
		}

		public static DeferredHolder<Item, Item> bow(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return registerBow(Items.BOW, gear("bow", baseName, factory, () -> new BreakableBow(properties.get().durability(tier.get().durability()), tooltipConsumer)));
		}

		public static DeferredHolder<Item, Item> xbow(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return registerBow(Items.CROSSBOW, gear("xbow", baseName, factory, () -> new BreakableCrossbow(properties.get().durability(tier.get().durability()), tooltipConsumer)));
		}

		public static DeferredHolder<Item, Item> axe(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("axe", baseName, factory, () -> new BreakableLootingAxe(tier.get(), properties.get(), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> pickaxe(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("pickaxe", baseName, factory, () -> new BreakableTool(properties.get().pickaxe(tier.get(), 1, -2.8F), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> shovel(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("shovel", baseName, factory, () -> new BreakableShovel(tier.get(), properties.get(), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> hoe(String baseName, Supplier<ToolMaterial> tier, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("hoe", baseName, factory, () -> new BreakableHoe(tier.get(), properties.get(), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> helmet(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("helmet", baseName, factory, data, () -> new BreakableArmor(data.get(), (stack, tick) -> false, ArmorType.HELMET, properties.get(), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> chest(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("chest", baseName, factory, data, () -> new BreakableArmor(data.get(), (stack, tick) -> false, ArmorType.CHESTPLATE, properties.get(), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> chest(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, BiPredicate<ItemStack, Boolean> elytra, Consumer<TooltipContext> tooltipConsumer) {
			return gear("chest", baseName, factory, data, () -> new BreakableArmor(data.get(), elytra, ArmorType.CHESTPLATE, properties.get(), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> legs(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("legs", baseName, factory, data, () -> new BreakableArmor(data.get(), (stack, tick) -> false, ArmorType.LEGGINGS, properties.get(), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> boots(String baseName, Supplier<ArmorData> data, Supplier<Item.Properties> properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return gear("boots", baseName, factory, data, () -> new BreakableArmor(data.get(), (stack, tick) -> false, ArmorType.BOOTS, properties.get(), tooltipConsumer));
		}

		public static DeferredHolder<Item, Item> gear(String type, String baseName, AttributeFactory factory, Supplier<Item> itemInit) {
			return gear(type, baseName, factory, null, itemInit);
		}

		public static DeferredHolder<Item, Item> gear(String type, String baseName, AttributeFactory factory, @Nullable Supplier<ArmorData> armorData, Supplier<Item> itemInit) {
			DeferredHolder<Item, Item> object = REGISTRY.register(baseName.concat("_").concat(type), itemInit);
			GEAR_ITEMS.add(Pair.of(object, factory));
			if (armorData != null)
				ARMOR_ITEMS.add(Pair.of(object, armorData));
			return object;
		}
	}

}

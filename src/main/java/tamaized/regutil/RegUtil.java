package tamaized.regutil;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "DuplicatedCode", "deprecation", "UnusedReturnValue"})
public class RegUtil {

	private static String MODID = "regutil";
	private static String BROKEN_STATE_NAME;

	private static final List<DeferredRegister<?>> REGISTERS = new ArrayList<>();
	private static final Map<Item, List<DeferredHolder<Item, Item>>> BOWS = new HashMap<>() {{
		put(Items.BOW, new ArrayList<>());
		put(Items.CROSSBOW, new ArrayList<>());
	}};
	private static final List<Pair<DeferredHolder<Item, Item>, AttributeFactory>> GEAR_ITEMS = new ArrayList<>();
	private static final List<Pair<DeferredHolder<Item, Item>, ArmorData>> ARMOR_ITEMS = new ArrayList<>();
	public static boolean renderingArmorOverlay = false;

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
		return ARMOR_ITEMS.stream().anyMatch(o -> o.getValue().overlay && o.getKey().isBound() && stack.is(o.getKey().get()));
	}

	public static boolean isSlotAnArmorSlot(int slot) {
		return slot >= Inventory.INVENTORY_SIZE && slot < Inventory.INVENTORY_SIZE + Inventory.ALL_ARMOR_SLOTS.length;
	}

	@SafeVarargs
	public static void setup(String modid, IEventBus bus, Supplier<RegistryClass>... inits) {
		RegUtil.MODID = modid;
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
		for (DeferredRegister<?> register : REGISTERS)
			register.register(bus);

		NeoForge.EVENT_BUS.addListener(ItemAttributeModifierEvent.class, event -> GEAR_ITEMS.stream()
			.filter(p -> event.getItemStack().is(p.getKey().get()) && !ToolAndArmorHelper.isBroken(event.getItemStack()))
			.forEach(p -> p.getValue().apply(event.getItemStack())
				.forEach(e -> event.addModifier(e.attribute(), e.modifier(), e.slot())))
		);

		bus.addListener(RegisterClientExtensionsEvent.class, event -> ARMOR_ITEMS.forEach(p -> event.registerItem(new IClientItemExtensions() {
			@Override
			public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
				HumanoidModel<?> model = p.getValue().getArmorModel(entityLiving, itemStack, armorSlot, _default);
				if (model != null)
					return model;
				if (!p.getValue().fullbright && !p.getValue().overlay)
					return IClientItemExtensions.super.getHumanoidArmorModel(entityLiving, itemStack, armorSlot, _default);
				ModelLayerLocation layer = armorSlot == ArmorItem.Type.LEGGINGS.getSlot() ? ModelLayers.PLAYER_INNER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR;
				return new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(layer)) {
					@Override
					public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
						final boolean fullbright = p.getValue().fullbright || (p.getValue().overlayFullbright && RegUtil.renderingArmorOverlay);
						super.renderToBuffer(poseStack, buffer, fullbright ? 0xF000F0 : packedLight, packedOverlay, color);
					}
				};
			}
		}, p.getKey())));
	}

	@SuppressWarnings("unchecked")
	public static <R> DeferredRegister<R> create(ResourceKey<Registry<R>> type) {
		if (type.equals(Registries.ITEM) && ToolAndArmorHelper.REGISTRY != null)
			return (DeferredRegister<R>) ToolAndArmorHelper.REGISTRY;
		DeferredRegister<R> def = DeferredRegister.create(type, RegUtil.MODID);
		REGISTERS.add(def);
		if (type.equals(Registries.ITEM))
			ToolAndArmorHelper.REGISTRY = (DeferredRegister<Item>) def;
		return def;
	}

	public interface AttributeFactory extends Function<ItemStack, Stream<ItemAttributeModifiers.Entry>> {

	}

	public static AttributeFactory makeAttributeFactory(AttributeData... data) {
		return stack -> Arrays.stream(data)
			.filter(a -> a.test.test(stack))
			.map(d -> new ItemAttributeModifiers.Entry(d.attribute(), new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MODID, d.id()), d.value(), d.op()), d.slot()));
	}

	public record ItemProps(Supplier<Item.Properties> properties) {

	}

	public static class ItemTier implements Tier {
		private final String name;
		private final TagKey<Block> incorrectBlocksForDrops;
		private final int maxUses;
		private final float efficiency;
		private final float attackDamage;
		private final int enchantability;
		private final LazyLoadedValue<Ingredient> repairMaterial;

		public ItemTier(String name, TagKey<Block> incorrectBlocksForDrops, int harvestLevelIn, int maxUsesIn, float efficiencyIn, float attackDamageIn, int enchantabilityIn, Supplier<Ingredient> repairMaterialIn) {
			this.name = name;
			this.incorrectBlocksForDrops = incorrectBlocksForDrops;
			this.maxUses = maxUsesIn;
			this.efficiency = efficiencyIn;
			this.attackDamage = attackDamageIn;
			this.enchantability = enchantabilityIn;
			this.repairMaterial = new LazyLoadedValue<>(repairMaterialIn);
		}

		public String name() {
			return name;
		}

		@Override
		public int getUses() {
			return this.maxUses;
		}

		@Override
		public float getSpeed() {
			return this.efficiency;
		}

		@Override
		public float getAttackDamageBonus() {
			return this.attackDamage;
		}

		@Override
		public int getEnchantmentValue() {
			return this.enchantability;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return this.repairMaterial.get();
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return incorrectBlocksForDrops;
		}
	}

	public static class ArmorData {

		private final Holder<ArmorMaterial> material;
		private final boolean fullbright;
		private final boolean overlay;
		private final boolean overlayFullbright;

		public ArmorData(Holder<ArmorMaterial> material, boolean fullbright, boolean overlay, boolean overlayFullbright) {
			this.material = material;
			this.fullbright = fullbright;
			this.overlay = overlay;
			this.overlayFullbright = overlayFullbright;
		}

		@Nullable
		@OnlyIn(Dist.CLIENT)
		public <A extends HumanoidModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, A _default) {
			return null;
		}

		public Optional<ResourceLocation> getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, boolean inner) {
			return Optional.empty();
		}

		public DeferredHolder<Item, Item> register(DeferredRegister<Item> REGISTRY, String append, Supplier<ArmorItem> obj) {
			return REGISTRY.register(material.unwrap().orThrow().location().getPath().toLowerCase(Locale.US).concat(append), obj);
		}

	}

	public static class ToolAndArmorHelper {

		private static DeferredRegister<Item> REGISTRY;

		public record TooltipContext(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

		}

		public static boolean isBroken(ItemStack stack) {
			return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
		}

		public static DeferredHolder<Item, Item> sword(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(factory, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_sword"), () -> new SwordItem(tier, properties.attributes(SwordItem.createAttributes(tier, 3, -2.4F))) {

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(stack.getItem());
					return Math.min(remaining, amount);
				}

				@Override
				public float getDestroySpeed(ItemStack stack, BlockState state) {
					return isBroken(stack) ? 0 : super.getDestroySpeed(stack, state);
				}

				@Override
				public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
					return !isBroken(stack) && super.hurtEnemy(stack, target, attacker);
				}

			}));
		}

		public static DeferredHolder<Item, Item> shield(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(factory, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_shield"), () -> new ShieldItem(properties.durability(tier.getUses())) {

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					int dmg = Math.min(amount, 6);
					if (dmg >= remaining) {
						onBroken.accept(stack.getItem());
						if (entity != null)
							entity.stopUsingItem();
					}
					return Math.min(remaining, dmg);
				}

				@Override
				public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
					final ItemStack stack = playerIn.getItemInHand(handIn);
					return isBroken(stack) ? InteractionResultHolder.fail(stack) : super.use(worldIn, playerIn, handIn);
				}

				@Override
				public int getEnchantmentValue() {
					return tier.getEnchantmentValue();
				}

				@Override
				public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
					return tier.getRepairIngredient().test(repair);
				}

			}));
		}

		private static DeferredHolder<Item, Item> registerBow(Item item, DeferredHolder<Item, Item> o) {
			if (BOWS.containsKey(item))
				BOWS.get(item).add(o);
			return o;
		}

		public static DeferredHolder<Item, Item> bow(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(factory, registerBow(Items.BOW, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_bow"), () -> new BowItem(properties.durability(tier.getUses())) {

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(stack.getItem());
					return Math.min(remaining, amount);
				}

				@Override
				public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
					final ItemStack stack = playerIn.getItemInHand(handIn);
					return isBroken(stack) ? InteractionResultHolder.fail(stack) : super.use(worldIn, playerIn, handIn);
				}

				@Override
				public int getEnchantmentValue() {
					return tier.getEnchantmentValue();
				}

				@Override
				public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
					return tier.getRepairIngredient().test(repair) || super.isValidRepairItem(toRepair, repair);
				}

			})));
		}

		public static DeferredHolder<Item, Item> xbow(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(factory, registerBow(Items.CROSSBOW, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_xbow"), () -> new CrossbowItem(properties.durability(tier.getUses())) {

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(stack.getItem());
					return Math.min(remaining, amount);
				}

				@Override
				public boolean useOnRelease(ItemStack stack) {
					return stack.getItem() instanceof CrossbowItem;
				}

				@Override
				public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
					ItemStack itemstack = playerIn.getItemInHand(handIn);
					if (isBroken(itemstack))
						return InteractionResultHolder.fail(itemstack);
					return super.use(worldIn, playerIn, handIn);
				}

				@Override
				public int getEnchantmentValue() {
					return tier.getEnchantmentValue();
				}

				@Override
				public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
					return tier.getRepairIngredient().test(repair) || super.isValidRepairItem(toRepair, repair);
				}

			})));
		}

		public static DeferredHolder<Item, Item> axe(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(factory, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_axe"), () -> new LootingAxe(tier, properties.attributes(AxeItem.createAttributes(tier, 5F, -3.0F))) {

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(stack.getItem());
					return Math.min(remaining, amount);
				}

				@Override
				public float getDestroySpeed(ItemStack stack, BlockState state) {
					return isBroken(stack) ? 0 : super.getDestroySpeed(stack, state);
				}

				@Override
				public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
					if (!isBroken(stack)) {
						stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
						return true;
					}
					return false;
				}

				@Override
				public InteractionResult useOn(UseOnContext context) {
					return isBroken(context.getItemInHand()) ? InteractionResult.FAIL : super.useOn(context);
				}

			}));
		}

		public static DeferredHolder<Item, Item> pickaxe(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(factory, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_pickaxe"), () -> new PickaxeItem(tier, properties.attributes(PickaxeItem.createAttributes(tier, 1, -2.8F))) {

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(stack.getItem());
					return Math.min(remaining, amount);
				}

				@Override
				public float getDestroySpeed(ItemStack stack, BlockState state) {
					return isBroken(stack) ? 0 : super.getDestroySpeed(stack, state);
				}

				@Override
				public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
					return !isBroken(stack) && super.hurtEnemy(stack, target, attacker);
				}

				@Override
				public InteractionResult useOn(UseOnContext context) {
					return isBroken(context.getItemInHand()) ? InteractionResult.FAIL : super.useOn(context);
				}

			}));
		}

		public static DeferredHolder<Item, Item> shovel(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(factory, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_shovel"), () -> new ShovelItem(tier, properties.attributes(ShovelItem.createAttributes(tier, 1.5F, -3.0F))) {

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(stack.getItem());
					return Math.min(remaining, amount);
				}

				@Override
				public float getDestroySpeed(ItemStack stack, BlockState state) {
					return isBroken(stack) ? 0 : super.getDestroySpeed(stack, state);
				}

				@Override
				public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
					return !isBroken(stack) && super.hurtEnemy(stack, target, attacker);
				}

				@Override
				public InteractionResult useOn(UseOnContext context) {
					return isBroken(context.getItemInHand()) ? InteractionResult.FAIL : super.useOn(context);
				}

			}));
		}

		public static DeferredHolder<Item, Item> hoe(ItemTier tier, Item.Properties properties, AttributeFactory factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapGearItemRegistration(factory, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_hoe"), () -> new HoeItem(tier, properties.attributes(HoeItem.createAttributes(tier, -3, 0.0F))) {

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(stack.getItem());
					return Math.min(remaining, amount);
				}

				@Override
				public float getDestroySpeed(ItemStack stack, BlockState state) {
					return isBroken(stack) ? 0 : super.getDestroySpeed(stack, state);
				}

				@Override
				public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
					return !isBroken(stack) && super.hurtEnemy(stack, target, attacker);
				}

				@Override
				public InteractionResult useOn(UseOnContext context) {
					return isBroken(context.getItemInHand()) ? InteractionResult.FAIL : super.useOn(context);
				}

			}));
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
			return () -> new ArmorItem(data.material, slot, properties) {

				@Override
				public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
					boolean flag = !isBroken(stack) && (elytra.test(stack, true) || super.elytraFlightTick(stack, entity, flightTicks));
					if (flag && !entity.level().isClientSide && (flightTicks + 1) % 20 == 0) {
						stack.hurtAndBreak(1, entity, EquipmentSlot.CHEST);
					}
					return flag;
				}

				@Override
				public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
					return !isBroken(stack) && (elytra.test(stack, false) || super.canElytraFly(stack, entity));
				}

				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
					if (isBroken(stack))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					if (elytra.test(stack, false))
						tooltipComponents.add(Component.translatable(RegUtil.MODID + ".tooltip.elytra").withStyle(ChatFormatting.DARK_AQUA));
					tooltipConsumer.accept(new ToolAndArmorHelper.TooltipContext(stack, context.level(), tooltipComponents, tooltipFlag));
					super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(stack.getItem());
					return Math.min(remaining, amount);
				}

				@Override
				public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
					super.inventoryTick(stack, level, entity, slotId, isSelected);
					if (RegUtil.isSlotAnArmorSlot(slotId) && isBroken(stack)) {
						if (!(entity instanceof Player player) || !player.addItem(stack))
							Containers.dropItemStack(level, entity.position().x(), entity.position().y(), entity.position().z(), stack);
						else
							stack.shrink(1);
					}
				}

				@Override
				public @org.jetbrains.annotations.Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
					return data.getArmorTexture(stack, entity, slot, innerModel).orElseGet(() -> super.getArmorTexture(stack, entity, slot, layer, innerModel));
				}
			};
		}

		public static abstract class LootingAxe extends AxeItem {

			private static final Set<ItemAbility> ACTIONS = Stream.concat(
				ItemAbilities.DEFAULT_AXE_ACTIONS.stream(),
				Stream.of(ItemAbilities.SWORD_SWEEP)
			).collect(Collectors.toCollection(Sets::newIdentityHashSet));

			public LootingAxe(Tier tier, Properties properties) {
				super(tier, properties);
			}

			@Override
			public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
				HolderLookup.RegistryLookup<Enchantment> registry = CommonHooks.resolveLookup(Registries.ENCHANTMENT);
				if (registry == null)
					return super.canPerformAction(stack, itemAbility);
				return registry.get(Enchantments.SWEEPING_EDGE)
					.filter(value -> EnchantmentHelper.getItemEnchantmentLevel(value, stack) > 0)
					.map(value -> ACTIONS.contains(itemAbility))
					.orElseGet(() -> super.canPerformAction(stack, itemAbility));
			}
		}

	}

	public record AttributeData(Predicate<ItemStack> test, Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {

		public static AttributeData make(Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
			return make(stack -> true, attribute, id, op, value, slot);
		}

		public static AttributeData make(Predicate<ItemStack> test, Holder<Attribute> attribute, String id, AttributeModifier.Operation op, double value, EquipmentSlotGroup slot) {
			return new AttributeData(test, attribute, id, op, value, slot);
		}

	}

}

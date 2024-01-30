package tamaized.regutil;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.ToolAction;
import net.neoforged.neoforge.common.ToolActions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "DuplicatedCode", "deprecation"})
public class RegUtil {

	private static String MODID = "regutil";

	private static final UUID[] ARMOR_MODIFIER_UUID_PER_SLOT = new UUID[]{UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"), UUID.fromString("86fda400-8542-4d95-b275-c6393de5b887")};
	private static final List<DeferredRegister<?>> REGISTERS = new ArrayList<>();
	private static final Map<Item, List<DeferredHolder<Item, Item>>> BOWS = new HashMap<>() {{
		put(Items.BOW, new ArrayList<>());
		put(Items.CROSSBOW, new ArrayList<>());
	}};
	private static final List<DeferredHolder<Item, Item>> ARMOR_OVERLAYS = new ArrayList<>();
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
		for (DeferredHolder<Item, Item> o : ARMOR_OVERLAYS) {
			if (o.isBound() && stack.is(o.get()))
				return true;
		}
		return false;
	}

	@SafeVarargs
	public static void setup(String modid, IEventBus bus, Supplier<RegistryClass>... inits) {
		RegUtil.MODID = modid;
		create(Registries.ITEM); // Pre-Bake the Item DeferredRegister for ToolAndArmorHelper
		for (Supplier<RegistryClass> init : inits)
			init.get().init(bus);
		class FixedUpgradeRecipe extends SmithingTransformRecipe {
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
			public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
				ItemStack itemstack = getResultItem(pRegistryAccess).copy();
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

		});
		for (DeferredRegister<?> register : REGISTERS)
			register.register(bus);
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

	public static BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> makeAttributeFactory(AttributeData... data) {
		return (slot, stack) -> {
			ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
			for (AttributeData attribute : data) {
				if (attribute.test.test(stack)) {
					ModAttribute a = attribute.attribute.get();
					builder.put(a, new AttributeModifier(slot == null ? a.id : ARMOR_MODIFIER_UUID_PER_SLOT[slot], a.type, attribute.value, attribute.op));
				}
			}
			return builder.build();
		};
	}

	public record ItemProps(Supplier<Item.Properties> properties) {

	}

	public static class ItemTier implements Tier {
		private final String name;
		private final int harvestLevel;
		private final int maxUses;
		private final float efficiency;
		private final float attackDamage;
		private final int enchantability;
		private final LazyLoadedValue<Ingredient> repairMaterial;

		public ItemTier(String name, int harvestLevelIn, int maxUsesIn, float efficiencyIn, float attackDamageIn, int enchantabilityIn, Supplier<Ingredient> repairMaterialIn) {
			this.name = name;
			this.harvestLevel = harvestLevelIn;
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
		public int getLevel() {
			return this.harvestLevel;
		}

		@Override
		public int getEnchantmentValue() {
			return this.enchantability;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return this.repairMaterial.get();
		}
	}

	public static class ArmorMaterial implements net.minecraft.world.item.ArmorMaterial { // KB Resist max = 0.25 (0.25 * 4 = 1 = 100%)
		private static final int[] MAX_DAMAGE_ARRAY = new int[]{13, 15, 16, 11};
		private final ResourceLocation name;
		private final int maxDamageFactor;
		private final int[] damageReductionAmountArray;
		private final int enchantability;
		private final SoundEvent soundEvent;
		private final float toughness;
		private final float knockbackResistance;
		private final LazyLoadedValue<Ingredient> repairMaterial;
		private final boolean fullbright;
		private final boolean overlay;
		private final boolean overlayFullbright;

		public ArmorMaterial(String name, int maxDamageFactor, int[] damageReductionAmountArray, int enchantability, SoundEvent soundEvent, float toughness, float knockbackResistance, Supplier<Ingredient> repairMaterial, boolean fullbright, boolean overlay, boolean overlayFullbright) {
			this.name = new ResourceLocation(RegUtil.MODID, name);
			this.maxDamageFactor = maxDamageFactor;
			this.damageReductionAmountArray = damageReductionAmountArray;
			this.enchantability = enchantability;
			this.soundEvent = soundEvent;
			this.toughness = toughness;
			this.knockbackResistance = knockbackResistance;
			this.repairMaterial = new LazyLoadedValue<>(repairMaterial);
			this.fullbright = fullbright;
			this.overlay = overlay;
			this.overlayFullbright = overlayFullbright;
		}

		@Nullable
		@OnlyIn(Dist.CLIENT)
		public <A extends HumanoidModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, A _default) {
			return null;
		}

		@Nullable
		@OnlyIn(Dist.CLIENT)
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return null;
		}

		@Override
		public int getDurabilityForType(ArmorItem.Type pType) {
			return MAX_DAMAGE_ARRAY[pType.getSlot().getIndex()] * this.maxDamageFactor;
		}

		@Override
		public int getDefenseForType(ArmorItem.Type pType) {
			return this.damageReductionAmountArray[pType.getSlot().getIndex()];
		}

		@Override
		public int getEnchantmentValue() {
			return this.enchantability;
		}

		@Override
		public SoundEvent getEquipSound() {
			return this.soundEvent;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return this.repairMaterial.get();
		}

		@Override
		public String getName() {
			return this.name.toString();
		}

		public DeferredHolder<Item, Item> register(DeferredRegister<Item> REGISTRY, String append, Supplier<ArmorItem> obj) {
			return REGISTRY.register(name.getPath().toLowerCase(Locale.US).concat(append), obj);
		}

		@Override
		public float getToughness() {
			return this.toughness;
		}

		@Override
		public float getKnockbackResistance() {
			return this.knockbackResistance;
		}
	}

	public static class ToolAndArmorHelper {
		
		private static DeferredRegister<Item> REGISTRY;
		
		public record TooltipContext(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
			
		}

		public static boolean isBroken(ItemStack stack) {
			return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
		}

		public static DeferredHolder<Item, Item> sword(ItemTier tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_sword"), () -> new SwordItem(tier, 3, -2.4F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(entity);
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
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getDefaultAttributeModifiers(slot));
						if (slot == EquipmentSlot.MAINHAND)
							map.putAll(factory.apply(null, stack));
					}
					return map.build();
				}
			});
		}

		public static DeferredHolder<Item, Item> shield(ItemTier tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_shield"), () -> new ShieldItem(properties.defaultDurability(tier.getUses())) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					int dmg = Math.min(amount, 6);
					if (dmg >= remaining) {
						onBroken.accept(entity);
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

				@Override
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getAttributeModifiers(slot, stack));
						if (slot == EquipmentSlot.OFFHAND)
							map.putAll(factory.apply(4, stack));
					}
					return map.build();
				}
			});
		}

		private static DeferredHolder<Item, Item> registerBow(Item item, DeferredHolder<Item, Item> o) {
			if (BOWS.containsKey(item))
				BOWS.get(item).add(o);
			return o;
		}

		public static DeferredHolder<Item, Item> bow(ItemTier tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return registerBow(Items.BOW, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_bow"), () -> new BowItem(properties.defaultDurability(tier.getUses())) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(entity);
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

				@Override
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getAttributeModifiers(slot, stack));
						if (slot == EquipmentSlot.MAINHAND)
							map.putAll(factory.apply(null, stack));
					}
					return map.build();
				}
			}));
		}

		public static DeferredHolder<Item, Item> xbow(ItemTier tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return registerBow(Items.CROSSBOW, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_xbow"), () -> new CrossbowItem(properties.defaultDurability(tier.getUses())) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(entity);
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
					if (isCharged(itemstack)) {
						performShooting(worldIn, playerIn, handIn, itemstack, itemstack.getItem() instanceof CrossbowItem && containsChargedProjectile(itemstack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F, 1.0F);
						setCharged(itemstack, false);
						return InteractionResultHolder.consume(itemstack);
					}
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

				@Override
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getAttributeModifiers(slot, stack));
						if (slot == EquipmentSlot.MAINHAND)
							map.putAll(factory.apply(null, stack));
					}
					return map.build();
				}
			}));
		}

		public static DeferredHolder<Item, Item> axe(ItemTier tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_axe"), () -> new LootingAxe(tier, 5F, -3.0F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(entity);
					return Math.min(remaining, amount);
				}

				@Override
				public float getDestroySpeed(ItemStack stack, BlockState state) {
					return isBroken(stack) ? 0 : super.getDestroySpeed(stack, state);
				}

				@Override
				public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
					if (!isBroken(stack)) {
						// This must remain an anon class to spoof the reobfuscator from mapping to the wrong SRG name
						//noinspection Convert2Lambda
						stack.hurtAndBreak(1, attacker, new Consumer<>() {
							@Override
							public void accept(LivingEntity entityIn1) {
								entityIn1.broadcastBreakEvent(EquipmentSlot.MAINHAND);
							}
						});
						return true;
					}
					return false;
				}

				@Override
				public InteractionResult useOn(UseOnContext context) {
					return isBroken(context.getItemInHand()) ? InteractionResult.FAIL : super.useOn(context);
				}

				@Override
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getDefaultAttributeModifiers(slot));
						if (slot == EquipmentSlot.MAINHAND)
							map.putAll(factory.apply(null, stack));
					}
					return map.build();
				}
			});
		}

		public static DeferredHolder<Item, Item> pickaxe(ItemTier tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_pickaxe"), () -> new PickaxeItem(tier, 1, -2.8F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(entity);
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

				@Override
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getDefaultAttributeModifiers(slot));
						if (slot == EquipmentSlot.MAINHAND)
							map.putAll(factory.apply(null, stack));
					}
					return map.build();
				}
			});
		}

		public static DeferredHolder<Item, Item> shovel(ItemTier tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_shovel"), () -> new ShovelItem(tier, 1.5F, -3.0F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(entity);
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

				@Override
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getDefaultAttributeModifiers(slot));
						if (slot == EquipmentSlot.MAINHAND)
							map.putAll(factory.apply(null, stack));
					}
					return map.build();
				}
			});
		}

		public static DeferredHolder<Item, Item> hoe(ItemTier tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_hoe"), () -> new HoeItem(tier, -3, 0.0F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(entity);
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

				@Override
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getDefaultAttributeModifiers(slot));
						if (slot == EquipmentSlot.MAINHAND)
							map.putAll(factory.apply(null, stack));
					}
					return map.build();
				}
			});
		}

		public static DeferredHolder<Item, Item> helmet(ArmorMaterial tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapArmorItemRegistration(tier, tier.register(REGISTRY, "_helmet", armorFactory(tier, ArmorItem.Type.HELMET, properties, factory, tooltipConsumer)));
		}

		public static DeferredHolder<Item, Item> chest(ArmorMaterial tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return chest(tier, properties, factory, (stack, tick) -> false, tooltipConsumer);
		}

		public static DeferredHolder<Item, Item> chest(ArmorMaterial tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, BiPredicate<ItemStack, Boolean> elytra, Consumer<TooltipContext> tooltipConsumer) {
			return wrapArmorItemRegistration(tier, tier.register(REGISTRY, "_chest", armorFactory(tier, ArmorItem.Type.CHESTPLATE, properties, factory, elytra, tooltipConsumer)));
		}

		public static DeferredHolder<Item, Item> legs(ArmorMaterial tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapArmorItemRegistration(tier, tier.register(REGISTRY, "_legs", armorFactory(tier, ArmorItem.Type.LEGGINGS, properties, factory, tooltipConsumer)));
		}

		public static DeferredHolder<Item, Item> boots(ArmorMaterial tier, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return wrapArmorItemRegistration(tier, tier.register(REGISTRY, "_boots", armorFactory(tier, ArmorItem.Type.BOOTS, properties, factory, tooltipConsumer)));
		}

		private static DeferredHolder<Item, Item> wrapArmorItemRegistration(ArmorMaterial tier, DeferredHolder<Item, Item> object) {
			if (tier.overlay)
				ARMOR_OVERLAYS.add(object);
			return object;
		}

		private static Supplier<ArmorItem> armorFactory(ArmorMaterial tier, ArmorItem.Type slot, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, Consumer<TooltipContext> tooltipConsumer) {
			return armorFactory(tier, slot, properties, factory, (stack, tick) -> false, tooltipConsumer);
		}

		private static Supplier<ArmorItem> armorFactory(ArmorMaterial tier, ArmorItem.Type slot, Item.Properties properties, BiFunction<Integer, ItemStack, Multimap<Attribute, AttributeModifier>> factory, BiPredicate<ItemStack, Boolean> elytra, Consumer<TooltipContext> tooltipConsumer) {
			return () -> new ArmorItem(tier, slot, properties) {

				@Override
				public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
					boolean flag = !isBroken(stack) && (elytra.test(stack, true) || super.elytraFlightTick(stack, entity, flightTicks));
					if (flag && !entity.level().isClientSide && (flightTicks + 1) % 20 == 0) {
						stack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(EquipmentSlot.CHEST));
					}
					return flag;
				}

				@Override
				public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
					return !isBroken(stack) && (elytra.test(stack, false) || super.canElytraFly(stack, entity));
				}

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
					if (elytra.test(stack, false))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.elytra").withStyle(ChatFormatting.DARK_AQUA));
					tooltipConsumer.accept(new TooltipContext(stack, worldIn, tooltip, flagIn));
					super.appendHoverText(stack, worldIn, tooltip, flagIn);
				}

				@Override
				public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
					int remaining = (stack.getMaxDamage() - 1) - stack.getDamageValue();
					if (amount >= remaining)
						onBroken.accept(entity);
					return Math.min(remaining, amount);
				}

				@Override
				public void onArmorTick(ItemStack stack, Level world, Player player) {
					if (isBroken(stack)) {
						if (!player.addItem(stack))
							Containers.dropItemStack(world, player.position().x(), player.position().y(), player.position().z(), stack);
						else
							stack.shrink(1);
					}
				}

				@Override
				public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot equipmentSlot, ItemStack stack) {
					ImmutableMultimap.Builder<Attribute, AttributeModifier> map = ImmutableMultimap.builder();
					if (!isBroken(stack)) {
						map.putAll(super.getDefaultAttributeModifiers(equipmentSlot));
						if (equipmentSlot == slot.getSlot())
							map.putAll(factory.apply(equipmentSlot.getIndex(), stack));
					}
					return map.build();
				}

				@Override
				@OnlyIn(Dist.CLIENT)
				public Object getRenderPropertiesInternal() {
					return new IClientItemExtensions() {
						@Override
						public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
							HumanoidModel<?> tierModel = tier.getArmorModel(entityLiving, itemStack, armorSlot, _default);
							return tierModel != null ? tierModel : (tier.fullbright || tier.overlay) ? new HumanoidModel<>(Minecraft.getInstance().getEntityModels().
									bakeLayer(slot == Type.LEGGINGS ? ModelLayers.PLAYER_INNER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)) {
								@Override
								public void renderToBuffer(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
									super.renderToBuffer(matrixStackIn, bufferIn, (tier.fullbright || (tier.overlayFullbright && RegUtil.renderingArmorOverlay)) ? 0xF000F0 : packedLightIn, packedOverlayIn, red, green, blue, alpha);
								}
							} : IClientItemExtensions.super.getHumanoidArmorModel(entityLiving, itemStack, armorSlot, _default);
						}
					};
				}

				@Nullable
				@Override
				@OnlyIn(Dist.CLIENT)
				public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
					String tierTexture = tier.getArmorTexture(stack, entity, slot, type);
					return tierTexture != null ? tierTexture : super.getArmorTexture(stack, entity, slot, type);
				}
			};
		}

		public static abstract class LootingAxe extends AxeItem {

			private static final Set<ToolAction> ACTIONS = Stream.of(ToolActions.AXE_DIG, ToolActions.AXE_STRIP, ToolActions.AXE_SCRAPE, ToolActions.AXE_WAX_OFF, ToolActions.SWORD_SWEEP).
					collect(Collectors.toCollection(Sets::newIdentityHashSet));

			public LootingAxe(Tier tier, float attackDamageIn, float attackSpeedIn, Properties builder) {
				super(tier, attackDamageIn, attackSpeedIn, builder);
			}

			@Override
			public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
				return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, stack) > 0 ? ACTIONS.contains(toolAction) : super.canPerformAction(stack, toolAction);
			}
		}

	}

	public static class ModAttribute extends Attribute {
		final UUID id;
		final String type;

		public ModAttribute(String name, double defaultValue, UUID id, String type) {
			super(name, defaultValue);
			this.id = id;
			this.type = type;
		}
	}

	public record AttributeData(Predicate<ItemStack> test, Supplier<ModAttribute> attribute, AttributeModifier.Operation op, double value) {
		public static AttributeData make(Supplier<ModAttribute> attribute, AttributeModifier.Operation op, double value) {
			return make(stack -> true, attribute, op, value);
		}
		public static AttributeData make(Predicate<ItemStack> test, Supplier<ModAttribute> attribute, AttributeModifier.Operation op, double value) {
			return new AttributeData(test, attribute, op, value);
		}
	}

}

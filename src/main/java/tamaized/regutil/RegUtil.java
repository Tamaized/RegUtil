package tamaized.regutil;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
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
import net.minecraft.world.item.CreativeModeTab;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class RegUtil {

	private static String MODID = "regutil";

	private static CreativeModeTab CREATIVE_TAB;

	public static CreativeModeTab creativeTab() {
		return CREATIVE_TAB;
	}

	private static final UUID[] ARMOR_MODIFIER_UUID_PER_SLOT = new UUID[]{UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"), UUID.fromString("86fda400-8542-4d95-b275-c6393de5b887")};
	private static final List<DeferredRegister<?>> REGISTERS = new ArrayList<>();
	private static final Map<Item, List<RegistryObject<Item>>> BOWS = new HashMap<>() {{
		put(Items.BOW, new ArrayList<>());
		put(Items.CROSSBOW, new ArrayList<>());
	}};
	private static final List<RegistryObject<Item>> ARMOR_OVERLAYS = new ArrayList<>();
	public static boolean renderingArmorOverlay = false;

	public static boolean isMyBow(ItemStack stack, Item check) {
		List<RegistryObject<Item>> list = BOWS.get(check);
		if (list == null)
			return false;
		for (RegistryObject<Item> o : list) {
			if (stack.is(o.get()))
				return true;
		}
		return false;
	}

	public static boolean isArmorOverlay(ItemStack stack) {
		for (RegistryObject<Item> o : ARMOR_OVERLAYS) {
			if (stack.is(o.get()))
				return true;
		}
		return false;
	}

	public static StructurePieceType registerStructurePiece(String name, StructurePieceType.StructureTemplateType piece) {
		return Registry.register(Registry.STRUCTURE_PIECE, new ResourceLocation(RegUtil.MODID, name.toLowerCase(Locale.ROOT)), piece);
	}

	@SafeVarargs
	public static void setup(String modid, @Nullable Supplier<RegistryObject<Item>> creativeTabItem, IEventBus bus, Supplier<RegistryClass>... inits) {
		RegUtil.MODID = modid;
		create(ForgeRegistries.ITEMS); // Pre-Bake the Item DeferredRegister for ToolAndArmorHelper
		if (creativeTabItem != null)
			CREATIVE_TAB = new CreativeModeTab(RegUtil.MODID.concat(".item_group")) {
				@Override
				public ItemStack makeIcon() {
					return new ItemStack(creativeTabItem.get().get());
				}
			};
		for (Supplier<RegistryClass> init : inits)
			init.get().init(bus);
		class FixedUpgradeRecipe extends UpgradeRecipe {
			public FixedUpgradeRecipe(ResourceLocation p_44523_, Ingredient p_44524_, Ingredient p_44525_, ItemStack p_44526_) {
				super(p_44523_, p_44524_, p_44525_, p_44526_);
			}

			@Override
			public ItemStack assemble(Container p_44531_) {
				ItemStack itemstack = getResultItem().copy();
				CompoundTag compoundtag = p_44531_.getItem(0).getTag();
				if (compoundtag != null)
					itemstack.getOrCreateTag().merge(compoundtag.copy());
				return itemstack;
			}
		}
		create(ForgeRegistries.RECIPE_SERIALIZERS).register("smithing", () -> new UpgradeRecipe.Serializer() {
			@Override
			public UpgradeRecipe fromJson(ResourceLocation p_44562_, JsonObject p_44563_) {
				Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(p_44563_, "base"));
				Ingredient ingredient1 = Ingredient.fromJson(GsonHelper.getAsJsonObject(p_44563_, "addition"));
				ItemStack itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(p_44563_, "result"));
				return new FixedUpgradeRecipe(p_44562_, ingredient, ingredient1, itemstack);
			}

			@Override
			public UpgradeRecipe fromNetwork(ResourceLocation p_44565_, FriendlyByteBuf p_44566_) {
				Ingredient ingredient = Ingredient.fromNetwork(p_44566_);
				Ingredient ingredient1 = Ingredient.fromNetwork(p_44566_);
				ItemStack itemstack = p_44566_.readItem();
				return new FixedUpgradeRecipe(p_44565_, ingredient, ingredient1, itemstack);
			}
		});
		for (DeferredRegister<?> register : REGISTERS)
			register.register(bus);
	}

	public static <R> DeferredRegister<R> create(IForgeRegistry<R> type) {
		return create(type.getRegistryKey());
	}

	@SuppressWarnings("unchecked")
	public static <R> DeferredRegister<R> create(ResourceKey<Registry<R>> type) {
		if (type.equals(ForgeRegistries.Keys.ITEMS) && ToolAndArmorHelper.REGISTRY != null)
			return (DeferredRegister<R>) ToolAndArmorHelper.REGISTRY;
		DeferredRegister<R> def = DeferredRegister.create(type, RegUtil.MODID);
		REGISTERS.add(def);
		if (type.equals(ForgeRegistries.Keys.ITEMS))
			ToolAndArmorHelper.REGISTRY = (DeferredRegister<Item>) def;
		return def;
	}

	public static Function<Integer, Multimap<Attribute, AttributeModifier>> makeAttributeFactory(AttributeData... data) {
		return (slot) -> {
			ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
			for (AttributeData attribute : data) {
				ModAttribute a = attribute.attribute.get();
				builder.put(a, new AttributeModifier(slot == null ? a.id : ARMOR_MODIFIER_UUID_PER_SLOT[slot], a.type, attribute.value, attribute.op));
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
		public int getDurabilityForSlot(EquipmentSlot slotIn) {
			return MAX_DAMAGE_ARRAY[slotIn.getIndex()] * this.maxDamageFactor;
		}

		@Override
		public int getDefenseForSlot(EquipmentSlot slotIn) {
			return this.damageReductionAmountArray[slotIn.getIndex()];
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

		public RegistryObject<Item> register(DeferredRegister<Item> REGISTRY, String append, Supplier<ArmorItem> obj) {
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

		public static boolean isBroken(ItemStack stack) {
			return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
		}

		public static RegistryObject<Item> sword(ItemTier tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_sword"), () -> new SwordItem(tier, 3, -2.4F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
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
							map.putAll(factory.apply(null));
					}
					return map.build();
				}
			});
		}

		public static RegistryObject<Item> shield(ItemTier tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_shield"), () -> new ShieldItem(properties.defaultDurability(tier.getUses())) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
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
							map.putAll(factory.apply(4));
					}
					return map.build();
				}
			});
		}

		private static RegistryObject<Item> registerBow(Item item, RegistryObject<Item> o) {
			if (BOWS.containsKey(item))
				BOWS.get(item).add(o);
			return o;
		}

		public static RegistryObject<Item> bow(ItemTier tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return registerBow(Items.BOW, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_bow"), () -> new BowItem(properties.defaultDurability(tier.getUses())) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
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
							map.putAll(factory.apply(null));
					}
					return map.build();
				}
			}));
		}

		public static RegistryObject<Item> xbow(ItemTier tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return registerBow(Items.CROSSBOW, REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_xbow"), () -> new CrossbowItem(properties.defaultDurability(tier.getUses())) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
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
							map.putAll(factory.apply(null));
					}
					return map.build();
				}
			}));
		}

		public static RegistryObject<Item> axe(ItemTier tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_axe"), () -> new LootingAxe(tier, 5F, -3.0F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
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
							map.putAll(factory.apply(null));
					}
					return map.build();
				}
			});
		}

		public static RegistryObject<Item> pickaxe(ItemTier tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_pickaxe"), () -> new PickaxeItem(tier, 1, -2.8F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
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
							map.putAll(factory.apply(null));
					}
					return map.build();
				}
			});
		}

		public static RegistryObject<Item> shovel(ItemTier tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_shovel"), () -> new ShovelItem(tier, 1.5F, -3.0F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
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
							map.putAll(factory.apply(null));
					}
					return map.build();
				}
			});
		}

		public static RegistryObject<Item> hoe(ItemTier tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return REGISTRY.register(tier.name().toLowerCase(Locale.US).concat("_hoe"), () -> new HoeItem(tier, -3, 0.0F, properties) {

				@Override
				@OnlyIn(Dist.CLIENT)
				public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
					if (isBroken(stack))
						tooltip.add(Component.translatable(RegUtil.MODID + ".tooltip.broken").withStyle(ChatFormatting.DARK_RED));
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
							map.putAll(factory.apply(null));
					}
					return map.build();
				}
			});
		}

		public static RegistryObject<Item> helmet(ArmorMaterial tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return wrapArmorItemRegistration(tier, tier.register(REGISTRY, "_helmet", armorFactory(tier, EquipmentSlot.HEAD, properties, factory)));
		}

		public static RegistryObject<Item> chest(ArmorMaterial tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return chest(tier, properties, factory, (stack, tick) -> false);
		}

		public static RegistryObject<Item> chest(ArmorMaterial tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory, BiPredicate<ItemStack, Boolean> elytra) {
			return wrapArmorItemRegistration(tier, tier.register(REGISTRY, "_chest", armorFactory(tier, EquipmentSlot.CHEST, properties, factory, elytra)));
		}

		public static RegistryObject<Item> legs(ArmorMaterial tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return wrapArmorItemRegistration(tier, tier.register(REGISTRY, "_legs", armorFactory(tier, EquipmentSlot.LEGS, properties, factory)));
		}

		public static RegistryObject<Item> boots(ArmorMaterial tier, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return wrapArmorItemRegistration(tier, tier.register(REGISTRY, "_boots", armorFactory(tier, EquipmentSlot.FEET, properties, factory)));
		}

		private static RegistryObject<Item> wrapArmorItemRegistration(ArmorMaterial tier, RegistryObject<Item> object) {
			if (tier.overlay)
				ARMOR_OVERLAYS.add(object);
			return object;
		}

		private static Supplier<ArmorItem> armorFactory(ArmorMaterial tier, EquipmentSlot slot, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory) {
			return armorFactory(tier, slot, properties, factory, (stack, tick) -> false);
		}

		private static Supplier<ArmorItem> armorFactory(ArmorMaterial tier, EquipmentSlot slot, Item.Properties properties, Function<Integer, Multimap<Attribute, AttributeModifier>> factory, BiPredicate<ItemStack, Boolean> elytra) {
			return () -> new ArmorItem(tier, slot, properties) {

				@Override
				public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
					boolean flag = !isBroken(stack) && (elytra.test(stack, true) || super.elytraFlightTick(stack, entity, flightTicks));
					if (flag && !entity.level.isClientSide && (flightTicks + 1) % 20 == 0) {
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
						if (equipmentSlot == slot)
							map.putAll(factory.apply(equipmentSlot.getIndex()));
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
									bakeLayer(slot == EquipmentSlot.LEGS ? ModelLayers.PLAYER_INNER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)) {
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

		public ModAttribute(String name, float defaultValue, UUID id, String type) {
			super(name, defaultValue);
			this.id = id;
			this.type = type;
		}
	}

	public record AttributeData(Supplier<ModAttribute> attribute, AttributeModifier.Operation op, double value) {
		public static AttributeData make(Supplier<ModAttribute> attribute, AttributeModifier.Operation op, double value) {
			return new AttributeData(attribute, op, value);
		}
	}

}

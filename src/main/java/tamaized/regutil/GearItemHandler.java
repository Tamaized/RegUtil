package tamaized.regutil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.commons.lang3.tuple.Pair;
import tamaized.beanification.Autowired;
import tamaized.beanification.Component;
import tamaized.beanification.PostConstruct;
import tamaized.pkginfoutil.PublicApi;
import tamaized.regutil.item.BreakableHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
public class GearItemHandler {

	@Autowired
	private BreakableHelper breakableHelper;

	private final List<Pair<DeferredHolder<Item, Item>, AttributeFactory>> gearItems = new ArrayList<>();
	private final List<Pair<DeferredHolder<Item, Item>, Supplier<ArmorData>>> armorItems = new ArrayList<>();

	@PublicApi
	public boolean renderingArmorOverlay = false;

	@PostConstruct
	private void setup(IEventBus modBus, IEventBus gameBus) {
		gameBus.addListener(ItemAttributeModifierEvent.class, event -> gearItems.stream()
			.filter(p -> event.getItemStack().is(p.getKey().get()) && !breakableHelper.isBroken(event.getItemStack()))
			.forEach(p -> p.getValue().apply(event.getItemStack())
				.forEach(e -> event.addModifier(e.attribute(), e.modifier(), e.slot())))
		);

		modBus.addListener(RegisterClientExtensionsEvent.class, event -> armorItems.forEach(p -> event.registerItem(new IClientItemExtensions() {
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
						final boolean fullbright = p.getValue().get().model().isFullbright() || (p.getValue().get().model().isOverlayFullbright() && renderingArmorOverlay);
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

	public void addGearFactory(DeferredHolder<Item, Item> item, AttributeFactory factory) {
		gearItems.add(Pair.of(item, factory));
	}

	public void addArmorFactory(DeferredHolder<Item, Item> item, Supplier<ArmorData> factory) {
		armorItems.add(Pair.of(item, factory));
	}

	@PublicApi
	public boolean isArmorOverlay(ItemStack stack) {
		return armorItems.stream().anyMatch(o -> o.getValue().get().model().hasOverlay() && o.getKey().isBound() && stack.is(o.getKey().get()));
	}

}

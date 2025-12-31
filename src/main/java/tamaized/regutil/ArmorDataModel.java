package tamaized.regutil;

import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Optional;

public class ArmorDataModel {

	private final boolean fullbright;
	private final boolean overlay;
	private final boolean overlayFullbright;

	public ArmorDataModel(boolean fullbright, boolean overlay, boolean overlayFullbright) {
		this.fullbright = fullbright;
		this.overlay = overlay;
		this.overlayFullbright = overlayFullbright;
	}

	public boolean isFullbright() {
		return fullbright;
	}

	public boolean hasOverlay() {
		return overlay;
	}

	public boolean isOverlayFullbright() {
		return overlayFullbright;
	}

	@Nullable
	@OnlyIn(Dist.CLIENT)
	public Model<?> getArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model<?> original) {
		return null;
	}

	public void setupModelAnimations(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, Model<?> model, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	public Optional<Identifier> getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer) {
		return Optional.empty();
	}

}

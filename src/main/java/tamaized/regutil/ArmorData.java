package tamaized.regutil;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public record ArmorData(Holder<ArmorMaterial> material, boolean fullbright, boolean overlay, boolean overlayFullbright) {

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

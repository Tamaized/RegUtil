package tamaized.regutil;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Locale;
import java.util.function.Supplier;

public record ArmorData(Holder<ArmorMaterial> material, int durabilityFactor, ArmorDataModel model) {

	public DeferredHolder<Item, Item> register(DeferredRegister<Item> REGISTRY, String append, Supplier<ArmorItem> obj) {
		return REGISTRY.register(material.unwrap().orThrow().location().getPath().toLowerCase(Locale.US).concat(append), obj);
	}

}

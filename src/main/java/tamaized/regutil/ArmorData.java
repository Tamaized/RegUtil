package tamaized.regutil;

import net.minecraft.core.Holder;
import net.minecraft.world.item.equipment.ArmorMaterial;

public record ArmorData(Holder<ArmorMaterial> material, int durabilityFactor, ArmorDataModel model) {

}

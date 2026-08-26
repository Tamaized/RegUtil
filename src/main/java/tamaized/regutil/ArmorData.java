package tamaized.regutil;

import net.minecraft.world.item.equipment.ArmorMaterial;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;

public record ArmorData(ArmorMaterial material, @Nullable IClientItemExtensions clientExtensions) {

}

package tamaized.regutil;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public record ItemProps(Supplier<Item.Properties> properties) {
}

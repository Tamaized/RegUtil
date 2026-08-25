package tamaized.regutil;

import net.minecraft.world.item.Item;
import tamaized.pkginfoutil.PublicApi;

import java.util.function.Supplier;

@PublicApi
public record ItemProps(Supplier<Item.Properties> properties) {
}

package tamaized.regutil;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.function.Function;
import java.util.stream.Stream;

public interface AttributeFactory extends Function<ItemStack, Stream<ItemAttributeModifiers.Entry>> {
}

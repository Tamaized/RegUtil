package tamaized.regutil;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import tamaized.beanification.Autowired;
import tamaized.pkginfoutil.PublicApi;
import tamaized.regutil.item.*;

import java.util.*;
import java.util.function.*;

@tamaized.beanification.Component
public class RegUtil {

	@PublicApi
	public static final String MODULE_NAME = "regutil";


	public RegUtil(
		@Autowired RegUtilModIdProvider modIdProvider,
		@Autowired Registers registers
	) {
		modIdProvider.setModId(ModLoadingContext.get().getActiveNamespace());
		@NotNull IEventBus bus = Objects.requireNonNull(ModLoadingContext.get().getActiveContainer().getEventBus());
		registers.create(Registries.ITEM); // Pre-Bake the Item DeferredRegister for ToolAndArmorHelper
		registers.getRegisters().forEach(register -> register.register(bus));
	}

	@PublicApi
	public static <R, T extends R> DeferredHolder<R, T> register(ResourceKey<Registry<R>> registry, final String name, final Function<Identifier, ? extends T> func) {
		if (Registers.INSTANCE.modIdProvider == null) {
			RegUtilModIdProvider tmpWire = new RegUtilModIdProvider();
			tmpWire.setModId(ModLoadingContext.get().getActiveNamespace());
			Registers.INSTANCE.modIdProvider = tmpWire;
		}

		return Registers.INSTANCE.create(registry).register(name, func);
	}

	@PublicApi
	public static <R, T extends R> DeferredHolder<R, T> register(ResourceKey<Registry<R>> registry, final String name, final Supplier<? extends T> func) {
		return register(registry, name, (_) -> func.get());
	}

}

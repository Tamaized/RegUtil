package tamaized.regutil;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
import tamaized.beanification.Autowired;
import tamaized.beanification.Bean;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Registers {

	static final Registers INSTANCE = new Registers();

	@Bean
	private static Registers registers(@Autowired RegUtilModIdProvider modIdProvider) {
		INSTANCE.modIdProvider = modIdProvider;
		return INSTANCE;
	}

	@Nullable
	RegUtilModIdProvider modIdProvider;

	private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <R> DeferredRegister<R> create(ResourceKey<Registry<R>> type) {
		DeferredRegister<?> value = registers.get(type);
		if (value != null)
			return (DeferredRegister<R>) value;
		DeferredRegister<R> def = DeferredRegister.create(type, Objects.requireNonNull(modIdProvider).getModId().orElseThrow());
		registers.put(type, def);
		return def;
	}

	Collection<DeferredRegister<?>> getRegisters() {
		return registers.values();
	}

}

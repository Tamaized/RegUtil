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

//	private final String brokenStateName;

	public RegUtil(
		@Autowired RegUtilModIdProvider modIdProvider,
		@Autowired Registers registers
	) {
		modIdProvider.setModId(ModLoadingContext.get().getActiveNamespace());
//		brokenStateName = Identifier.fromNamespaceAndPath(modIdProvider.getModId().orElseThrow(), "broken_state_attributes").toString();
		@NotNull IEventBus bus = Objects.requireNonNull(ModLoadingContext.get().getActiveContainer().getEventBus());
		registers.create(Registries.ITEM); // Pre-Bake the Item DeferredRegister for ToolAndArmorHelper
		// Looks like smithing templates merge data now instead of overwrite, TODO: double check this behavior
		/*class FixedUpgradeRecipe extends SmithingTransformRecipe {
			final Ingredient template;
			final Ingredient base;
			final Ingredient addition;
			final ItemStack result;
			public FixedUpgradeRecipe(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, ItemStack pResult) {
				super(pTemplate, pBase, pAddition, pResult);
				template = pTemplate;
				base = pBase;
				addition = pAddition;
				result = pResult;
			}

			@Override
			public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
				ItemStack itemstack = input.base().transmuteCopy(this.result.getItem(), this.result.getCount());
				itemstack.applyComponents(this.result.getComponentsPatch());
				return itemstack;

				CompoundTag compoundtag = pContainer.getItem(1).getTag();
				if (compoundtag != null)
					itemstack.getOrCreateTag().merge(compoundtag.copy());
				return itemstack;
			}
		}
		create(Registries.RECIPE_SERIALIZER).register("smithing", () -> new SmithingTransformRecipe.Serializer() {
			private static final Codec<SmithingTransformRecipe> CODEC = RecordCodecBuilder.create(
					p_301062_ -> p_301062_.group(
									Ingredient.CODEC.fieldOf("template").forGetter(p_301310_ -> ((FixedUpgradeRecipe) p_301310_).template),
									Ingredient.CODEC.fieldOf("base").forGetter(p_300938_ -> ((FixedUpgradeRecipe) p_300938_).base),
									Ingredient.CODEC.fieldOf("addition").forGetter(p_301153_ -> ((FixedUpgradeRecipe) p_301153_).addition),
									ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter(p_300935_ -> ((FixedUpgradeRecipe) p_300935_).result)
							)
							.apply(p_301062_, (template, base, addition, result) -> (SmithingTransformRecipe) new FixedUpgradeRecipe(template, base, addition, result))
			);

			@Override
			public Codec<SmithingTransformRecipe> codec() {
				return CODEC;
			}

			@Override
			public SmithingTransformRecipe fromNetwork(FriendlyByteBuf p_267316_) {
				Ingredient ingredient = Ingredient.fromNetwork(p_267316_);
				Ingredient ingredient1 = Ingredient.fromNetwork(p_267316_);
				Ingredient ingredient2 = Ingredient.fromNetwork(p_267316_);
				ItemStack itemstack = p_267316_.readItem();
				return new FixedUpgradeRecipe(ingredient, ingredient1, ingredient2, itemstack);
			}

		});*/

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

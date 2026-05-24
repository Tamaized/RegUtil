package tamaized.regutil.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import tamaized.beanification.Autowired;
import tamaized.beanification.Configurable;
import tamaized.regutil.ExtraTooltipContext;

import java.util.function.Consumer;

@Configurable
public class BreakableCrossbow extends CrossbowItem {

	@Autowired
	private BreakableHelper breakableHelper;

	private final Consumer<ExtraTooltipContext> tooltipConsumer;

	public BreakableCrossbow(Properties properties, Consumer<ExtraTooltipContext> tooltipConsumer) {
		super(properties);
		this.tooltipConsumer = tooltipConsumer;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
		breakableHelper.appendHoverText(stack, context, display, builder, tooltipFlag, tooltipConsumer);
		super.appendHoverText(stack, context, display, builder, tooltipFlag);
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity, Consumer<Item> onBroken) {
		return breakableHelper.damageItem(stack, amount, onBroken);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return breakableHelper.use(player, hand, () -> super.use(level, player, hand));
	}

	@Override
	public boolean useOnRelease(ItemStack stack) {
		return stack.getItem() instanceof CrossbowItem;
	}

}

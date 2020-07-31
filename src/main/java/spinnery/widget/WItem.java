package spinnery.widget;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import spinnery.client.render.BaseRenderer;

/**
 * Draws a given item stack set via {@link WItem#setStack(ItemStack)} to the specified position. The specified size is ignored when drawing the item.
 */
public class WItem extends WAbstractWidget {
	private static final int Z_ITEM_OFFSET = 3;

	protected ItemStack stack = ItemStack.EMPTY;

	/**
	 * Gets the stack that this widget will draw
	 * @return The {@link ItemStack} to draw
	 */
	public ItemStack getStack() {
		return stack;
	}

	public <W extends WItem> W setStack(ItemStack stack) {
		this.stack = stack;
		return (W) this;
	}

	@Override
	public void draw(MatrixStack matrices, VertexConsumerProvider provider) {
		if (isHidden()) {
			return;
		}

		BaseRenderer.getAdvancedItemRenderer().renderInGui(matrices, provider, stack, getX(), getY(), getZ() + Z_ITEM_OFFSET);

		super.draw(matrices, provider);
	}
}

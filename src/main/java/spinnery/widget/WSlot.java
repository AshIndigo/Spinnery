package spinnery.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import spinnery.Spinnery;
import spinnery.client.render.BaseRenderer;
import spinnery.client.screen.BaseContainerScreen;
import spinnery.client.screen.BaseHandledScreen;
import spinnery.common.handler.BaseScreenHandler;
import spinnery.widget.api.Action;
import spinnery.widget.api.Position;
import spinnery.widget.api.Size;
import spinnery.widget.api.WModifiableCollection;

import java.util.*;
import java.util.function.BiConsumer;

import static net.fabricmc.fabric.api.network.ClientSidePacketRegistry.INSTANCE;
import static spinnery.common.registry.NetworkRegistry.*;
import static spinnery.common.utility.MouseUtilities.*;
import static spinnery.widget.api.Action.*;

/**
 * An item slot that can be used to hold item stacks. Requires a slot number {@link WSlot#setSlotNumber(int)} and
 * inventory number {@link WSlot#setInventoryNumber(int)} to properly function. Items can be go beyond the default 64 limit via the {@link WSlot#setMaximumCount(int)} method
 * Must be added to the ScreenHandler and Screen to function properly
 */
public class WSlot extends WAbstractWidget {
    private static final int Z_ITEM_OFFSET = 100;
    private static final int Z_ITEM_INFORMATION_OFFSET = 200;

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int MIDDLE = 2;

    protected int slotNumber;
    protected int inventoryNumber;
    protected int maximumCount = 0;

    protected boolean overrideMaximumCount = false;
    protected boolean skipRelease = false;
    protected boolean isLocked = false;
    protected boolean isWhitelist = false;

    protected Identifier previewTexture;

    protected List<Item> acceptItems = new ArrayList<>();
    protected List<Item> denyItems = new ArrayList<>();

    protected List<Tag<Item>> acceptTags = new ArrayList<>();
    protected List<Tag<Item>> denyTags = new ArrayList<>();

    protected List<BiConsumer<Action, Action.Subtype>> consumers = new ArrayList<>();

    /**
     * Called in {@link BaseScreenHandler#onSlotAction(int, int, int, Action, PlayerEntity)} to represent an action being performed on an item.
     * Calls {@link BiConsumer#accept(Object, Object)} on every aded consumer
     * @param action The type of {@link Action} being performed
     * @param subtype The {@link Subtype} being performed
     */
    public void consume(Action action, Action.Subtype subtype) {
        for (BiConsumer<Action, Action.Subtype> consumer : consumers) {
            consumer.accept(action, subtype);
        }
    }

    /**
     * Adds a consumer to the slot to be called later
     * @param consumer The {@link BiConsumer} that will be called via {@link WSlot#consume(Action, Subtype)}
     * @param <W> This WSlot or a subclass of it
     * @return The current instance of this class
     */
    public <W extends WSlot> W addConsumer(BiConsumer<Action, Action.Subtype> consumer) {
        consumers.add(consumer);
        return (W) this;
    }

    /**
     * Removes an already added consumer
     * @param consumer The {@link BiConsumer} that will be removed
     * @param <W> WSlot or a subclass of it
     * @return The current instance of this class
     */
    public <W extends WSlot> W removeConsumer(BiConsumer<Action, Action.Subtype> consumer) {
        consumers.remove(consumer);
        return (W) this;
    }

    /**
     * Add's the player inventory to a {@link WModifiableCollection} for instance {@link WPanel}.
     * Client only version of the method, see {@link WSlot#addHeadlessPlayerInventory(WInterface)} for the Server/{@link BaseScreenHandler}
     * @param position Starting {@link Position} to add the first top left slot.
     * @param size The {@link Size} of each slot, you probably just want 18x18
     * @param parent The {@link WModifiableCollection} to add the slots too, i.e {@link WPanel}
     * @return A {@link Collection} of all the slots added to the parent
     */
    @Environment(EnvType.CLIENT)
    public static Collection<WSlot> addPlayerInventory(Position position, Size size, WModifiableCollection parent) {
        Collection<WSlot> set = addArray(position, size, parent, 9, BaseScreenHandler.PLAYER_INVENTORY, 9, 3);
        set.addAll(addArray(position.add(0, size.getHeight() * 3 + 4, 0), size, parent, 0, BaseScreenHandler.PLAYER_INVENTORY, 9, 1));
        return set;
    }

    /**
     * Adds an array to the given {@link WModifiableCollection} with a given height and width. This is the client only version,
     * see {@link WSlot#addHeadlessArray(WModifiableCollection, int, int, int, int)} for the Server/{@link BaseScreenHandler}
     * @param position Starting {@link Position} for the array
     * @param size The {@link Size} of each slot that will be added. This will most likely be 18x18
     * @param parent The {@link WModifiableCollection} to add the slots to, i.e {@link WPanel}
     * @param slotNumber The initial slot number to start from.
     * @param inventoryNumber The inventory number for the added slots
     * @param arrayWidth How wide should the array be
     * @param arrayHeight How tall should the array be
     * @return A {@link Collection} of all the {@link WSlot}s the method just added
     */
    @Environment(EnvType.CLIENT)
    public static Collection<WSlot> addArray(Position position, Size size, WModifiableCollection parent, int slotNumber, int inventoryNumber, int arrayWidth, int arrayHeight) {
        Collection<WSlot> set = new HashSet<>();
        for (int y = 0; y < arrayHeight; ++y) {
            for (int x = 0; x < arrayWidth; ++x) {
                set.add(parent.createChild(WSlot::new, position.add(size.getWidth() * x, size.getHeight() * y, 0), size)
                        .setSlotNumber(slotNumber + y * arrayWidth + x)
                        .setInventoryNumber(inventoryNumber));
            }
        }
        return set;
    }

    /**
     * Adds the players inventory to the given {@link WInterface}
     * Server version of the method, See {@link WSlot#addPlayerInventory(Position, Size, WModifiableCollection)} for Client/Screen version
     * @param linkedInterface The {@link WInterface} to add the slots to
     * @return A {@link Collection} of all added slots
     */
    public static Collection<WSlot> addHeadlessPlayerInventory(WInterface linkedInterface) {
        Collection<WSlot> set = addHeadlessArray(linkedInterface, 0, BaseScreenHandler.PLAYER_INVENTORY, 9, 1);
        set.addAll(addHeadlessArray(linkedInterface, 9, BaseScreenHandler.PLAYER_INVENTORY, 9, 3));
        return set;
    }

    /**
     * Adds an array to the given {@link WModifiableCollection} with a given height and width.
     * Server version of the method, See {@link WSlot#addArray(Position, Size, WModifiableCollection, int, int, int, int)} for Client/Screen version
     * @param parent The {@link WModifiableCollection} to add the slots to, i.e {@link WInterface}
     * @param slotNumber The initial slot number to start from.
     * @param inventoryNumber The inventory number for the added slots
     * @param arrayWidth How wide should the array be
     * @param arrayHeight How tall should the array be
     * @return A {@link Collection} of all the {@link WSlot}s the method just added
     */
    public static Collection<WSlot> addHeadlessArray(WModifiableCollection parent, int slotNumber, int inventoryNumber, int arrayWidth, int arrayHeight) {
        Collection<WSlot> set = new HashSet<>();
        for (int y = 0; y < arrayHeight; ++y) {
            for (int x = 0; x < arrayWidth; ++x) {
                set.add(parent.createChild(WSlot::new)
                        .setSlotNumber(slotNumber + y * arrayWidth + x)
                        .setInventoryNumber(inventoryNumber));
            }
        }
        return set;
    }

    /**
     * If the slot is whitelisted or not
     * @return true if whitelisted, false if blacklisted
     */
    public boolean isWhitelist() {
        return isWhitelist;
    }

    /**
     * Sets the slot to act as a whitelist
     * @param <W> This WSlot or a subclass of it
     * @return The instance this was called on
     */
    public <W extends WSlot> W setWhitelist() {
        this.isWhitelist = true;
        return (W) this;
    }

    /**
     * Sets the slot to act as a blacklist
     * @param <W> This WSlot or a subclass of it
     * @return The instance this was called on
     */
    public <W extends WSlot> W setBlacklist() {
        this.isWhitelist = false;
        return (W) this;
    }

    public <W extends WSlot> W accept(Tag<Item>... tags) {
        this.acceptTags.addAll(Arrays.asList(tags));
        return (W) this;
    }

    public <W extends WSlot> W accept(Item... stacks) {
        this.acceptItems.addAll(Arrays.asList(stacks));
        return (W) this;
    }

    public <W extends WSlot> W refuse(Tag<Item>... tags) {
        this.denyTags.addAll(Arrays.asList(tags));
        return (W) this;
    }

    public <W extends WSlot> W refuse(Item... items) {
        this.denyItems.addAll(Arrays.asList(items));
        return (W) this;
    }

    public boolean accepts(ItemStack... stacks) {
        if (!(Arrays.stream(stacks).allMatch(stack -> getLinkedInventory().isValid(slotNumber, stack)))) {
            return false;
        }
        if (isWhitelist) {
            return Arrays.stream(stacks).allMatch(stack ->
                    (acceptItems.contains(stack.getItem()) || acceptTags.stream().anyMatch(tag -> tag.contains(stack.getItem()))));
        } else {
            return Arrays.stream(stacks).noneMatch(stack ->
                    (denyItems.contains(stack.getItem()) || denyTags.stream().anyMatch(tag -> tag.contains(stack.getItem()))));
        }
    }

    public boolean refuses(ItemStack... stacks) {
        return !accepts(stacks);
    }

    public int getMaxCount() {
        return maximumCount;
    }

    public void setFocusedInScreen() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null) {
            BaseHandledScreen<?> handledScreen = ((BaseHandledScreen<?>) screen);
            if (handledScreen.getDrawSlot() != this) {
                handledScreen.setDrawSlot(this);
            }
        }
    }

    public void setUnfocusedInScreen() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null) {
            BaseHandledScreen<?> handledScreen = ((BaseHandledScreen<?>) screen);
            if (handledScreen.getDrawSlot() == this) {
                handledScreen.setDrawSlot(null);
            }
        }
    }

    @Override
    public boolean updateFocus(float positionX, float positionY) {
        boolean value = super.updateFocus(positionX, positionY);
        if (isFocused()) {
            if (Spinnery.ENVIRONMENT == EnvType.CLIENT) {
                setFocusedInScreen();
            }
        } else {
            if (Spinnery.ENVIRONMENT == EnvType.CLIENT) {
                setUnfocusedInScreen();
            }
        }
        return value;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void draw(MatrixStack matrices, VertexConsumerProvider provider) {
        if (isHidden()) {
            return;
        }

        float x = getX();
        float y = getY();
        float z = getZ();

        float sX = getWidth();
        float sY = getHeight();

        BaseRenderer.drawBeveledPanel(matrices, provider, x, y, z, sX, sY, getStyle().asColor("top_left"), getStyle().asColor("background.unfocused"), getStyle().asColor("bottom_right"));

        if (hasPreviewTexture()) {
            BaseRenderer.drawTexturedQuad(matrices, provider, x + 1, y + 1, z, sX - 2, sY - 2, getPreviewTexture());
        }

        ItemStack stackA = getPreviewStack().isEmpty() ? getStack() : getPreviewStack();

        BaseRenderer.getAdvancedItemRenderer().renderInGui(matrices, provider, stackA, ((x + 1) + ((sX - 18) / 2)), ((y + 1) + ((sY - 18) / 2)), z);
        BaseRenderer.getAdvancedItemRenderer().renderGuiItemOverlay(matrices, provider, MinecraftClient.getInstance().textRenderer, stackA, x, y, z + 1, stackA.getCount() == 1 ? "" : withSuffix(stackA.getCount()));

        if (isFocused()) {
            BaseRenderer.drawQuad(matrices, provider, x + 1, y + 1, z + 1, sX - 2, sY - 2, getStyle().asColor("overlay"));
        }

        super.draw(matrices, provider);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onMouseReleased(float mouseX, float mouseY, int button) {
        if (button == MIDDLE || isLocked()) return;

        PlayerEntity player = getInterface().getHandler().getPlayerInventory().player;
        BaseScreenHandler handler = getInterface().getHandler();

        int[] slotNumbers = handler.getDragSlots(button).stream().mapToInt(WSlot::getSlotNumber).toArray();
        int[] inventoryNumbers = handler.getDragSlots(button).stream().mapToInt(WSlot::getInventoryNumber).toArray();

        boolean isDragging = handler.isDragging() && nanoInterval() > nanoDelay();
        boolean isCursorEmpty = player.inventory.getCursorStack().isEmpty();

        if (!skipRelease && !Screen.hasShiftDown()) {
            if (isDragging) {
                handler.onSlotDrag(slotNumbers, inventoryNumbers, Action.of(button, true));
                INSTANCE.sendToServer(SLOT_DRAG_PACKET, createSlotDragPacket(handler.syncId, slotNumbers, inventoryNumbers, Action.of(button, true)));
            } else if (!isFocused()) {
                return;
            } else if ((button == LEFT || button == RIGHT) && !isCursorEmpty) {
                handler.onSlotAction(slotNumber, inventoryNumber, button, PICKUP, player);
                INSTANCE.sendToServer(SLOT_CLICK_PACKET, createSlotClickPacket(handler.syncId, slotNumber, inventoryNumber, button, PICKUP));
            }
        }

        handler.flush();

        skipRelease = false;

        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onMouseClicked(float mouseX, float mouseY, int button) {
        if (!isFocused() || isLocked()) return;

        PlayerEntity player = getInterface().getHandler().getPlayerInventory().player;
        BaseScreenHandler handler = getInterface().getHandler();

        boolean isCursorEmpty = player.inventory.getCursorStack().isEmpty();

        if (nanoInterval() < nanoDelay() * 1.25f && button == LEFT) {
            skipRelease = true;
            handler.onSlotAction(slotNumber, inventoryNumber, button, PICKUP_ALL, player);
            INSTANCE.sendToServer(SLOT_CLICK_PACKET, createSlotClickPacket(handler.syncId, slotNumber, inventoryNumber, button, PICKUP_ALL));
        } else {
            nanoUpdate();

            if (Screen.hasShiftDown()) {
                if (button == LEFT) {
                    getInterface().getCachedWidgets().put(getClass(), this);
                    handler.onSlotAction(slotNumber, inventoryNumber, button, QUICK_MOVE, player);
                    INSTANCE.sendToServer(SLOT_CLICK_PACKET, createSlotClickPacket(handler.syncId, slotNumber, inventoryNumber, button, QUICK_MOVE));
                }
            } else {
                if ((button == LEFT || button == RIGHT) && isCursorEmpty) {
                    skipRelease = true;
                    handler.onSlotAction(slotNumber, inventoryNumber, button, PICKUP, player);
                    INSTANCE.sendToServer(SLOT_CLICK_PACKET, createSlotClickPacket(handler.syncId, slotNumber, inventoryNumber, button, PICKUP));
                } else if (button == MIDDLE) {
                    handler.onSlotAction(slotNumber, inventoryNumber, button, CLONE, player);
                    INSTANCE.sendToServer(SLOT_CLICK_PACKET, createSlotClickPacket(handler.syncId, slotNumber, inventoryNumber, button, CLONE));
                }
            }
        }

        super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onMouseDragged(float mouseX, float mouseY, int button, double deltaX, double deltaY) {
        if (!isFocused() || button == MIDDLE || isLocked()) return;

        PlayerEntity player = getInterface().getHandler().getPlayerInventory().player;
        BaseScreenHandler handler = getInterface().getHandler();

        boolean isCached = getInterface().getCachedWidgets().get(getClass()) == this;

        int[] slotNumbers = handler.getDragSlots(button).stream().mapToInt(WSlot::getSlotNumber).toArray();
        int[] inventoryNumbers = handler.getDragSlots(button).stream().mapToInt(WSlot::getInventoryNumber).toArray();

        if (Screen.hasShiftDown()) {
            if (button == LEFT && !isCached) {
                getInterface().getCachedWidgets().put(getClass(), this);
                handler.onSlotAction(slotNumber, inventoryNumber, button, QUICK_MOVE, player);
                INSTANCE.sendToServer(SLOT_CLICK_PACKET, createSlotClickPacket(handler.syncId, slotNumber, inventoryNumber, button, QUICK_MOVE));
            }
        } else {
            if ((button == LEFT || button == RIGHT) && nanoInterval() > nanoDelay() / 1.5f) {
                if (!handler.getDragSlots(button).isEmpty()) {
                    ItemStack stackA = handler.getDragSlots(button).iterator().next().getStack();
                    ItemStack stackB = getStack();

                    if ((stackA.getItem() != stackB.getItem() || stackA.getTag() != stackB.getTag())) return;
                }

                handler.getDragSlots(button).add(this);
                handler.onSlotDrag(slotNumbers, inventoryNumbers, Action.of(button, false));
            }
        }

        super.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean isLocked() {
        return isLocked;
    }

    public <W extends WSlot> W setLocked(boolean isLocked) {
        this.isLocked = isLocked;
        return (W) this;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public int getInventoryNumber() {
        return inventoryNumber;
    }

    public <W extends WSlot> W setInventoryNumber(int inventoryNumber) {
        this.inventoryNumber = inventoryNumber;
        return (W) this;
    }

    public <W extends WSlot> W setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
        return (W) this;
    }

    public <W extends WSlot> W setNumber(int inventoryNumber, int slotNumber) {
        this.inventoryNumber = inventoryNumber;
        this.slotNumber = slotNumber;
        return (W) this;
    }

    @Environment(EnvType.CLIENT)
    public boolean hasPreviewTexture() {
        return previewTexture != null;
    }

    @Environment(EnvType.CLIENT)
    public Identifier getPreviewTexture() {
        return previewTexture;
    }

    @Environment(EnvType.CLIENT)
    public <W extends WSlot> W setPreviewTexture(Identifier previewTexture) {
        this.previewTexture = previewTexture;
        return (W) this;
    }

    public ItemStack getPreviewStack() {
        getInterface().getHandler().getPreviewStacks().putIfAbsent(getInventoryNumber(), new HashMap<>());
        return getInterface().getHandler().getPreviewStacks().get(getInventoryNumber()).getOrDefault(getSlotNumber(), ItemStack.EMPTY);
    }

    public ItemStack getStack() {
        try {
            ItemStack stackA = getLinkedInventory().getStack(getSlotNumber());

            if (!isOverrideMaximumCount()) {
                setMaximumCount(stackA.getMaxCount());
            }

            return stackA;
        } catch (ArrayIndexOutOfBoundsException exception) {
            Spinnery.LOGGER.log(Level.ERROR, "Cannot access slot " + getSlotNumber() + ", as it does exist in the inventory!");
            exception.printStackTrace();
            return ItemStack.EMPTY;
        } catch (NullPointerException exception) {
            Spinnery.LOGGER.log(Level.ERROR, "Cannot retrieve stack for slot " + getSlotNumber() + ", due to NullPointerException!");
            exception.printStackTrace();
            return ItemStack.EMPTY;
        }
    }

    @Environment(EnvType.CLIENT)
    private static String withSuffix(long value) {
        if (value < 1000) return "" + value;
        int exp = (int) (Math.log(value) / Math.log(1000));
        return String.format("%.1f%c", value / Math.pow(1000, exp), "KMGTPE".charAt(exp - 1));
    }

    public Inventory getLinkedInventory() {
        return getInterface().getHandler().getInventories().get(inventoryNumber);
    }

    public <W extends WSlot> W setStack(ItemStack stack) {
        try {
            getLinkedInventory().setStack(slotNumber, stack);
            if (!isOverrideMaximumCount()) {
                setMaximumCount(stack.getMaxCount());
            }
        } catch (ArrayIndexOutOfBoundsException exception) {
            Spinnery.LOGGER.log(Level.ERROR, "Cannot access slot " + getSlotNumber() + ", as it does exist in the inventory!");
            exception.printStackTrace();
        }
        return (W) this;
    }

    public boolean isOverrideMaximumCount() {
        return overrideMaximumCount;
    }

    public <W extends WSlot> W setMaximumCount(int maximumCount) {
        this.maximumCount = maximumCount;
        return (W) this;
    }

    public <W extends WSlot> W setOverrideMaximumCount(boolean overrideMaximumCount) {
        this.overrideMaximumCount = overrideMaximumCount;
        return (W) this;
    }

    public <W extends WSlot> W setPreviewStack(ItemStack previewStack) {
        getInterface().getHandler().getPreviewStacks().putIfAbsent(getInventoryNumber(), new HashMap<>());
        getInterface().getHandler().getPreviewStacks().get(getInventoryNumber()).put(getSlotNumber(), previewStack);
        return (W) this;
    }

    public static <W extends WSlot> W setStack(WSlot slot, ItemStack stack) {
        slot.setStack(stack);
        return (W) slot;
    }

    public void acceptStack(ItemStack itemStack) {
        setStack(itemStack);
    }
}

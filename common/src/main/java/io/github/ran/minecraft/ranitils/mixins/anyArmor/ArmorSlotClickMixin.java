package io.github.ran.minecraft.ranitils.mixins.anyArmor;

import io.github.ran.minecraft.ranitils.features.anyArmor.AnyArmor;
import io.github.ran.minecraft.ranitils.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


// FIXME: Fix this bad code cause I didn't know what I was doing and it was more of a test to see if this is possible.
@Mixin(MultiPlayerGameMode.class)
public abstract class ArmorSlotClickMixin {
	@Unique
	private int prevPickSlot = -1;

	#if POST_MC_1_16_5
	@Shadow public abstract void handleInventoryMouseClick(int i, int j, int k, ClickType clickType, Player player);
	#else
	@Shadow public abstract ItemStack handleInventoryMouseClick(int i, int j, int k, ClickType arg, Player arg2);
	#endif

	@Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
	#if POST_MC_1_16_5
	private void onClickSlot(int syncId, int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
	#else
	private void onClickSlot(int syncId, int slotId, int button, ClickType clickType, Player player, CallbackInfoReturnable<ItemStack> cir) {
	#endif
		if (ModConfig.getInstance().wearableItems) {
			// If player clicked on a slot in their inventory
			if (clickType == ClickType.PICKUP && Minecraft.getInstance().screen instanceof InventoryScreen) {
				// If player clicked on an armor slot else set the clicked slot
				if (slotId >= 5 && slotId <= 8) {
					// Get & set the slot clicked before the armor slot
					if (prevPickSlot > -1) {
						if (prevPickSlot >= 5 && prevPickSlot <= 8) {
							// If the previously clicked slot is an armor slot, put the armor back in the slot
							putBack(syncId, slotId, button, clickType, player);
						} else {
							// Magic!
							this.handleInventoryMouseClick(syncId, prevPickSlot, button, clickType, player);
							AnyArmor.putArmor_MC(prevPickSlot, slotId);

							// Put the item previously in the armor slot in the user's cursor
							this.handleInventoryMouseClick(syncId, prevPickSlot, 0, ClickType.PICKUP, player);
						}
						#if POST_MC_1_16_5
						ci.cancel();
						#else
						cir.setReturnValue(ItemStack.EMPTY);
						#endif
					} else {
						prevPickSlot = slotId;
					}
				} else {
					prevPickSlot = slotId;
				}
			}
		}
	}

	@Unique
	private void putBack(int syncId, int slotId, int button, ClickType clickType, Player player) {
		for (Slot slot : player.containerMenu.slots) {
			#if POST_MC_1_16_5
			if (!slot.hasItem() && slot.getContainerSlot() > 8) {
				this.handleInventoryMouseClick(syncId, slot.getContainerSlot(), button, clickType, player);
				AnyArmor.putArmor_MC(slot.getContainerSlot(), slotId);
				return;
			}
			#else
			if (!slot.hasItem() && slot.index > 8) {
				this.handleInventoryMouseClick(syncId, slot.index, button, clickType, player);
				AnyArmor.putArmor_MC(slot.index, slotId);
				return;
			}
			#endif
		}
	}
}

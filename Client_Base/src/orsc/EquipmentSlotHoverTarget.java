package orsc;

final class EquipmentSlotHoverTarget {
	static final int SLOT_WIDTH = 48;
	static final int SLOT_HEIGHT = 32;

	private EquipmentSlotHoverTarget() {
	}

	static int findSlot(
		int[] slotX,
		int[] slotY,
		int slotCount,
		int panelX,
		int panelY,
		int pointerX,
		int pointerY) {
		if (slotX == null || slotY == null || slotCount <= 0 || pointerY < panelY) {
			return -1;
		}
		int limit = Math.min(slotCount, Math.min(slotX.length, slotY.length));
		for (int slot = 0; slot < limit; slot++) {
			int left = panelX + slotX[slot];
			int top = panelY + slotY[slot];
			if (pointerX >= left && pointerX < left + SLOT_WIDTH
				&& pointerY >= top && pointerY < top + SLOT_HEIGHT) {
				return slot;
			}
		}
		return -1;
	}
}

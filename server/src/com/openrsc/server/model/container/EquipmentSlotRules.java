package com.openrsc.server.model.container;

import com.openrsc.server.model.container.Equipment.EquipmentSlot;

/** Pure equipment-slot relationship rules; container mutation remains in {@link Equipment}. */
final class EquipmentSlotRules {
	private EquipmentSlotRules() {
	}

	static boolean isOffenseSlot(int wieldPosition, EquipmentSlot... allowedSlots) {
		EquipmentSlot itemSlot = EquipmentSlot.get(wieldPosition);
		if (itemSlot == null) {
			return false;
		}
		for (EquipmentSlot allowedSlot : allowedSlots) {
			if (itemSlot == allowedSlot) {
				return true;
			}
		}
		return false;
	}

	static boolean bowConflictsWithOffhand(
		boolean requestedIsBow,
		int requestedWieldPosition,
		boolean equippedIsBow,
		int equippedWieldPosition) {
		return requestedIsBow && equippedWieldPosition == EquipmentSlot.SLOT_OFFHAND.getIndex()
			|| requestedWieldPosition == EquipmentSlot.SLOT_OFFHAND.getIndex() && equippedIsBow;
	}

	static boolean allowsHandFootArmorOverlap(int requestedWieldPosition, int equippedWieldPosition) {
		EquipmentSlot requestedSlot = EquipmentSlot.get(requestedWieldPosition);
		EquipmentSlot equippedSlot = EquipmentSlot.get(equippedWieldPosition);
		if (requestedSlot == null || equippedSlot == null) {
			return false;
		}
		return isHandFootArmorSlot(requestedSlot) && isBodyLegArmorSlot(equippedSlot)
			|| isHandFootArmorSlot(equippedSlot) && isBodyLegArmorSlot(requestedSlot);
	}

	static boolean isMajorArmorPenaltySlot(int wieldPosition) {
		EquipmentSlot itemSlot = EquipmentSlot.get(wieldPosition);
		if (itemSlot == null) {
			return false;
		}
		switch (itemSlot) {
			case SLOT_LARGE_HELMET:
			case SLOT_MEDIUM_HELMET:
			case SLOT_CHAIN_BODY:
			case SLOT_PLATE_BODY:
			case SLOT_PLATE_LEGS:
			case SLOT_SKIRT:
			case SLOT_GLOVES:
			case SLOT_BOOTS:
				return true;
			default:
				return false;
		}
	}

	static boolean isArmorSlot(int wieldPosition) {
		EquipmentSlot itemSlot = EquipmentSlot.get(wieldPosition);
		if (itemSlot == null) {
			return false;
		}
		switch (itemSlot) {
			case SLOT_LARGE_HELMET:
			case SLOT_MEDIUM_HELMET:
			case SLOT_CHAIN_BODY:
			case SLOT_PLATE_BODY:
			case SLOT_PLATE_LEGS:
			case SLOT_SKIRT:
			case SLOT_GLOVES:
			case SLOT_BOOTS:
			case SLOT_OFFHAND:
				return true;
			default:
				return false;
		}
	}

	private static boolean isHandFootArmorSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.SLOT_GLOVES || slot == EquipmentSlot.SLOT_BOOTS;
	}

	private static boolean isBodyLegArmorSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.SLOT_CHAIN_BODY
			|| slot == EquipmentSlot.SLOT_PLATE_BODY
			|| slot == EquipmentSlot.SLOT_PLATE_LEGS
			|| slot == EquipmentSlot.SLOT_SKIRT;
	}
}

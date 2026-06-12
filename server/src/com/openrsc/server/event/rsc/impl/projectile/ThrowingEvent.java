package com.openrsc.server.event.rsc.impl.projectile;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.content.DropTable;
import com.openrsc.server.content.SkillCapes;
import com.openrsc.server.event.rsc.DuplicationStrategy;
import com.openrsc.server.event.rsc.GameTickEvent;
import com.openrsc.server.model.PathValidation;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.KillType;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.Prayers;
import com.openrsc.server.model.entity.update.Projectile;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.triggers.PlayerRangeNpcTrigger;
import com.openrsc.server.plugins.triggers.PlayerRangePlayerTrigger;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.Formulae;
import com.openrsc.server.util.rsc.MessageType;

import java.util.ArrayList;
import java.util.List;

public class ThrowingEvent extends GameTickEvent {

	private static final int SHURIKEN_THROW_COUNT = 3;
	private boolean deliveredFirstProjectile;
	private Mob target;

	public ThrowingEvent(final World world, final Player owner, final long ticksDelay, final Mob victim) {
		super(world, owner, ticksDelay, "Throwing Event", DuplicationStrategy.ONE_PER_MOB);
		this.target = victim;
	}

	public boolean equals(Object o) {
		if (o instanceof ThrowingEvent) {
			ThrowingEvent e = (ThrowingEvent) o;
			return e.belongsTo(getOwner());
		}
		return false;
	}

	public Mob getTarget() {
		return target;
	}

	public void reTarget(final Mob mob) {
		target = mob;
		setDelayTicks(2);
		long currentTick = getPlayerOwner().getWorld().getServer().getCurrentTick();
		if (getPlayerOwner().getAttribute("can_range_again", 0L) > currentTick + 1) {
			getPlayerOwner().setAttribute("can_range_again", currentTick + 1);
		}
	}

	public void restart() {
		running = true;
	}

	private GroundItem getFloorItem(int id, Player player, Mob floorTarget) {
		return floorTarget.getViewArea().getVisibleGroundItem(id, floorTarget.getLocation(), player);
	}

	private int getAttackRadius(final int throwingEquip) {
		return RangeUtils.getThrowingAttackRadius(throwingEquip);
	}

	@Override
	public void run() {
		final Player player = getPlayerOwner();

		long currentTick = player.getWorld().getServer().getCurrentTick();
		if (player.getAttribute("can_range_again", 0L) > currentTick) return;

		int throwingID = player.getThrowingEquip();
		if (!player.loggedIn() || (player.inCombat() && !(target.isNpc()
				&& player.getOpponent() != null
				&& player.getOpponent().isNpc()
				&& !player.getOpponent().equals(target)))
				|| (target.isPlayer() && !((Player) target).loggedIn())
				|| target.getSkills().getLevel(Skill.HITS.id()) <= 0
				|| !player.checkAttack(target, true)
				|| !player.withinRange(target)) {
			player.resetRange();
			return;
		}

		final int attackRadius = getAttackRadius(throwingID);
		if (!player.withinRange(target, attackRadius)) {
			player.walkToEntity(target.getX(), target.getY());
			if (getOwner().nextStep(getOwner().getX(), getOwner().getY(), target) == null && throwingID != -1) {
				player.message("I can't get close enough");
				player.resetRange();
			}
			return;
		}
		if (!player.withinRange(target, RangeUtils.getApproachRadius(attackRadius)) && !player.finishedPath()) {
			return;
		}



		player.resetPath();
		if (!PathValidation.checkPath(getWorld(), player.getLocation(), target.getLocation())) {
			player.message("I can't get a clear shot from here");
			player.resetRange();
			return;
		}

		// Authentic player always faced NW
		player.face(player.getX() + 1, player.getY() - 1);

		if (target.isPlayer()) {
			Player playerTarget = (Player) target;
			if (!playerTarget.getConfig().WANT_MYWORLD && playerTarget.getPrayers().isPrayerActivated(Prayers.PROTECT_FROM_MISSILES)) {
				player.message("Player has a protection from missiles prayer active!");
				return;
			}
		}

		if (target.isNpc()) {
			if (target.getWorld().getServer().getPluginHandler().handlePlugin(PlayerRangeNpcTrigger.class, getPlayerOwner(), new Object[]{getOwner(), target})) {
				player.resetRange();
				return;
			}
		} else {
			if (target.getWorld().getServer().getPluginHandler().handlePlugin(PlayerRangePlayerTrigger.class, player, new Object[]{getOwner(), target})) {
				player.resetRange();
				return;
			}
		}

		if (throwingID == -1) {
			ActionSender.sendSound(player, "outofammo");
			player.message(ProjectileFailureReason.OUT_OF_AMMO.getText());
			player.resetRange();
			return;
		}

		List<Mob> throwingTargets = selectThrowingTargets(player, throwingID, attackRadius);
		if (throwingTargets.isEmpty()) {
			player.resetRange();
			return;
		}

		int throwsToConsume = RangeUtils.SHURIKENS.contains(throwingID) ? throwingTargets.size() : 1;
		int availableThrows = getAvailableThrowingCount(player, throwingID);
		if (availableThrows < 1) {
			player.resetRange();
			return;
		}
		throwsToConsume = Math.min(throwsToConsume, availableThrows);
		while (throwingTargets.size() > throwsToConsume) {
			throwingTargets.remove(throwingTargets.size() - 1);
		}
		if (!removeThrowingItems(player, throwingID, throwsToConsume)) {
			player.resetRange();
			return;
		}
		/*if (!getPlayerOwner().getLocation().isMembersWild()) {
			getPlayerOwner().message("Members content can only be used in wild levels: "
					+ World.membersWildStart + " - " + World.membersWildMax);
			getPlayerOwner().message("You can not use this type of ranged in wilderness");
			getPlayerOwner().resetRange();
			stop();
			return;
		}*/

		boolean skillCape = SkillCapes.shouldActivate(player, ItemId.RANGED_CAPE);

		int delay = 3;
		if (skillCape) {
			player.playerServerMessage(MessageType.QUEST, "@gre@Your Ranged cape activates, letting you shoot two arrows at once!");
			delay = 1;
		}

		for (int i = 0; i < throwingTargets.size(); i++) {
			applyThrowingHit(player, throwingID, throwingTargets.get(i), skillCape, i == 0);
		}
		ActionSender.sendSound(player, "shoot");

		final int adjustedDelay = RangeUtils.getAdjustedRangeDelayTicks(player, delay);
		setDelayTicks(adjustedDelay);

		player.setAttribute("can_range_again", getWorld().getServer().getCurrentTick() + adjustedDelay);
		getOwner().setKillType(KillType.RANGED);
		deliveredFirstProjectile = true;
	}

	private int getAvailableThrowingCount(Player player, int throwingID) {
		if (getWorld().getServer().getConfig().WANT_EQUIPMENT_TAB) {
			int slot = player.getCarriedItems().getEquipment().searchEquipmentForItem(throwingID);
			if (slot < 0)
				return 0;
			Item rangeType = player.getCarriedItems().getEquipment().get(slot);
			if (rangeType == null)
				return 0;
			return rangeType.getAmount();
		}

		int slot = player.getCarriedItems().getInventory().getLastIndexById(throwingID);
		if (slot < 0) {
			return 0;
		}
		Item rangeType = player.getCarriedItems().getInventory().get(slot);
		return rangeType == null ? 0 : rangeType.getAmount();
	}

	private boolean removeThrowingItems(Player player, int throwingID, int amount) {
		if (getWorld().getServer().getConfig().WANT_EQUIPMENT_TAB) {
			int slot = player.getCarriedItems().getEquipment().searchEquipmentForItem(throwingID);
			if (slot < 0)
				return false;
			Item rangeType = player.getCarriedItems().getEquipment().get(slot);
			if (rangeType == null)
				return false;
			return player.getCarriedItems().getEquipment().remove(rangeType, amount) >= 0;
		}

		int slot = player.getCarriedItems().getInventory().getLastIndexById(throwingID);
		if (slot < 0) {
			return false;
		}
		Item rangeType = player.getCarriedItems().getInventory().get(slot);
		if (rangeType == null) {
			return false;
		}
		Item toRemove = new Item(rangeType.getCatalogId(), amount, false, rangeType.getItemId());
		return player.getCarriedItems().remove(toRemove) >= 0;
	}

	private List<Mob> selectThrowingTargets(Player player, int throwingID, int attackRadius) {
		List<Mob> targets = new ArrayList<>();
		targets.add(target);
		if (!RangeUtils.SHURIKENS.contains(throwingID) || !target.isNpc()) {
			return targets;
		}

		List<Npc> candidates = getValidAdditionalShurikenTargets(player, attackRadius);
		if (candidates.isEmpty()) {
			return targets;
		}

		int aggroedCount = isAggroedToPlayer((Npc) target, player) ? 1 : 0;
		for (Npc candidate : candidates) {
			if (isAggroedToPlayer(candidate, player)) {
				aggroedCount++;
			}
		}

		List<Npc> preferred = new ArrayList<>();
		List<Npc> fallback = new ArrayList<>();
		boolean preferAggroed = aggroedCount >= SHURIKEN_THROW_COUNT;
		for (Npc candidate : candidates) {
			boolean aggroed = isAggroedToPlayer(candidate, player);
			if (aggroed == preferAggroed) {
				preferred.add(candidate);
			} else {
				fallback.add(candidate);
			}
		}

		addRandomShurikenTargets(targets, preferred);
		addRandomShurikenTargets(targets, fallback);
		return targets;
	}

	private List<Npc> getValidAdditionalShurikenTargets(Player player, int attackRadius) {
		List<Npc> candidates = new ArrayList<>();
		for (Npc npc : player.getViewArea().getNpcsInView()) {
			if (npc.equals(target)
				|| npc.getSkills().getLevel(Skill.HITS.id()) <= 0
				|| !npc.getDef().isAttackable()
				|| !player.withinRange(npc, attackRadius)
				|| !PathValidation.checkPath(getWorld(), player.getLocation(), npc.getLocation())
				|| !player.checkAttack(npc, true)) {
				continue;
			}
			candidates.add(npc);
		}
		return candidates;
	}

	private void addRandomShurikenTargets(List<Mob> targets, List<Npc> candidates) {
		while (targets.size() < SHURIKEN_THROW_COUNT && !candidates.isEmpty()) {
			targets.add(candidates.remove(DataConversions.random(0, candidates.size() - 1)));
		}
	}

	private boolean isAggroedToPlayer(Npc npc, Player player) {
		return npc.getOpponent() == player || npc.getPreferredThreatTarget() == player;
	}

	private void applyThrowingHit(Player player, int throwingID, Mob hitTarget, boolean skillCape, boolean showProjectile) {
		int damage = RangeUtils.doRangedDamage(player, throwingID, throwingID, hitTarget, skillCape);

		RangeUtils.applyDragonFireBreath(player, hitTarget, deliveredFirstProjectile);
		if((hitTarget.isPlayer() || getWorld().getServer().getConfig().RANGED_GIVES_XP_HIT) && damage > 0) {
			player.incExp(Skill.RANGED.id(), Formulae.rangedHitExperience(hitTarget, damage), true);
		}

		if (Formulae.loseArrow(damage)) {
			GroundItem thrownItemOnGround = getFloorItem(throwingID, player, hitTarget);

			if (!DropTable.handleRingOfAvarice(player, new Item(throwingID, 1))) {
				if (thrownItemOnGround == null || !thrownItemOnGround.getDef().isStackable()) {
					getWorld().registerItem(new GroundItem(player.getWorld(), throwingID, hitTarget.getX(), hitTarget.getY(), 1, player));
				} else {
					thrownItemOnGround.setAmount(thrownItemOnGround.getAmount() + 1);
				}
			}
		}

		final int projectileType = RangeUtils.SHURIKENS.contains(throwingID)
			? Projectile.SHURIKEN
			: RangeUtils.THROWING_KNIVES.contains(throwingID)
				? Projectile.THROWING_KNIFE
				: RangeUtils.THROWING_DARTS.contains(throwingID)
					? Projectile.THROWING_DART
					: Projectile.RANGED;
		getWorld().getServer().getGameEventHandler().add(new ProjectileEvent(getWorld(), player, hitTarget, damage, 2,
			true, throwingID, 0, 0, 0, 0, DuplicationStrategy.ONE_PER_MOB, projectileType, 0, showProjectile));
	}
}

package com.uninvited.mimic;

import com.uninvited.config.UninvitedConfig;
import com.uninvited.stage.UninvitedState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * The thing that isn't on the player list.
 *
 * <p>Server side it stores WHO it is impersonating (UUID, real name, the homoglyph name it shows,
 * and that player's public skin property) in tracked data, so every client renders the same
 * disguise. The client renderer (client source set) turns that into a real player skin.
 *
 * <p>Built-in tells, so players can learn to spot it:
 * <ul>
 *   <li>Wrong held item / wrong armour (see {@link MimicTells}).</li>
 *   <li>No footstep sounds ({@link #playStepSound} does nothing).</li>
 *   <li>Never jumps ({@link #jump} does nothing; it can only step up one block).</li>
 *   <li>One character of its name tag is a lookalike ({@link Homoglyphs}).</li>
 * </ul>
 */
public class MimicEntity extends HostileEntity {
	private static final TrackedData<Optional<UUID>> DISGUISE_UUID =
			DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	private static final TrackedData<String> DISGUISE_NAME =
			DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.STRING);
	private static final TrackedData<String> FAKE_NAME =
			DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.STRING);
	/** Base64 "textures" property of the impersonated player's GameProfile ("" if unknown). */
	private static final TrackedData<String> SKIN_VALUE =
			DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.STRING);
	private static final TrackedData<String> SKIN_SIGNATURE =
			DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.STRING);
	/** True while it flickers out of existence (Witness Bell, or being found out). */
	private static final TrackedData<Boolean> REVEALED =
			DataTracker.registerData(MimicEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private static final int REVEAL_TICKS = 24;

	// ---- Server-only state (never saved: the entity type has disableSaving()) -------------
	private MimicMode mode = MimicMode.IDLE;
	@Nullable
	private UUID targetUuid;
	private int lifetimeTicks = 20 * 120;
	private int revealTicks = -1;

	public MimicEntity(EntityType<? extends MimicEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			this.setEquipmentDropChance(slot, 0.0f);
		}
	}

	public static DefaultAttributeContainer.Builder createMimicAttributes(UninvitedConfig config) {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, config.mimicHealth)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, config.mimicDamage)
				// Roughly a walking player at navigation speed 1.0, a sprinting one at ~1.35.
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 96.0)
				// Tell: it never jumps. A full-block step height lets it walk up one block anyway.
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(DISGUISE_UUID, Optional.empty());
		builder.add(DISGUISE_NAME, "");
		builder.add(FAKE_NAME, "");
		builder.add(SKIN_VALUE, "");
		builder.add(SKIN_SIGNATURE, "");
		builder.add(REVEALED, false);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 64.0f, 1.0f));
	}

	// ---------------------------------------------------------------------------------------
	// Disguise
	// ---------------------------------------------------------------------------------------

	/**
	 * Puts on a player's face. {@code seed} picks the homoglyph and the tells, so the same
	 * disguise is "wrong" the same way for everyone.
	 */
	public void disguiseAs(UninvitedState.KnownPlayer player, @Nullable ServerPlayerEntity online, long seed) {
		this.dataTracker.set(DISGUISE_UUID, Optional.of(player.uuid()));
		this.dataTracker.set(DISGUISE_NAME, player.name());
		this.dataTracker.set(FAKE_NAME, Homoglyphs.disguise(player.name(), seed ^ player.uuid().getLeastSignificantBits()));
		this.dataTracker.set(SKIN_VALUE, player.skinValue() == null ? "" : player.skinValue());
		this.dataTracker.set(SKIN_SIGNATURE, player.skinSignature() == null ? "" : player.skinSignature());
		MimicTells.apply(this, online, seed);
	}

	public Optional<UUID> getDisguiseUuid() {
		return this.dataTracker.get(DISGUISE_UUID);
	}

	public String getDisguiseName() {
		return this.dataTracker.get(DISGUISE_NAME);
	}

	public String getFakeName() {
		return this.dataTracker.get(FAKE_NAME);
	}

	public String getSkinValue() {
		return this.dataTracker.get(SKIN_VALUE);
	}

	public String getSkinSignature() {
		return this.dataTracker.get(SKIN_SIGNATURE);
	}

	public boolean isRevealed() {
		return this.dataTracker.get(REVEALED);
	}

	/** The name tag (and death messages: "Alex was slain by A1ex") show the homoglyph name. */
	@Override
	public Text getName() {
		String fake = getFakeName();
		if (!fake.isEmpty()) return Text.literal(fake);
		return super.getName();
	}

	// ---------------------------------------------------------------------------------------
	// Behaviour state
	// ---------------------------------------------------------------------------------------

	public MimicMode getMode() {
		return mode;
	}

	public void setMode(MimicMode mode) {
		this.mode = mode;
	}

	@Nullable
	public UUID getTargetUuid() {
		return targetUuid;
	}

	public void setTargetUuid(@Nullable UUID targetUuid) {
		this.targetUuid = targetUuid;
	}

	/** The player this mimic is after, if they are still in this world and targetable. */
	@Nullable
	public ServerPlayerEntity getTargetPlayer() {
		if (targetUuid == null || !(this.getWorld() instanceof ServerWorld world)) return null;
		PlayerEntity player = world.getPlayerByUuid(targetUuid);
		if (player instanceof ServerPlayerEntity serverPlayer && serverPlayer.isAlive() && !serverPlayer.isSpectator()) {
			return serverPlayer;
		}
		return null;
	}

	public void setLifetimeTicks(int ticks) {
		this.lifetimeTicks = ticks;
	}

	/** Re-reads health/damage from the config, so /uninvited reload applies to new mimics. */
	public void applyConfigAttributes() {
		UninvitedConfig config = UninvitedConfig.get();
		EntityAttributeInstance health = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (health != null) health.setBaseValue(config.mimicHealth);
		EntityAttributeInstance damage = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
		if (damage != null) damage.setBaseValue(config.mimicDamage);
		setHealth(getMaxHealth());
	}

	// ---------------------------------------------------------------------------------------
	// Vanishing
	// ---------------------------------------------------------------------------------------

	/** Flickers for about a second, then is gone. Used by the Witness Bell and when it's found out. */
	public void reveal() {
		if (revealTicks >= 0 || this.getWorld().isClient) return;
		revealTicks = REVEAL_TICKS;
		this.dataTracker.set(REVEALED, true);
		this.getNavigation().stop();
		this.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 0.8f, 0.5f);
	}

	/** Gone, right now, with only a wisp of smoke. */
	public void vanish() {
		if (this.getWorld() instanceof ServerWorld world && !this.isRemoved()) {
			world.spawnParticles(ParticleTypes.SMOKE, getX(), getBodyY(0.5), getZ(), 12, 0.25, 0.6, 0.25, 0.01);
			world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_CANDLE_EXTINGUISH, SoundCategory.HOSTILE, 0.6f, 0.5f);
			this.discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld().isClient) return;

		if (revealTicks >= 0) {
			if (revealTicks-- == 0) vanish();
			return;
		}
		if (--lifetimeTicks <= 0) {
			vanish();
		}
	}

	// ---------------------------------------------------------------------------------------
	// Tells: no footsteps, no jumping
	// ---------------------------------------------------------------------------------------

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		// Tell: it makes no footstep sounds. Listen for a friend who walks silently.
	}

	@Override
	public void jump() {
		// Tell: it never jumps. Step height 1.0 lets it walk up single blocks; anything taller
		// and it has to find another way round.
	}

	// ---------------------------------------------------------------------------------------
	// Sounds: it borrows the player's, except footsteps
	// ---------------------------------------------------------------------------------------

	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PLAYER_DEATH;
	}

	// ---------------------------------------------------------------------------------------
	// Damage: before the hunt it simply isn't there when you swing at it
	// ---------------------------------------------------------------------------------------

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (this.getWorld().isClient || revealTicks >= 0) return false;
		if (source.getAttacker() instanceof PlayerEntity && mode != MimicMode.HUNT) {
			vanish();
			return false;
		}
		boolean hurt = super.damage(source, amount);
		if (hurt && this.isAlive() && mode == MimicMode.HUNT
				&& getHealth() <= getMaxHealth() * (1.0 - UninvitedConfig.get().mimicFleeHealthFraction)) {
			mode = MimicMode.FLEE;
		}
		return hurt;
	}

	@Override
	protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
		// Its clothes are as fake as its face.
	}

	// ---------------------------------------------------------------------------------------
	// Lifecycle: the director decides when it goes, not vanilla despawning
	// ---------------------------------------------------------------------------------------

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public void checkDespawn() {
	}

	@Override
	protected boolean isDisallowedInPeaceful() {
		// It still haunts peaceful worlds; it just can't hurt anyone there.
		return false;
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		// Intentionally nothing extra: the entity type is never saved.
	}
}

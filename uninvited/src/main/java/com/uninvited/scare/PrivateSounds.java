package com.uninvited.scare;

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

/**
 * Sounds that only ONE player hears. Vanilla {@code world.playSound} broadcasts to everyone
 * nearby; we send the sound packet straight to the target's connection instead. The position is
 * still a real world position, so the client pans and attenuates it normally: a footstep 3 blocks
 * behind you sounds 3 blocks behind you.
 */
public final class PrivateSounds {
	private PrivateSounds() {
	}

	public static void play(ServerPlayerEntity player, SoundEvent sound, Vec3d pos, float volume, float pitch) {
		play(player, Registries.SOUND_EVENT.getEntry(sound), pos, volume, pitch);
	}

	public static void play(ServerPlayerEntity player, RegistryEntry<SoundEvent> sound, Vec3d pos, float volume, float pitch) {
		player.networkHandler.sendPacket(new PlaySoundS2CPacket(sound, SoundCategory.BLOCKS, pos.x, pos.y, pos.z,
				volume, pitch, player.getRandom().nextLong()));
	}

	/**
	 * A point {@code distance} blocks behind where the player is facing. (A player's velocity
	 * isn't reliable on the server, since clients move themselves, so we go by their facing.)
	 */
	public static Vec3d behind(ServerPlayerEntity player, double distance) {
		Vec3d dir = player.getRotationVector().multiply(1, 0, 1);
		if (dir.lengthSquared() < 1.0e-4) dir = new Vec3d(0, 0, 1);
		return player.getPos().subtract(dir.normalize().multiply(distance));
	}
}

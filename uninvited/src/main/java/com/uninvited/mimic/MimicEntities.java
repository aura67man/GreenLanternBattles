package com.uninvited.mimic;

import com.uninvited.Uninvited;
import com.uninvited.config.UninvitedConfig;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class MimicEntities {
	public static final EntityType<MimicEntity> MIMIC = Registry.register(Registries.ENTITY_TYPE, Uninvited.id("mimic"),
			EntityType.Builder.create(MimicEntity::new, SpawnGroup.MONSTER)
					// Same box and eye height as a player, so it moves and collides like one.
					.dimensions(0.6f, 1.8f)
					.eyeHeight(1.62f)
					// Visible from far away (stage 2 stares from the edge of render distance).
					// The server's view distance still caps this.
					.maxTrackingRange(10)
					.trackingTickInterval(2)
					// Never written to disk: a mimic that survived a restart would break the
					// "one at a time" bookkeeping and would wake up with no target.
					.disableSaving()
					.build("mimic"));

	private MimicEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createMimicAttributes(UninvitedConfig.get()));
	}
}

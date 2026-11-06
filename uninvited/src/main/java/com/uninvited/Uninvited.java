package com.uninvited;

import com.uninvited.command.UninvitedCommands;
import com.uninvited.config.UninvitedConfig;
import com.uninvited.stage.StageTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Uninvited implements ModInitializer {
	public static final String MOD_ID = "uninvited";
	public static final Logger LOG = LoggerFactory.getLogger("UNINVITED");

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		UninvitedConfig.load();
		StageTracker.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				UninvitedCommands.register(dispatcher));
		LOG.info("UNINVITED loaded. Stay together.");
	}
}

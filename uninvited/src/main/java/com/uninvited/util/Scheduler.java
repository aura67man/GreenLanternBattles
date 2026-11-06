package com.uninvited.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny "do this N ticks from now" helper (closing a door a moment after opening it, etc). */
public final class Scheduler {
	private record Task(long dueTick, Runnable action) {
	}

	private static final List<Task> TASKS = new ArrayList<>();
	private static long now;

	private Scheduler() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Scheduler::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> TASKS.clear());
	}

	public static void runLater(int ticks, Runnable action) {
		TASKS.add(new Task(now + Math.max(1, ticks), action));
	}

	private static void tick(MinecraftServer server) {
		now = server.getTicks();
		if (TASKS.isEmpty()) return;
		List<Task> due = new ArrayList<>();
		for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
			Task task = it.next();
			if (task.dueTick() <= now) {
				due.add(task);
				it.remove();
			}
		}
		for (Task task : due) task.action().run();
	}
}

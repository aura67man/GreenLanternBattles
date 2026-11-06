# UNINVITED

A multiplayer horror mod for Minecraft 1.21.1 (Fabric), for 2–6 friends on a server.

Something joins the world that isn't on the player list. It learns what you look like, how you talk and where you go, then it pretends to be one of you. It only goes after players who are **alone**. Stay together.

The mimic is drawn with the vanilla player model wearing a real player's skin, so the mod needs almost no custom art.

## Versions

| Component | Version | Notes |
|---|---|---|
| Minecraft | 1.21.1 | |
| Fabric Loader | 0.20.1 | latest stable for 1.21.1 |
| Fabric API | 0.116.17+1.21.1 | |
| Yarn | 1.21.1+build.3 | |
| Fabric Loom | 1.17.21 | plugin id `net.fabricmc.fabric-loom-remap` (see notes) |
| Gradle | 9.8.2 (wrapper) | Loom 1.17 needs Gradle 9.5+ |
| Java | 21 | |
| Simple Voice Chat API | 2.6.27 (`compileOnly`) | optional at runtime |

Install the mod on **both** the server and every client (custom entity, items and blocks). Fabric API is required. Simple Voice Chat is optional.

## Building

```bash
cd uninvited
./gradlew build
```

The mod jar is written to `build/libs/uninvited-<version>.jar`.

## How it plays

**The rule:** a player is *alone* when no other living, non-spectator player is within `aloneRadius` (24) blocks in the same dimension. Every scare, appearance and attack goes through `AloneUtil`. Groups are safe.

**Escalation:** one stage every `nightsPerStage` (2) in-game nights, counted from when the mod first ran in the world. The counter is saved in `data/uninvited.dat`. A night counts when beds start working (tick 12542). A big time skip counts as one night at most, and turning time back never lowers the count.

| Stage | Nights | What happens to lone players |
|---|---|---|
| 1 Settling in | 1–2 | Footsteps behind you that stop when you stop (then one more step). A wooden door or trapdoor nearby opens and shuts. A chest or barrel in your base opens and closes. A torch you aren't looking at drops off the wall. Sounds are pitched down and only you hear them. |
| 2 Watching | 3–4 | The mimic appears near the edge of view distance wearing a friend's skin. It stares, and vanishes if you look straight at it for about a second or get within 20 blocks. Once a night, a stray block of dirt appears on the floor inside your base while you're away. |
| 3 Becoming | 5–6 | It walks around as a copy of a friend, follows you at 14–28 blocks from behind, and waves when you notice it. Fake chat starts. With voice mimicry on, it may speak. |
| 4 Hunting | 7+ | It stalks you, walks up openly like a friend would, pauses, then charges. After 2 hits its next reach is the grab: you are **Taken** (see below). It flees instead if you hurt it enough or reach your friends. |

During the day everything is scaled by `daytimeActivity` (0.25).

**Taken (the hunt's ending):** when the hunter catches you, it doesn't kill you.

1. The screen goes black and you can't move.
2. You wake up alone, 250–450 blocks away, preferably in a cave (in a superflat world there are no caves, so it's the surface), under Darkness. *"You don't remember walking here."*
3. Meanwhile a mimic wearing **your** face appears behind the friend nearest to where you were taken, walks up and tags along with them. It chats in your name (your real old lines and some new ones like "dont listen to them"), and with voice mimicry on it uses your voice too. If nobody else is online, it waits for you at home instead.
4. It ends when someone exposes it (Witness Bell, or punching it, which a real player would just take), when you get back within 12 blocks of it (it turns, stares at you for two seconds and comes apart), or after `impostorMinutes`.

While you're taken, the director leaves you alone. Set `takenEnabled` to false for the old ending (a few hits, then it runs).

**Tells** (things players can learn to spot):

- One character of the name tag is a lookalike (`i`→`l`, `o`→`0`, `m`→`rn`, or a Cyrillic letter). It is stable per impersonated player per world.
- It holds the wrong item, or nothing.
- Exactly one armour piece differs from the real player's.
- It makes no footstep sounds.
- It never jumps. It can still step up one block.
- Before stage 4 it simply isn't there when you hit it.
- Fake chat lines are system messages. On a server with `enforce-secure-profile=false`, real chat shows a "Not Secure" marker and fake lines don't.

**Counterplay:**

- **Salt** (bone meal + sand → 4 salt): place it like redstone dust. The mimic's pathfinding treats salt as a wall, so it can't walk across a line or stand on it, and a closed ring is a safe zone. It won't swing at you across a salt line either. If it's knocked onto salt, it comes apart.
- **Witness Bell** (string, 4 gold nuggets, gold ingot):

  ```
   S
  NGN
  N N
  ```

  Ring it and every real player within 48 blocks glows for 5 seconds. Anything that looks like a player but doesn't glow is not your friend. A mimic in range flickers and vanishes. 5-minute cooldown, shown on the hotbar.

## Config: `config/uninvited.json`

The file is created on first start. Edit it while the server is stopped, or run `/uninvited reload`.

| Key | Default | Meaning |
|---|---|---|
| `aloneRadius` | 24.0 | blocks; nobody within this radius = alone |
| `nightsPerStage` | 2 | nights per escalation stage |
| `daytimeActivity` | 0.25 | multiplier for all chances during the day |
| `spawnChancePerMinute` | 0.6 | per lone player at night, stage 2+ |
| `scareChancePerMinute` | 1.0 | per lone player at night, stage 1+ (30 s cooldown) |
| `secondsBetweenAppearances` | 90 | minimum gap between mimic appearances per world |
| `maxMimics` | 1 | live mimics per world |
| `mimicDamage` | 4.0 | per hit (2.0 = one heart) |
| `mimicAttackCooldownTicks` | 30 | between hits |
| `mimicHealth` | 30.0 | |
| `mimicFleeHealthFraction` | 0.5 | flees after losing this much health |
| `takenEnabled` | true | the hunt ends in an abduction instead of just hits |
| `hitsBeforeTaken` | 2 | hits it lands before the grab |
| `takenDistanceMin` / `takenDistanceMax` | 250 / 450 | how far away a taken player wakes up |
| `impostorMinutes` | 8 | how long the impostor lasts if nobody exposes it |
| `impostorChatSeconds` | 45 | average gap between its chat lines |
| `fakeChat` | true | |
| `fakeChatChancePerMinute` | 0.35 | stage 3+ (90 s cooldown per player) |
| `bellCooldownSeconds` | 300 | |
| `bellRadius` | 48.0 | |
| `bellGlowSeconds` | 5 | |
| `voiceMimicry` | **false** | operator opt-in, needs Simple Voice Chat |
| `voiceChancePerMinute` | 0.5 | stage 3+, while a mimic is near a lone player |

## Debug commands (operator, permission level 2)

| Command | Does |
|---|---|
| `/uninvited stage <1-4>` | force a stage (saved with the world) |
| `/uninvited stage auto` | go back to the night counter |
| `/uninvited summon <player> [idle\|watch\|follow\|hunt\|flee]` | spawn a mimic disguised as `<player>`, 5 blocks in front of you. It targets you. With no mode, it uses the current stage's behaviour. |
| `/uninvited scare <player> [type]` | run a random stage-appropriate scare, or a specific one: `footsteps door chest torch odd_block watcher fake_chat voice follower hunter`. Ignores the alone check and cooldowns. |
| `/uninvited status` | stage, nights, day/night, every live mimic (disguise, shown name, mode, target, position), voice status, who is alone and who is grouped |
| `/uninvited take <player>` | take that player right now (blackout, teleport, impostor) |
| `/uninvited despawn` | remove all mimics |
| `/uninvited reload` | reload the config (turning voice off wipes all voice buffers immediately) |

## Testing it, milestone by milestone

Dev runs use `run/` for the server and the client. For a real multiplayer test, run a server and connect one or more clients to `localhost`.

```bash
./gradlew runServer
```

```bash
./gradlew runClient
```

The dev server is in online mode by default. For several local clients, set `online-mode=false` in `run/server.properties`, then give each client a username: `./gradlew runClient --args="--username Alice"` (and `Bob`, etc.). Two clients running from the same `run/` folder can trip over each other's files, so if that happens, run the second one from a copy of the project.

1. **Setup, stages, alone check.** `/uninvited status` shows stage 0 by day. `/time set 13000` makes night 1 and stage 1. `/time add 24000` twice gives night 3 and stage 2. Stand near a friend: you move from "Alone" to "In a group" once you're within 24 blocks. `/uninvited stage 4`, restart the server, and the stage is still 4.
2. **Mimic and skins.** `/uninvited summon <your name> idle`. You get yourself, in your skin (slim or wide arms to match), with a name like `Te5ter`, the wrong item and one wrong armour piece. Walk it into a ledge: it steps up one block, never jumps, and makes no footstep sounds. Summon a friend who has logged off: it still wears their skin.
3. **AI.**
   - `/uninvited stage 2`, then `/execute at @s positioned ^ ^ ^-40 run uninvited summon <you> watch`. Turn round and look at it: it's gone in about a second.
   - `stage 3` plus `summon <you> follow`. It keeps behind you, and when you turn it stops and waves. Walk at it and it vanishes.
   - `stage 4` in survival plus `summon <you> hunt`. It stalks for 8–23 s, walks up, pauses, charges, hits twice, then grabs you. Have a friend come within 24 blocks of you and it runs instead.
   - **Taken** (two players): with a friend online, `/uninvited take <you>`. You black out and wake up far away, and your friend sees "you" walk up behind them and start chatting. `/uninvited status` shows `Taken: <you> (impostor with <friend>)`. `/tp` back next to it and it turns, stares and comes apart. Alone, the impostor appears in your base when you get within 40 blocks of home.
4. **Scares and fake chat.** Put a wooden door, a chest and a couple of torches near you, then `/uninvited scare <you> door`, `chest`, `torch`, `footsteps` (walk, then stop). For fake chat, have a friend type something like `come here`, then `/uninvited stage 3` and `/uninvited scare <you> fake_chat`. You'll see `<Friend> come here` and they won't.
5. **Salt and bell.** Craft both (or `/give @s uninvited:salt 64` and `/give @s uninvited:witness_bell`). Ring yourself with salt at least 2 blocks out, then `stage 4` and `summon <you> hunt` outside the ring: it circles but can't get in. Summon one in `follow` mode and ring the bell: you glow, it flickers out, and the bell shows its cooldown.
6. **Voice (optional).** Install [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) for 1.21.1 on the server and the clients. Set `"voiceMimicry": true` in `config/uninvited.json` and restart (or `/uninvited reload`). Everyone gets the notice in chat. Talk or laugh for a bit, `stage 3`, `summon <friend> follow`, then `/uninvited scare <you> voice`. The friend's voice comes from the mimic, a little slower and lower, and only you hear it. Without SVC, `/uninvited status` says voice is unavailable and nothing else changes.

## Voice mimicry and privacy

This feature is off by default, and only someone who can edit the server config (or run the op-only `/uninvited reload`) can turn it on.

- **Capture:** `MicrophonePacketEvent`. Each Opus frame is decoded once, only to measure its loudness, and the original Opus bytes are kept. Phrases are split on 240 ms of silence or when push-to-talk is released, and capped at 4 s. Clips that are quiet (RMS < 700) or under 0.5 s are dropped. Each player keeps about 3 minutes of clips. When over budget, the quietest of the older clips go first, so shouts and laughs last longest. Players talking in an SVC group are never captured.
- **Playback:** a 1–3 s window, slowed and lowered about 10%, played through an `EntityAudioChannel` on the mimic with `setFilter` so only the target hears it. Rarely, it plays the target's own voice back from just behind them. That uses a `LocationalAudioChannel`, because it has to come from behind the player, not from the mimic.
- **Storage:** memory only. Nothing is ever written to disk. A player's audio is wiped when they log out, everything is wiped when the server or voice server stops, and switching `voiceMimicry` off wipes everything.
- **Notice:** while it's on, every player sees a chat notice each time they join. It's shown on every join rather than only the first time, so nobody misses that it's active.

**How SVC stays optional:** it's a `compileOnly` dependency. Every class that names an SVC type lives in `com.uninvited.voice`, and that package is only reached through the `voicechat` entrypoint in `fabric.mod.json`. Only SVC asks Fabric for that entrypoint, so without SVC those classes are never loaded. The rest of the mod talks to `voicebridge.VoiceBridge` / `VoiceBackend`, which have no SVC types and are additionally guarded by `FabricLoader.isModLoaded("voicechat")`.

## Code map

```
com.uninvited
├── Uninvited                  main entrypoint
├── config/UninvitedConfig     config/uninvited.json
├── stage/                     Stage, UninvitedState (PersistentState), StageTracker (night counter)
├── util/AloneUtil             THE alone rule
├── util/Scheduler             run-later helper
├── command/                   /uninvited ...
├── mimic/                     MimicEntity, disguise, tells, homoglyphs, spawning, placement, senses
│   └── ai/                    Watch / Follow / Lure / Hunt / Flee goals, MimicBrain, MimicNavigation
├── director/                  once-a-second director: appearances, base meddling, voice timing
├── scare/                     footsteps, doors, chests, torches, private sounds
├── chat/                      ChatMemory (RAM only), FakeChat
├── salt/                      SaltBlock, SaltRules, SaltAvoidingNodeMaker (pathfinding exclusion)
├── item/                      ModItems, WitnessBellItem
├── taken/                     Taken (the grab), TakenDestination (where you wake up), TakenTracker (the impostor)
├── voicebridge/               SVC-free interface to the optional voice integration
└── voice/                     ALL Simple Voice Chat code (plugin, capture buffer, DSP, playback)
client (src/client)
└── client/render/             MimicEntityRenderer, MimicModelRenderer (wide/slim), MimicSkins
```

## Implementation notes and API differences from the brief

- **Loom:** the newest Loom (1.18+/1.19) needs Java 25 to run Gradle. To stay on Java 21 this uses Loom 1.17.21, the newest line that runs on Java 21, with Gradle 9.8.2. Loom now has two plugin ids. `net.fabricmc.fabric-loom-remap` is the one for obfuscated versions like 1.21.1 that use Yarn.
- **1.21 data paths** are singular (`data/uninvited/recipe/`, `data/uninvited/loot_table/`), and recipe results use `"id"`.
- **Skins:** resolved from the tab list (`PlayerListEntry#getSkinTextures`). For players who are offline, the server stores their public, Mojang-signed `textures` property and syncs it on the mimic, and the client loads it through `PlayerSkinProvider#getSkinTextures(GameProfile)`. After that it falls back to a skin seen earlier in the session, then `DefaultSkinHelper`. Slim vs. wide comes from `SkinTextures#model()`. Like vanilla, the renderer keeps one sub-renderer per arm model.
- **Glow:** `Entity#setGlowing` is final and controls the shared flag, so the bell applies the vanilla Glowing status effect instead.
- **Fake chat:** signed chat can't be forged, so it's a system message using the vanilla `chat.type.text` translation, with the same name hover and click-to-`/tell` as a real chat name.
- **Salt pathfinding:** `LandPathNodeMaker#getCollidingNodeTypes` asks `getDefaultNodeType` about every block the mob's box covers. Salt returns `BLOCKED` there, so A* never expands a node whose feet block is salt. See `SaltAvoidingNodeMaker` for details.
- **Mimics are never saved** (`disableSaving()`), so a restart can't leave an orphaned mimic behind.
- **Chat memory** persists past logout (in RAM) so the mimic can quote friends who have left. It's cleared on server stop. Audio, by contrast, is wiped on logout, as required.

## Later (not built, but there's room for it)

- **Whispers / ghosts:** add new `ScareType`s and a ghost mode. `PrivateSounds` already sends positioned sounds to one player, so fake knocks are the same mechanism.
- **Fake tab-list entry:** send a `PlayerListS2CPacket` for the disguise to the target only, during `Stage.HUNTING`.
- **Candle of Names:** a block that checks `MimicSpawner.activeMimics(world)` for any mimic within 32 blocks.
- **Vignette:** sync "mimic is near" to the client (custom payload or a tracked flag on the mimic) and draw it with `HudRenderCallback`.

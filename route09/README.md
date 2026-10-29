# The Last Bus: Route 09

A first-person story horror game in one self-contained `index.html`. Open it in a modern desktop browser (Chrome, Edge, Firefox or Safari) and play. It needs no server, no install and no downloads. Everything is inside the file: three.js (MIT licence) is inlined, every texture and drawing is generated with Canvas, and every sound is synthesised with the Web Audio API.

You play Alex Morgan. Eight years after your younger brother Noah vanished during a storm, an unknown number texts you: *"If you want Noah back, take Route 09."*

## How to play

1. Open `route09/index.html` in a browser. A local file works fine. Headphones are recommended.
2. Click **New Game**, then click the game view to capture the mouse.
3. A full playthrough takes roughly 25–40 minutes. The game saves at every chapter checkpoint, and **Continue** resumes from the latest one.

## Controls

| Key | Action |
| --- | --- |
| Mouse | Look (click the view to capture the mouse; if capture fails, drag to look) |
| W A S D / Arrow keys | Move (← → also turn) |
| Shift | Sprint (uses stamina, makes noise) |
| C / Ctrl | Crouch (C toggles, Ctrl holds). You're quieter, and you can fit through low hatches |
| E | Interact, inspect, hide, leave a hiding place, advance dialogue |
| F | Flashlight (your phone light before you find one) |
| Tab / I | Inventory and journal |
| M | Road map (once found) |
| H | Use a first-aid kit |
| Space | Advance dialogue. Hold it to fast-forward a cut-scene, or to hold your breath while hiding |
| Esc | Pause and release the mouse |

## What's in it

- A prologue and six chapters: the highway, the bus, the Last Stop diner, the town of Greywood, Greywood Elementary, the emergency shelter, and the tunnel under the road.
- **The Conductor** runs on a state-machine AI with idle, patrol, investigate, alert, chase, search and lost states. It sees with a vision cone and line of sight, notices your flashlight, hears running, and searches hiding places near where it lost you. If it saw you climb into one, it checks that one first. A red eye in the top-right corner shows how aware of you it is.
- Hiding places: lockers, cupboards, wardrobes, and the spaces under seats, beds and bunks. Hold Space to hold your breath if it checks your spot.
- Chases: the bus escape, the diner, the Greywood street, the school (library → classrooms → lockers → basement), and the final chamber.
- Puzzles: the diner fuse panel, the classroom clock and calendar, the library terminal, the gym emergency lights, the shelter generator and override code, the memory console, the seven candles (optional), and the seven emergency lights.
- Four endings: **A — The Wrong Passenger**, **B — Leave Noah Behind**, **C — Bring Him Home**, and a hidden **D — The Seat Beside You**.
- An optional journal of 15 notes and 3 secret recordings. Collecting all of them is part of how you unlock ending D.
- Health, stamina and flashlight battery with spare batteries. Checkpoints top the battery up.
- Automatic hints when you're stuck (these can be turned off). Talking to Mia also gives hints.
- Settings: mouse sensitivity, invert Y, master, music and effects volume, brightness, render scale, camera shake, reduced flashing, ambient subtitles, dynamic shadows and automatic hints.

## Saves and resetting

Progress is saved to `localStorage` at chapter checkpoints. Saved data includes your chapter, items, story flags, decisions, journal, unlocked endings and settings. Monster positions and cut-scene state are never saved.

To reset, use **Settings → Erase saved progress** (this keeps your settings), or start a **New Game**, which replaces your checkpoint and journal but keeps unlocked endings. You can also clear the site data for the page in your browser.

## Known limitations

- The art style is deliberately low-poly and stylised: blocky characters and procedural textures, rendered at a reduced resolution by default for a retro look. You can raise **Render scale** in Settings.
- There is no recorded voice acting. All dialogue is subtitled text with synthesised sound cues, so story dialogue is always shown. The subtitle setting only controls ambient whispers.
- It needs WebGL and is designed for keyboard and mouse. Touch devices aren't supported.
- Ctrl works as crouch, but Ctrl+W closes the tab in most browsers, so C is the safer choice.
- Pointer lock can be refused inside some embedded frames. The game then falls back to click-and-drag looking.

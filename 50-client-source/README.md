# Aurora

A smooth, animated client menu for Minecraft 1.21.11 (Fabric) with a HUD and radar. Built for single-player worlds.

## Build it

You need **Java 21**.

```
./gradlew build          # Windows: gradlew.bat build
```

The jar is created in `build/libs/` (use `aurora-1.0.0.jar`, not the `-sources` one).

To test without installing anything: `./gradlew runClient`.

To install it: drop the jar into your `mods` folder next to **Fabric Loader** and **Fabric API** for 1.21.11.

If Gradle says a version does not exist, open `gradle.properties` and copy the current numbers for 1.21.11 from https://fabricmc.net/develop/.

## Use it

Press **Right Shift** to open the menu, and again to close it. You can change the key under Options > Controls > Key Binds > Aurora.

- Left click a module to turn it on or off
- Right click a module to open its settings
- Drag a panel by its header to move it
- Right click a panel header to collapse it
- Hover a module to see what it does

Your choices are saved to `config/aurora.properties`.

## What is included

| Panel | Module | What it does |
|---|---|---|
| Misc | Sprint | Sprints automatically while you walk forward |
| Misc | Fast Place | Removes the delay between placing blocks |
| Misc | Auto Tool | Picks the fastest hotbar tool when mining |
| Misc | Auto Eat | Eats safe food when hunger drops below a setting |
| Render | Fullbright | Maximum brightness everywhere |
| Render | Entity ESP | Outlines players, hostile or passive mobs through walls |
| Render | HUD | FPS, clock, ping, ticks and speed pills |
| Render | Radar | Round radar with a rotating N, players and mobs |
| Client | Interface | Animation speed and background dim |

## Adding a module

1. Create a class in `dev/aurora/module/impl/` that extends `Module`.
2. Add settings with `add(new BooleanSetting(...))`, `NumberSetting` or `ModeSetting`.
3. Put your logic in `onTick()`, and clean up in `onDisable()`.
4. Register it in `ModuleManager.registerDefaults()`.

It then shows up in the menu with its settings, animations and saving already working.

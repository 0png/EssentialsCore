# EssentialsCore Agent Guide

## Communication
- Communicate with the user in Traditional Chinese.
- Keep code, identifiers, commit messages, and technical documentation in English.
- State assumptions briefly and report verification results clearly.

## Project
- EssentialsCore is a lightweight, GUI-first survival plugin for Paper 26.2.
- Use Java 25, Gradle Kotlin DSL, and `paper-api:26.2.build.87-stable`.
- The base package is `dev.zeropng.essentialscore`.
- PlaceholderAPI is optional; never make the plugin fail when it is absent.
- Do not add RTP unless the user explicitly restores it to scope.

## Architecture
- Keep the current lightweight package structure and avoid unnecessary frameworks or layers.
- Prefer focused managers, listeners, commands, and GUI classes over repository/service abstractions.
- Keep Bukkit/Paper API access on the main thread unless the API explicitly supports async use.
- Use non-blocking chunk loading and teleport APIs where appropriate.

## GUI and Input
- Use custom `InventoryHolder` types for plugin GUIs.
- Cancel all item-taking paths, including clicks, drags, hotbar swaps, and shift-clicks.
- Fill unused GUI slots with gray stained glass panes, except `/trash` item-storage slots.
- Open replacement inventories on the next server tick.
- Use `ChatInputManager` for private text input; support `cancel`, validation retries, and timeout.

## Data and Localization
- Preserve compatibility with existing `config.yml`, `data.yml`, `ranks.yml`, and `warps.yml` files.
- Save important mutations immediately and use the existing atomic file utilities.
- Never delete valid player data when lowering limits or changing defaults.
- Add every message to both `lang/zh_TW.yml` and `lang/en.yml`.
- Preserve server-customized translations and keep bundled-language fallback working.

## Quality
- Add or update tests for behavior changes and regressions.
- Run `.\gradlew.bat clean check build` before declaring work complete.
- Keep `/rtp` unregistered and update `PluginDescriptorTest` when commands or versions change.
- Build artifacts belong in `build/libs`; do not commit generated build output.
- Use Conventional Commits when the user asks for commits, and never push without authorization.

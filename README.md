# EssentialsCore

[![CI](https://github.com/0png/EssentialsCore/actions/workflows/ci.yml/badge.svg)](https://github.com/0png/EssentialsCore/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/0png/EssentialsCore)](https://github.com/0png/EssentialsCore/releases/latest)
[![Paper](https://img.shields.io/badge/Paper-26.2-2c2f33)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-25-f89820)](https://adoptium.net/)

EssentialsCore is a lightweight, GUI-first survival utility plugin for Paper. It provides Homes,
teleport requests, public Warps, display-only Ranks, pet management, death Back, and several small
quality-of-life features without introducing an economy, permission framework, or database layer.

**[Download the latest release](https://github.com/0png/EssentialsCore/releases/latest)**

## Requirements

- Paper 26.2
- Java 25
- PlaceholderAPI 2.12.3 or newer (optional, only for Rank placeholders)
- TAB (optional, can consume the PlaceholderAPI Rank prefix)

PlaceholderAPI and TAB are soft integrations. EssentialsCore loads normally when either plugin is
absent.

## Installation

1. Download `EssentialsCore-<version>.jar` from the
   [latest GitHub Release](https://github.com/0png/EssentialsCore/releases/latest).
2. Optionally verify the JAR against the attached `.sha256` file.
3. Place the JAR in the Paper server's `plugins` directory.
4. Start or restart the server.
5. Run `/ec` for the player menu or `/ec admin` as an OP for administration.

Existing configuration and player data are kept in the `plugins/EssentialsCore` directory.

## Features

### GUI-first essentials

- A 27-slot `/ec` main menu for Homes, player teleporting, requests, pets, and Rank information.
- Paginated player, request, Home, pet, Warp, and Rank management inventories.
- Managed GUI slots are protected against clicks, drags, hotbar swaps, shift-clicks, and item theft.
- Unused menu slots use gray stained glass panes; `/trash` remains a usable storage inventory.
- Private chat input with validation, `cancel`, and a 60-second timeout.
- Server-wide Traditional Chinese (`zh-TW`) and English (`en`) localization.

### Homes and teleport requests

- Named, cross-world Homes with GUI creation, teleporting, and confirmed deletion.
- Configurable Home limit, cooldown, and teleport delay.
- `/tpa` and `/tpahere` player selectors with clickable chat accept/deny controls.
- Request GUI, expiry, send cooldown, and post-accept teleport delay.
- Delayed teleports cancel when the teleported player moves, changes world, takes valid damage, or
  disconnects.

### Warps, Back, and Trash

- Public Warp GUI with OP creation, rename, icon, position, deletion, cooldown, and delay controls.
- Persistent `/back` destination for the player's most recent death location.
- Death title reminding the player that `/back` is available.
- Safe `/trash` inventory: closing returns items, while explicit confirmation permanently deletes
  them.

### Ranks

- Display-only Owner, Admin, Moderator, VIP, and Member defaults.
- Full OP GUI and command management for Rank creation, editing, deletion, default selection, and
  player assignment.
- Rank prefixes in normal chat and player nametags.
- Optional PlaceholderAPI support for TAB and other compatible plugins.
- No gameplay restrictions or permissions are attached to a Rank.

### Pets and quality-of-life features

- Index and recall owned tameable pets, including cross-world recall from recorded chunks.
- Bulk recall, sit, and stand controls from the pet GUI.
- Pet Protection blocks traceable damage from other players' melee attacks, arrows, tridents,
  harmful splash potions, attributable TNT, and pets owned by another player.
- Optional universal lead support for non-player living entities; fence attachment still works, and
  entities directly leashed to a player follow that player's teleports.
- Optional empty-hand Shift-right-click pet affection effect.
- `/sit` toggle for sitting on stairs and single slabs, `/lay` for lying down, and `/hat` for wearing
  the main-hand item.

## Player commands

| Command | Description |
| --- | --- |
| `/ec` | Open the main menu. |
| `/ec help` | Open the in-game command help menu. |
| `/home` | Open the Home menu. |
| `/sethome` | Start private Home-name input at the current location. |
| `/tpa` | Select a player and request to teleport to them. |
| `/tpahere` | Select a player and invite them to teleport to you. |
| `/tpaccept [player]` | Accept the newest request, optionally from a named player. |
| `/tpdeny [player]` | Deny the newest request, optionally from a named player. |
| `/pet` | Open the pet recall and bulk-control menu. |
| `/warp [name]` | Open the public Warp menu or use a named Warp. |
| `/back` | Return to the most recent death location. |
| `/trash` | Open the safe disposal inventory. |
| `/rank` | Open the player's read-only Rank information GUI. |
| `/sit` | Toggle right-click sitting on stairs and single slabs. |
| `/lay` | Lie down; repeat, sneak, move, or take damage to stand up. |
| `/hat` | Move the main-hand item into the helmet slot. |

RTP is intentionally outside the current release scope, and EssentialsCore does not register
`/rtp`.

## OP administration

`/ec admin` opens the administration menu for:

- Home limit, cooldown, and teleport delay.
- TPA request expiry, send cooldown, and teleport delay.
- Public Warp management and teleport settings.
- Global Pet Protection.
- Experimental universal lead and pet affection toggles.

Rank management is available through the Rank GUI and these OP/console commands:

```text
/rank create <id> <display name>
/rank edit <id> name|prefix|color <value>
/rank set <player> <id>
/rank default <id>
/rank delete <id> confirm
/rank list
/rank info [player]
```

OPs receive a private EssentialsCore version and administration reminder when joining. OP status
does not bypass Home or TPA cooldowns and does not bypass Pet Protection.

## Default configuration

| Setting | Default |
| --- | ---: |
| Home limit | 3 |
| Home cooldown | 30 seconds |
| Home teleport delay | 3 seconds |
| TPA request expiry | 60 seconds |
| TPA send cooldown | 30 seconds |
| TPA teleport delay | 3 seconds |
| Warp cooldown | 30 seconds |
| Warp teleport delay | 3 seconds |
| Pet Protection | Enabled |
| Universal lead | Disabled |
| Pet affection | Disabled |
| Private input timeout | 60 seconds |

The global language is selected with `language: zh-TW|en`; changing it takes effect after a server
restart.

## PlaceholderAPI

```text
%essentialscore_rank%
%essentialscore_rank_prefix%
```

Example TAB format:

```text
%essentialscore_rank_prefix%%player%
```

A different plugin that manages scoreboard teams may override EssentialsCore's nametag prefix.

## Data files

- `config.yml`: language, Home, TPA, Warp, Pet Protection, input, and experimental settings.
- `data.yml`: Homes, player Rank assignments, death Back locations, and the pet index.
- `ranks.yml`: Rank definitions and the default Rank.
- `warps.yml`: public Warp locations and GUI icons.
- `lang/zh_TW.yml` and `lang/en.yml`: localized GUI and chat messages.

EssentialsCore uses YAML with atomic replacement; it does not require SQLite or an external
database.

## Building from source

Use the included Gradle 9.6.1 Wrapper with a Java 25 JDK:

```bash
./gradlew clean check build
```

Windows PowerShell:

```powershell
.\gradlew.bat clean check build
```

The plugin JAR is written to `build/libs/EssentialsCore-<version>.jar`.

## Development and releases

- Pull requests run Java 25 compilation, all tests, Gradle Wrapper validation, and artifact
  packaging.
- PR titles follow Conventional Commits, for example `feat: add afk support` or
  `fix(home): cancel an invalid teleport`.
- Release Please maintains the release PR, `version.txt`, and `CHANGELOG.md`.
- Merging a release PR creates the version tag and GitHub Release, then uploads the verified JAR
  and SHA-256 file.
- Dependabot checks Gradle and GitHub Actions dependencies weekly.

See [RELEASING.md](docs/RELEASING.md) for the maintainer workflow and
[CHANGELOG.md](CHANGELOG.md) for released changes.

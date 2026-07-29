# Changelog

All notable changes to EssentialsCore are documented in this file. Future entries are generated
automatically from Conventional Commits by Release Please.

## [1.4.1](https://github.com/0png/EssentialsCore/compare/v1.4.0...v1.4.1) (2026-07-29)


### Continuous Integration

* add automated build and release pipeline ([20b8af0](https://github.com/0png/EssentialsCore/commit/20b8af0ae1df441fa515e8045f836c7cb8946957))

## [1.4.0] - 2026-07-28

### Added

- Added complete GUI-based Rank creation, editing, deletion, default selection, and player assignment.
- Added public Warp management and teleport GUI, a safe Trash GUI, `/ec help`, and persistent `/back` death locations.
- Added `/sit`, `/lay`, and `/hat`, plus configurable universal leashing and pet interaction effects.
- Added pet-wide sit, stand, and recall controls, including leashed-entity teleport support.
- Added Rank prefixes to chat and player nametags, with optional PlaceholderAPI integration.

### Changed

- Filled managed menu backgrounds with gray stained glass panes while keeping the Trash inventory usable.
- Improved Home limit administration, sitting position recovery, Traditional Chinese and English localization, and OP join information.

### Fixed

- Fixed chat buttons resolving the wrong TPA request.
- Fixed Rank creation persistence and missing localization keys appearing as raw key names.

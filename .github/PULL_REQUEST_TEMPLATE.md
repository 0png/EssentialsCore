## Summary

Describe what changed and why.

## Verification

- [ ] `./gradlew clean check build` passes on Java 25.
- [ ] Player-visible text is present in both `zh_TW.yml` and `en.yml`.
- [ ] Relevant GUI and Paper behavior was manually checked when needed.

Use a Conventional Commit PR title such as `feat: add afk support` or
`fix(home): preserve the destination after restart`. The squash-merge commit uses this title and
drives the automatic version bump and changelog.

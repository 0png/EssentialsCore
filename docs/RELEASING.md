# Release workflow

EssentialsCore uses GitHub Actions and Release Please. Normal development never edits the version
or changelog by hand.

## One-time repository settings

In **Settings → Actions → General**, enable **Read and write permissions** and allow GitHub Actions
to create and approve pull requests. The workflow uses the built-in `GITHUB_TOKEN`; no credential is
stored in the repository.

For fully automatic checks on Release Please's own pull request, create a fine-grained token named
`RELEASE_PLEASE_TOKEN` in **Settings → Secrets and variables → Actions**. Give it repository
permissions for Contents, Issues, and Pull requests (read/write). Without this optional secret,
releases still work, but GitHub does not trigger another workflow from a PR created by the built-in
token.

Protect `main`, require pull requests, prefer squash merging, and require these checks:

- `Build and test`
- `Validate title`

## Development flow

1. Open a pull request whose title follows Conventional Commits.
2. Let CI build and test with Temurin Java 25. It also validates the Gradle Wrapper and uploads a
   temporary JAR plus SHA-256 checksum.
3. Squash-merge the pull request after checks pass.
4. Release Please opens or updates one release pull request containing the next `version.txt`,
   `.release-please-manifest.json`, and generated `CHANGELOG.md` changes.
5. Merge the release pull request when ready. The release workflow rebuilds from a clean checkout,
   confirms the Gradle version and tag match, creates the GitHub Release, and uploads the JAR and
   checksum.

## Version rules

- `fix:` and `perf:` produce a patch release, such as `1.4.0` → `1.4.1`.
- `feat:` produces a minor release, such as `1.4.0` → `1.5.0`.
- `feat!:` or a `BREAKING CHANGE:` footer produces a major release.
- `docs:`, `test:`, `build:`, `ci:`, and `chore:` alone do not normally start a release.

Scopes are optional. Examples:

```text
feat(pet): add a pet status filter
fix(home): cancel teleport after world change
docs: clarify Paper installation
```

To request an exact exceptional version, add this footer to a Conventional Commit:

```text
Release-As: 2.0.0
```

Do not manually tag releases or edit `version.txt` during normal development. If the release asset
upload fails after a GitHub Release was created, rerun the failed Release workflow; asset upload is
idempotent and uses `--clobber`.

# Building from Source

jdbgen is a plain Gradle project with no external tooling requirements beyond a JDK — the Gradle wrapper takes care of Gradle itself. This page covers the build commands, what each one produces, how the source tree is organised, and the steps for cutting a release.

[← Documentation index](README.md)

## Requirements

| | |
|---|---|
| JDK | **11 or newer.** The continuous integration build uses Temurin JDK 11. Because every `JavaCompile` task sets `options.release = 11`, newer JDKs also work and still produce Java 11 bytecode — a `clean build` on JDK 17 has been verified. Building the Windows installer needs **JDK 14 or newer**, see [The Windows installer](#the-windows-installer). |
| Gradle | Not needed. `gradlew`/`gradlew.bat` download Gradle 8.14.3 on first use. |
| Network | Required for the first build, to fetch Gradle and the dependencies from Maven Central. |

The project uses [Lombok](https://projectlombok.org/) as an annotation processor. That is handled by Gradle, but your IDE needs the Lombok plugin to resolve the generated getters and setters.

## Quick start

```bash
git clone https://github.com/xcomart/jdbgen.git
cd jdbgen
./gradlew build
```

On Windows use `gradlew.bat` in place of `./gradlew`.

## Gradle tasks

| Command | Result |
|---|---|
| `./gradlew build` | Compiles, runs the tests and assembles. `assemble` depends on `distZip`, so a full build also produces the distribution archive. |
| `./gradlew distZip` | `build/distributions/jdbgen-<version>.zip` — the release archive. |
| `./gradlew jpackageImage` | `build/jpackage-image/jdbgen/` — the application image, with its launcher signed. Windows only, and it needs a JDK 14+; see [The Windows installer](#the-windows-installer). |
| `./gradlew jpackage` | `build/jpackage/jdbgen-<version>.msi` — the signed Windows installer, built from the application image above. Windows only, and it needs a JDK 14+ and the WiX Toolset; see [The Windows installer](#the-windows-installer). |
| `./gradlew jar` | `build/libs/jdbgen-<version>.jar`. The manifest sets `Main-Class: comart.tools.jdbgen.JDBGenerator` and a `Class-Path` listing all 22 runtime jars as `lib/<name>.jar`, so the jar **cannot run on its own** — it needs a sibling `lib/` directory. |
| `./gradlew test` | Runs the JUnit 5 (Jupiter 5.10.2) suite. HTML report at `build/reports/tests/test/index.html`. |
| `./gradlew run` | Launches the application straight from the source class path. The user interface language comes from `config.json`, see [Translations](#translations). |
| `./gradlew javadoc` | API documentation in `build/docs/javadoc/`. |
| `./gradlew docShots` | Retakes the screenshots in `docs/images/`; see [Documentation screenshots](#documentation-screenshots). Needs a desktop session. |
| `./gradlew clean` | Deletes `build/`. |

> **`./gradlew run` uses your real configuration.** The application writes into the user data directory of the operating system (`%APPDATA%\jdbgen`, `~/Library/Application Support/jdbgen`, `~/.config/jdbgen`), so a development run shares `config.json`, the master password and `drivers/` with an installed jdbgen. Add a `systemProperty 'jdbgen.dataDir', …` line to the `run` task if you would rather have it write into a scratch directory. The read-only side resolves against the project root, because there is no jar to sit next to: `resource/icon.png`, `resource/loading.gif` and `templates/` are already there, so the icon, the busy indicator and the sample templates work as they do in a real installation.

## Build output

`./gradlew distZip` assembles the same layout that ships on the releases page:

```
jdbgen-<version>/
├── jdbgen-<version>.jar
├── jdbgen.sh                 copied from shells/
├── jdbgen.cmd                copied from shells/
├── lib/                      the full runtime class path (22 jars)
├── templates/                copied from templates/
├── resource/                 copied from resource/
└── sample_h2.db.mv.db
```

Everything is nested under a single top-level directory named after the artifact, so unpacking never scatters files. The `lib/` contents come from Gradle's `runtimeClasspath` configuration, which means adding a dependency in `build.gradle` automatically updates both `lib/` and the jar's `Class-Path`.

The version placeholder is resolved at build time: `src/main/configs/version.properties` contains only

```properties
version=${version}
```

and `processResources` expands `${version}` from the Gradle project version. `PlatformUtils.getVersion()` reads the filtered `/version.properties` from the class path, and that value drives both the About dialog and the startup update check. If the file is missing the version reads as `unknown` and the update check is skipped.

## The Windows installer

The ZIP stays the portable download. Next to it, `./gradlew jpackage` produces `build/jpackage/jdbgen-<version>.msi`, a Windows installer that bundles its own Java runtime — jpackage runs `jlink` for it — so its users need no Java installed. That MSI is what is published to winget.

Two things have to be in place:

- **A JDK 14 or newer running Gradle.** `jpackage` lives in `<java.home>/bin`; the task checks for it and reports what to do when it is missing. The application bytecode is unaffected: `options.release = 11` still pins it to Java 11.
- **WiX Toolset 3.x on `PATH`** (`candle.exe`, `light.exe`). GitHub's `windows-latest` runners ship with it. Locally, install it from [the WiX 3 releases](https://github.com/wixtoolset/wix3/releases). Without it jpackage aborts and its own message is what the task reports.

The task fails early on a non-Windows host. Its input is staged by `jpackageInput` into `build/jpackage-input/`: the same content as the ZIP minus the `jdbgen.cmd`/`jdbgen.sh` launchers, which the native launcher generated by jpackage replaces. Everything staged ends up next to the jar in the installed `app` directory, so the jar manifest's `Class-Path` keeps resolving. An installer left over from a previous run is deleted first, because jpackage refuses to overwrite one.

The installer is built in two steps, because the executables have to be signed *before* they are packed into the MSI:

1. `jpackageImage` runs `jpackage --type app-image` into `build/jpackage-image/jdbgen/` and signs the `jdbgen.exe` launcher in it.
2. `jpackage` hands that finished image to `jpackage --type msi --app-image …` and signs the resulting `.msi`.

In `--app-image` mode jpackage packs an image instead of building one, so the options that describe how to build one — `--input`, `--main-jar`, `--main-class`, `--icon` — belong to the first step only; repeating them in the second is an error. The MSI's own metadata (`--name`, `--app-version`, `--vendor`, `--about-url`, the `--win-*` options and `--win-upgrade-uuid`) stays with the second.

Two details in `build.gradle` are worth knowing before touching them:

- **`windowsUpgradeUuid` must never change.** Windows Installer recognises "the same product across versions" by that upgrade code. A new value would make the next release install *beside* the previous one instead of upgrading it, and orphan every copy already installed.
- **The MSI `ProductVersion` is derived, not copied.** It only accepts `major.minor.build` with major and minor up to 255 and build up to 65535, so `msiVersionOf` takes the leading numbers of the project version and rejects anything that does not start with a digit. Qualifiers such as `-SNAPSHOT` are dropped rather than passed on, because jpackage refuses them.

### Code signing

Windows presents an unsigned installer as coming from an unknown publisher, and SmartScreen warns about it, so both the launcher and the MSI are Authenticode signed when a certificate is available:

```
signtool sign /n Xcomart /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 <file>
```

- **The certificate** is the one whose subject is `CN=Xcomart` in the personal store of the user running the build, `Cert:\CurrentUser\My`. `certutil -user -store My Xcomart` shows whether it is there. The timestamp means the signature stays valid after the certificate expires.
- **`signtool.exe`** is looked for on `PATH` first, then below the Windows SDK (`C:\Program Files (x86)\Windows Kits\10\bin\<version>\x64\`, newest version first) and in the ClickOnce SDK directory. Install it with the "Windows SDK Signing Tools for Desktop Apps" component of the Windows SDK or of Visual Studio.
- **Signing is optional.** Without `signtool` or without the certificate the build warns and produces unsigned output rather than failing — a contributor's machine and a fork's CI runner have neither. `./gradlew jpackage -Pcodesign=false` turns it off deliberately, without the warnings. A `signtool` run that *fails* while both are present does fail the build, because that is a broken signature rather than a missing one.

Check the result with `Get-AuthenticodeSignature build\jpackage\jdbgen-<version>.msi`. A certificate issued by a public CA reads `Valid`; a self-signed one reads `UnknownError` (its issuer is not trusted on the machine) while `SignerCertificate.Subject` still shows `CN=Xcomart`, which is what the signature actually carries.

On CI the certificate is imported by the `Import the code signing certificate` step of `.github/workflows/release.yml` from two repository secrets, and the step is skipped unless **both** are set:

| Secret | Content |
|---|---|
| `CODESIGN_PFX_BASE64` | The certificate exported with `Export-PfxCertificate`, base64 encoded |
| `CODESIGN_PFX_PASSWORD` | The password the PFX was exported with |

```powershell
$c = Get-ChildItem Cert:\CurrentUser\My | ? { $_.Subject -eq 'CN=Xcomart' }
Export-PfxCertificate -Cert $c -FilePath codesign.pfx -Password (Read-Host -AsSecureString 'password')
[Convert]::ToBase64String([IO.File]::ReadAllBytes('codesign.pfx')) > codesign.b64
```

Register the content of `codesign.b64` and the password as the two secrets, then delete both local files — the PFX holds the private key. The workflow deletes the file it decodes on the runner for the same reason.

### The installer icon

jpackage needs a Windows `.ico` for the generated launcher, the Add/Remove Programs entry and the shortcuts. `resource/icon.ico` is a derived file, generated from `resource/icon.png` by:

```powershell
powershell -ExecutionPolicy Bypass -File packaging\make-icon.ps1
```

Re-run it whenever `icon.png` changes. It needs nothing but Windows PowerShell (it uses `System.Drawing`) and writes a multi-resolution PNG-in-ICO container with frames from 16 to 256 pixels. The application itself keeps using `icon.png`.

`icon.png` is itself generated, by `packaging/draw-icon.java` — run `java packaging/draw-icon.java` from the repository root to redraw the 512×512 source, then re-run `make-icon.ps1`.

`resource/loading.gif`, the busy indicator, is generated the same way, by `java packaging/draw-loading.java`.

## Project layout

```
jdbgen/
├── build.gradle                          single build script; version lives here
├── settings.gradle
├── gradle/wrapper/                       pins Gradle 8.14.3
├── src/main/java/comart/
│   ├── tools/jdbgen/
│   │   ├── JDBGenerator.java             main(): language, look and feel, update check, main window
│   │   ├── template/                     the template engine
│   │   ├── types/                        configuration and database model types
│   │   ├── ui/                           Swing windows (NetBeans .form + .java pairs)
│   │   └── update/                       the self-update
│   └── utils/                            shared helpers
├── src/main/configs/version.properties   token-filtered into the resources
├── src/main/resources/
│   ├── defaultConfig.json                stock drivers and Maven endpoints
│   ├── i18n/                             translation bundles (XML properties)
│   └── icons/                            stock icons
├── src/test/java/                        JUnit 5 tests
├── templates/                            sample templates shipped in the zip
├── shells/                               jdbgen.sh, jdbgen.cmd
├── resource/                             icon.png, loading.gif, icon.ico
├── packaging/make-icon.ps1               regenerates icon.ico from icon.png
├── sample_h2.db.mv.db                    sample H2 database shipped in the zip
├── docs/                                 this documentation
└── .github/workflows/
    ├── build.yml                         CI, on every push and pull request
    └── release.yml                       zip + MSI + GitHub release + winget, on a v* tag
```

### `comart.tools.jdbgen.template`

`TemplateManager` — the whole template engine in one class: it parses `${…}` statements, dispatches the `if`/`for`/`item`/`super` handlers, applies decorators, and renders a table or column model into output text. See [template-reference.md](template-reference.md) for the language it implements.

### `comart.tools.jdbgen.types`

The serialisable configuration model and the database metadata model.

- `JDBGenConfig` — the singleton root object. Loads and saves `config.json`, builds the default configuration (including the `Sample H2 Embedded` connection) when none exists, backs up an unreadable configuration, and triggers re-encryption when legacy-format values are found. Its `peekLanguage()` reads the `language` entry out of the file as plain JSON, without the master password, because the language has to be known before the password prompt appears.
- `JDBConnection`, `JDBDriver`, `JDBTemplate`, `JDBPreset`, `JDBAbbr`, `JDBListBase` — connections, driver definitions, template entries, presets and abbreviation rules. `JDBConnection` annotates `connectionUrl`, `userName` and `userPassword` with the encrypting Gson type adapter.
- `HasIcon`, `HasTitle` — small interfaces the list renderers use to display any configuration item.
- `db/` — `DBMeta`, `DBMetaModel`, `DBSchema`, `DBTable`, `DBColumn`, `SqlTypes`: the JDBC metadata reader and the table/column objects that templates iterate over.
- `maven/` — request and response types for the Maven Central search API.

### `comart.tools.jdbgen.ui`

Every Swing window, as NetBeans GUI Builder `.form` files paired with generated `.java`: `JDBGeneratorMain` (main window), `JDBConnectionManager`, `JDBDriverManager`, `JDBPresets`, `JDBAbbreviationMapper`, `MavenExplorer`, `JDBTableView`, `ProcessProgress`, `JDBAbout`, `Acknowledgements`, plus the `SchemaCellRenderer` and `NamingUtils` helpers.

> Edit the `.form` files in NetBeans rather than hand-editing the generated `initComponents()` blocks in the matching `.java` files — the GUI Builder overwrites them.

### `comart.tools.jdbgen.update`

The self-update, split in two because the second half has to run while `lib/` is being replaced.

- `UpdateManager` — runs inside the application: picks the `jdbgen-*.zip` asset out of the release description, downloads it into `<installation>/.update/` behind a cancellable progress dialog, unpacks it (stripping the top-level `jdbgen-<version>/` directory and refusing entries that point outside the target), copies the running jar to `.update/updater.jar` and starts the applier in a JVM of its own. It also removes a staging directory left over from an earlier run at every startup.
- `UpdateApplier` — the `main()` of that second JVM. It waits for the application to release its jar, moves the old jar and `lib/` to `.update/backup/`, copies the new release over the installation, restarts jdbgen and cleans up. **It runs with nothing but a copy of the jdbgen jar on its class path, so it must use JDK classes only** — no slf4j/lombok logging, no gson, no okhttp. Its output is redirected into `.update/update.log`. See [installation.md](installation.md#updating) for which files it replaces.

### `comart.utils`

- `StrUtils` — string casing and padding helpers used by the template decorators, plus all password-based encryption (v2 AES-256-GCM with PBKDF2, and the legacy AES-128/CBC reader).
- `UIUtils` — look-and-feel setup, dialogs, list/table renderers, and `getIcon()`, the resolver behind every icon string ([icons.md](icons.md)).
- `I18n` — the translation lookup behind every user-visible string, see [Translations](#translations).
- `AppDirs` — the two directories the application uses: the per-user data directory it writes to and the read-only installation it reads the release files from. It also resolves the relative paths of a configuration against both, decides whether the installation can be written to (by creating a file, not by reading permission bits), and carries the files of a 0.3.0-or-older installation over on first start. See [Where jdbgen keeps its data](installation.md#where-jdbgen-keeps-its-data).
- `PlatformUtils` — OS detection, dock icon, opening URLs, version lookup and the GitHub release update check, which either hands a newer release over to `comart.tools.jdbgen.update` or, when the installation is not writable, tells the user how to install it themselves.
- `EncryptionAdapter` — the Gson `TypeAdapter` that encrypts on write and decrypts on read.
- `HttpUtils` — the shared OkHttp client.
- `MavenREST` — Maven Central search and download.
- `ClassUtils` — finds `java.sql.Driver` implementations inside a driver jar so the Driver Manager can offer a class list.
- `ObjUtils`, `tuple/Pair` — reflection-based property access and a small tuple type.

## Translations

The user interface is English by default and ships Korean, Spanish, Japanese and Simplified Chinese translations. The language is stored as the `language` entry of `config.json` (`null`/absent or `"system"` = operating system locale, otherwise a language tag such as `"en"` or `"ko"`) and is picked in the combo box next to `Dark UI` in the main window. `JDBGenerator.main()` reads it with `JDBGenConfig.peekLanguage()` before anything else can open a dialog and hands it to `I18n.applyLanguage()`, which also sets the JVM default locale. A change only takes effect on the next start.

### Bundle files

Bundles are [Java properties XML](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html#loadFromXML-java.io.InputStream-) documents in UTF-8, on the class path below `i18n/`:

```
src/main/resources/i18n/common.xml        English, the fallback of every locale
src/main/resources/i18n/common_ko.xml     Korean
src/main/resources/i18n/common_es.xml     Spanish
src/main/resources/i18n/common_ja.xml     Japanese
src/main/resources/i18n/common_zh_CN.xml  Simplified Chinese
```

Adding a language means one `_<locale>` file per base bundle, a new entry in the `LANGUAGES` array and combo model of `JDBGeneratorMain.initLanguageCombo()`, and the suffix in the `LANGUAGES` list of `I18nBundleTest`, which then enforces that every bundle carries the new translation completely.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
  <entry key="common.update.title">Update Available</entry>
  <entry key="common.update.available">New version {0} is available.&#10;Do you want to update now?</entry>
</properties>
```

Write line breaks as `&#10;` rather than as real newlines — the indentation of a multi-line element would end up in the value.

`I18n` loads them through a `ResourceBundle.Control` subclass that knows the `xml` format. The usual locale chain applies (`ko_KR` → `ko` → the file without a suffix), except that the JVM default locale is *not* used as a last fallback, so an explicitly selected language stays what it is.

### Key naming

A key reads `<bundle>.<rest>`: the first segment names the file it lives in, and the whole key is what is looked up inside that file. `common.update.title` is the entry `common.update.title` of `i18n/common.xml`. Keys are therefore unique across the application, and the file to edit is obvious from the key.

- **Non-form strings go into `common`** — everything built in code: `PlatformUtils`, `UpdateManager`, `JDBGenConfig`, the shared `UIUtils` dialogs.
- **Form strings go into a bundle of their own, named after the window** — the texts the NetBeans GUI Builder writes into `initComponents()` and the messages of that window's own code.

Nothing in `I18n` ever throws: a missing bundle, a missing key or a broken `{0}` pattern is logged and the key is returned instead, so a partially translated build still runs.

- `I18n.t(key)` returns the entry unchanged.
- `I18n.t(key, args…)` runs it through `MessageFormat`. In those entries a literal apostrophe has to be doubled (`''{0}'' is required`), otherwise `MessageFormat` reads the quotes as an escape and prints `{0}` verbatim.

Log messages and exception messages stay English and are never translated. `comart.tools.jdbgen.update.UpdateApplier` is excluded as well: it runs in a JVM with nothing but the jdbgen jar on its class path.

## Tests

The suite runs on JUnit 5 (Jupiter 5.10.2) via `useJUnitPlatform()`.

| Test | Covers |
|---|---|
| `comart/tools/jdbgen/template/TemplateManagerTest` | The template engine: statements, decorators, loops and conditionals. `TestResultSet` in the same package is a fake `java.sql.ResultSet` that feeds it table metadata without a database. |
| `comart/utils/StrUtilsTest` | Case conversion, padding and the other string helpers the decorators build on. |
| `comart/utils/ObjUtilsTest` | Reflection-based property lookup. |
| `comart/utils/AppDirsTest` | The two directories: the per-user location of each platform and its fallbacks, both system property overrides, resolving a relative configuration path against the user data directory before the installation, keeping a stored path relative to whichever of the two it is below, and deciding writability by actually writing a file. |
| `comart/utils/AppDirsMigrationTest` | Carrying a 0.3.0-or-older installation over: configuration, backups and `drivers/` copied into the user data directory, an existing configuration never overwritten, the working directory treated as a legacy location too, and nothing done when there is no previous installation. |
| `comart/utils/EncryptionTest` | v2 round trips, non-deterministic ciphertext, rejection of a wrong master password, reading legacy-format values, null/empty pass-through, and malformed input. |
| `comart/utils/I18nTest` | The bundle loader: the English default, a Korean locale, a regional locale falling back to its language, an untranslated key falling back to the original, unknown keys and unknown bundles returning the key instead of throwing, `MessageFormat` substitution, and the language setting to locale mapping. Its fixtures are `src/test/resources/i18n/testonly*.xml`. |
| `comart/utils/I18nBundleTest` | The shipped bundles: every bundle below `i18n/` carries every supported language, each translation has the key set and `{0}` placeholders of the English original, every key is prefixed with its bundle name, and every entry parses as a message pattern. |
| `comart/tools/jdbgen/types/JDBGenConfigBackupTest` | Configuration backup and restore: moving an unreadable file aside, reporting a failed backup, and putting it back. |
| `comart/tools/jdbgen/types/JDBGenConfigLanguageTest` | Reading the `language` entry out of `config.json` without the master password: a stored value, no entry, an empty entry, a broken file and a missing file. |
| `comart/tools/jdbgen/types/JDBGenConfigDefaultsTest` | The default configuration: the sample database copied next to the configuration and an existing copy kept, a release without the sample database still producing a usable connection, the template paths pointing into the installation and the output directory below the user data directory. |
| `comart/tools/jdbgen/types/ConfigRoundTripTest` | Configuration serialisation, including that encrypted fields survive a save/load cycle and that the bundled `defaultConfig.json` serialises cleanly. |
| `comart/tools/jdbgen/update/UpdateManagerTest` | Picking the distribution archive out of a release description, stripping the top-level directory while unpacking, and refusing archive entries that point outside the target directory. |
| `comart/tools/jdbgen/update/UpdateApplierTest` | Applying a release to an installation: jar and `lib/` replaced, launcher scripts and resources overwritten, configuration, drivers, output, edited templates and the sample database kept, and the previous version put back when the update fails. |

Run everything with `./gradlew test`, or a single class with:

```bash
./gradlew test --tests 'comart.utils.EncryptionTest'
```

The tests are headless and need no database or network.

## Documentation screenshots

Every screenshot below `docs/images/` is generated, not taken by hand:

```bash
./gradlew docShots
```

The task runs `src/test/java/comart/tools/jdbgen/docs/ScreenshotTool.java`, which
opens the real application windows, fills them with a fixed sample configuration
and lets each window paint itself into a PNG. Painting the window rather than
grabbing the screen is what keeps the result independent of the desktop it runs
on — no other window, no drop shadow and no display scaling ends up in the
picture — and it works because FlatLaf draws the title bar itself, so the whole
window is Swing. The windows still appear on screen for a moment, so the task
needs a desktop session and cannot run on the CI runner.

What it sets up:

* a throwaway data directory (`build/doc-shots` by default, `-PshotsHome=<dir>`
  to move it) handed to the application as `jdbgen.dataDir`. **Your own
  `config.json`, master password and driver jars are never touched**, and the
  tool refuses to start when that property names the real user data directory.
* `jdbgen.resourceBase` pointing at the working copy, so `templates/` and
  `resource/` resolve the way they do in an installation.
* the H2 driver resolved from Maven Central into the sandbox `drivers/`, and a
  freshly created sample database with the two `T_SAMPLE_*` tables the shots
  show.
* English, the light theme and the sample connection, preset and abbreviation
  rules of the pictures.

The master password prompt never appears: the configuration is built from the
bundled defaults with `JDBGenConfig.getInstance(true)`, which neither reads nor
writes a configuration file.

`maven_repository.png` is the one shot that needs the network. `search.maven.org`
throttles, so the search is retried three times before the run reports
`WARNING maven_repository.png: …` and keeps the picture it has. Write somewhere
else than `docs/images` with `-PshotsOut=<dir>` while you are trying things out.

## Continuous integration

There are two workflows: `build.yml` on every push and pull request, and `release.yml` on a `v*` tag ([Cutting a release](#cutting-a-release)).

`.github/workflows/build.yml` runs on pushes and pull requests targeting `master`/`main`:

1. Check out the repository.
2. Set up Temurin JDK 11 and the Gradle action's cache.
3. Read the project version out of `./gradlew -q properties`.
4. Run `./gradlew clean build`.
5. Upload `build/distributions/*.zip` as a workflow artifact.

**This workflow does not create GitHub releases.** The artifact is only attached to the workflow run. Publishing is `release.yml`'s job.

## Cutting a release

The version is declared in one place — `build.gradle`:

```groovy
version = '0.3.0'
```

`src/main/configs/version.properties` only holds the `${version}` placeholder and needs no editing.

Cutting a release is therefore *bump the version, then push the tag*:

1. Bump `version` in `build.gradle` and commit it on its own (the existing history uses a bare `Release <version>` message).
2. Tag that commit as `v<version>` — matching the existing tags (`v0.2.4`, `v0.2.5`, …) — and push the tag.

`.github/workflows/release.yml` takes it from there:

1. **Check the tag against the project version.** `v0.3.1` and `version = '0.3.0'` in `build.gradle` disagree, so the workflow stops with an error rather than publishing a release whose label does not match what is inside the artifacts. Bump the version or move the tag, then push again.
2. **Build the zip on `ubuntu-latest`** with `./gradlew build`, which runs the whole test suite first — a failing test stops the release before anything is published.
3. **Build the MSI on `windows-latest`** with `./gradlew jpackage`, on JDK 21 and the runner's preinstalled WiX Toolset. The step before it imports the code signing certificate from the `CODESIGN_PFX_BASE64` / `CODESIGN_PFX_PASSWORD` secrets; without both the installer is built unsigned, see [Code signing](#code-signing).
4. **Create the GitHub release** for the tag with both artifacts attached and the release notes generated from the commits since the previous tag.
5. **Submit the new version to winget**, if the repository has a `WINGET_TOKEN` secret. Without it that job logs a line and does nothing, and the release itself still succeeds.

There is nothing to upload by hand. What is still worth doing afterwards is the sanity check: install or unpack one of the published artifacts and confirm the About dialog shows the new version.

Both asset names matter. Older installations update themselves from the `jdbgen-<version>.zip` asset — that is what `UpdateManager` looks for, and it expects the usual single top-level `jdbgen-<version>/` directory inside. The winget job builds the installer URL from the MSI asset name. The startup update check compares the running version against `tag_name` from the GitHub API and tolerates the leading `v`.

### winget

The winget job runs `wingetcreate update Xcomart.Jdbgen --submit`, which opens a pull request against `microsoft/winget-pkgs` from a fork. It needs two things:

- **`WINGET_TOKEN`**, a repository secret holding a classic personal access token with the `public_repo` scope, belonging to an account that has a fork of `microsoft/winget-pkgs`. `wingetcreate` pushes the manifest to that fork and opens the pull request from it.
- **An existing `Xcomart.Jdbgen` package**, because `update` starts from the published manifest. The very first submission has to be made by hand, once, after the first MSI release exists:

  ```
  wingetcreate new <url of the released .msi>
  ```

  with `PackageIdentifier` `Xcomart.Jdbgen`. Once that pull request is merged, every later release is handled by the workflow.

The manifest points at the release asset URL rather than at a local file — `wingetcreate` downloads it from there to compute the installer hash — which is why the winget job waits for the release job to have published it.

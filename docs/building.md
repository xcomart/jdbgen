# Building from Source

jdbgen is a plain Gradle project with no external tooling requirements beyond a JDK — the Gradle wrapper takes care of Gradle itself. This page covers the build commands, what each one produces, how the source tree is organised, and the steps for cutting a release.

[← Documentation index](README.md)

## Requirements

| | |
|---|---|
| JDK | **11 or newer.** The continuous integration build uses Temurin JDK 11. Because every `JavaCompile` task sets `options.release = 11`, newer JDKs also work and still produce Java 11 bytecode — a `clean build` on JDK 17 has been verified. |
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
| `./gradlew jar` | `build/libs/jdbgen-<version>.jar`. The manifest sets `Main-Class: comart.tools.jdbgen.JDBGenerator` and a `Class-Path` listing all 22 runtime jars as `lib/<name>.jar`, so the jar **cannot run on its own** — it needs a sibling `lib/` directory. |
| `./gradlew test` | Runs the JUnit 5 (Jupiter 5.10.2) suite. HTML report at `build/reports/tests/test/index.html`. |
| `./gradlew run` | Launches the application straight from the source class path. The user interface language comes from `config.json`, see [Translations](#translations). |
| `./gradlew javadoc` | API documentation in `build/docs/javadoc/`. |
| `./gradlew clean` | Deletes `build/`. |

> **`./gradlew run` writes into the project root.** The task inherits Gradle's working directory, which is the project root, and the application resolves `config.json`, `drivers/` and `resource/` relative to the working directory. A `config.json` (and possibly `drivers/`) will therefore appear at the top of your checkout. Both are git-ignored, but delete them if you want a clean first-run experience. `resource/icon.png` and `resource/loading.gif` are already there, so the icon and busy indicator work as they do in a real installation.

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
├── resource/                             icon.png, loading.gif
├── sample_h2.db.mv.db                    sample H2 database shipped in the zip
├── docs/                                 this documentation
└── .github/workflows/build.yml           CI
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
- `PlatformUtils` — OS detection, dock icon, opening URLs, version lookup and the GitHub release update check, which hands a newer release over to `comart.tools.jdbgen.update`.
- `EncryptionAdapter` — the Gson `TypeAdapter` that encrypts on write and decrypts on read.
- `HttpUtils` — the shared OkHttp client.
- `MavenREST` — Maven Central search and download.
- `ClassUtils` — finds `java.sql.Driver` implementations inside a driver jar so the Driver Manager can offer a class list.
- `ObjUtils`, `tuple/Pair` — reflection-based property access and a small tuple type.

## Translations

The user interface is English by default and ships a Korean translation. The language is stored as the `language` entry of `config.json` (`null`/absent or `"system"` = operating system locale, otherwise a language tag such as `"en"` or `"ko"`) and is picked in the combo box next to `Dark UI` in the main window. `JDBGenerator.main()` reads it with `JDBGenConfig.peekLanguage()` before anything else can open a dialog and hands it to `I18n.applyLanguage()`, which also sets the JVM default locale. A change only takes effect on the next start.

### Bundle files

Bundles are [Java properties XML](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html#loadFromXML-java.io.InputStream-) documents in UTF-8, on the class path below `i18n/`:

```
src/main/resources/i18n/common.xml      English, the fallback of every locale
src/main/resources/i18n/common_ko.xml   Korean
```

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
| `comart/utils/EncryptionTest` | v2 round trips, non-deterministic ciphertext, rejection of a wrong master password, reading legacy-format values, null/empty pass-through, and malformed input. |
| `comart/utils/I18nTest` | The bundle loader: the English default, a Korean locale, a regional locale falling back to its language, an untranslated key falling back to the original, unknown keys and unknown bundles returning the key instead of throwing, `MessageFormat` substitution, and the language setting to locale mapping. Its fixtures are `src/test/resources/i18n/testonly*.xml`. |
| `comart/utils/I18nBundleTest` | The shipped bundles: `common.xml` and `common_ko.xml` carry the same key set and the same `{0}` placeholders, every key is prefixed with its bundle name, and every entry parses as a message pattern. |
| `comart/tools/jdbgen/types/JDBGenConfigBackupTest` | Configuration backup and restore: moving an unreadable file aside, reporting a failed backup, and putting it back. |
| `comart/tools/jdbgen/types/JDBGenConfigLanguageTest` | Reading the `language` entry out of `config.json` without the master password: a stored value, no entry, an empty entry, a broken file and a missing file. |
| `comart/tools/jdbgen/types/ConfigRoundTripTest` | Configuration serialisation, including that encrypted fields survive a save/load cycle and that the bundled `defaultConfig.json` serialises cleanly. |
| `comart/tools/jdbgen/update/UpdateManagerTest` | Picking the distribution archive out of a release description, stripping the top-level directory while unpacking, and refusing archive entries that point outside the target directory. |
| `comart/tools/jdbgen/update/UpdateApplierTest` | Applying a release to an installation: jar and `lib/` replaced, launcher scripts and resources overwritten, configuration, drivers, output, edited templates and the sample database kept, and the previous version put back when the update fails. |

Run everything with `./gradlew test`, or a single class with:

```bash
./gradlew test --tests 'comart.utils.EncryptionTest'
```

The tests are headless and need no database or network.

## Continuous integration

`.github/workflows/build.yml` runs on pushes and pull requests targeting `master`/`main`:

1. Check out the repository.
2. Set up Temurin JDK 11 and the Gradle action's cache.
3. Read the project version out of `./gradlew -q properties`.
4. Run `./gradlew clean build`.
5. Upload `build/distributions/*.zip` as a workflow artifact.

**CI does not create GitHub releases.** The artifact is only attached to the workflow run; the ZIP on the releases page is uploaded by hand.

## Cutting a release

The version is declared in one place — `build.gradle`:

```groovy
version = '0.2.5'
```

`src/main/configs/version.properties` only holds the `${version}` placeholder and needs no editing.

1. Bump `version` in `build.gradle` and commit it on its own (the existing history uses a bare `Release <version>` message).
2. Run `./gradlew clean build` and check that the tests pass and `build/distributions/jdbgen-<version>.zip` was produced.
3. Sanity-check the archive: unpack it somewhere else, run the launcher, confirm the About dialog shows the new version.
4. Tag the commit as `v<version>` — matching the existing tags (`v0.2.4`, `v0.2.5`, …) — and push the tag. The startup update check compares the running version against `tag_name` from the GitHub API and tolerates the leading `v`.
5. Create the GitHub release for that tag and upload `build/distributions/jdbgen-<version>.zip` manually.

Once the release is published, running installations of older versions will offer the update on their next start. The update is installed automatically, so the `jdbgen-<version>.zip` asset has to be attached to the release and keep its name — that is what `UpdateManager` looks for, and it expects the usual single top-level `jdbgen-<version>/` directory inside.

# Installation

jdbgen ships as a single ZIP archive that contains everything it needs except the JDBC drivers themselves. This page covers what you need before you start, how to unpack and run the application, where it keeps its configuration, and what the bundled sample connection and drivers give you out of the box.

[← Documentation index](README.md)

## Requirements

| | |
|---|---|
| Java | The application itself is compiled for Java 11 (`options.release = 11`), so a **JRE 11 or newer** is enough to run the jar. |
| Operating system | Anything with a desktop Java runtime — Linux, macOS, Windows. The UI is Swing with the [FlatLaf](https://www.formdev.com/flatlaf/) look and feel. |
| Disk space | About 10 MB for the unpacked distribution, plus whatever JDBC driver jars you download. |
| Network | Optional. It is only used for the update check, the Maven Repository Explorer, and remote (`http`/`https`) icon URLs. |

Make sure the `bin` directory of your Java installation is on `PATH`, or that `JAVA_HOME` points at the installation.

Both launchers read the version from `java -version`, which a plain JRE provides,
and refuse to start only when the major version is below 11. A version string
they cannot parse produces a warning rather than a refusal, so an unusual runtime
never blocks startup.

> **Note**
> Releases up to and including 0.2.5 probed the version with `javap`, a JDK-only
> tool. On a plain JRE `jdbgen.sh` exited with **"Java too old!"** no matter how
> new the runtime was. If you hit that on an older release, start the jar
> directly — see [Running](#running) — or install a JDK.

## Download and install

1. Download `jdbgen-<version>.zip` from the [latest release](https://github.com/xcomart/jdbgen/releases/latest).
2. Unpack it wherever you like. The archive already contains a top-level `jdbgen-<version>/` directory, so it will not scatter files into the current directory.
3. On Linux/macOS, make the launcher executable if your unzip tool dropped the permission bit:

   ```bash
   chmod +x jdbgen-<version>/jdbgen.sh
   ```

There is no installer and nothing is written outside the unpacked directory.

## Directory layout

```
jdbgen-<version>/
├── jdbgen-<version>.jar     the application
├── jdbgen.sh                launcher for Linux/Unix/macOS
├── jdbgen.cmd               launcher for Windows
├── lib/                     22 dependency jars (gson, flatlaf, okhttp, logback, …)
├── templates/               sample templates
│   ├── java_model.java
│   ├── mybatis_mapper.xml
│   └── php_ci.php
├── resource/
│   ├── icon.png             window and dock icon
│   └── loading.gif          busy indicator
└── sample_h2.db.mv.db       small H2 database to try the tool against
```

Two directories appear the first time you run the application:

```
├── config.json              your configuration (created on first run)
└── drivers/                 JDBC driver jars downloaded through the UI
```

> **Do not move the jar out of this directory.** Its manifest declares a `Class-Path` of `lib/*.jar` relative to the jar's own location, so a jar without its sibling `lib/` directory fails to start.

## Running

### With the bundled launcher

```bash
# Linux / Unix / macOS  (requires a JDK — see the warning above)
./jdbgen.sh
```

```bat
REM Windows
jdbgen.cmd
```

Both scripts `cd` into the installation directory before starting Java, locate `jdbgen-*.jar` themselves, and launch it. On Windows the application is started with `javaw.exe` so no console window stays open.

### Directly with `java -jar`

```bash
cd /path/to/jdbgen-<version>
java -jar jdbgen-<version>.jar
```

This is the recommended route if you only have a JRE, or if you want to pass extra JVM options:

```bash
java -Duser.language=en -jar jdbgen-<version>.jar
```

### The working directory matters

Every path the application uses for its own data is **relative to the current working directory**, not to the jar:

| Path | Used for |
|---|---|
| `config.json` | the entire configuration |
| `config.json.<yyyyMMdd_HHmmss>.bak` | configuration backups |
| `drivers/` | JDBC jars downloaded through the Maven Repository Explorer |
| `resource/icon.png` | window/dock icon |
| `resource/loading.gif` | busy indicator |

The launcher scripts take care of this by `cd`-ing into the installation directory first. When you run `java -jar` yourself, **`cd` into the installation directory first** — otherwise you get a fresh empty configuration in whatever directory you happened to be in, and the window icon and busy animation will be missing.

If you want a desktop launcher or shell alias, make it change directory first:

```bash
alias jdbgen='(cd /opt/jdbgen-<version> && java -jar jdbgen-<version>.jar)'
```

## First run

1. **Update check.** On startup the application queries the GitHub Releases API for `xcomart/jdbgen`. If a newer tag than the running version is found, it offers to update; accepting downloads the new archive, installs it over the current directory and restarts jdbgen. Cancelling the download simply continues the startup. If the automatic update cannot be carried out, jdbgen offers to open the [releases page](https://github.com/xcomart/jdbgen/releases/latest) in your browser instead. If the network is unreachable the check fails quietly and startup continues. See [Updating](#updating) for what is replaced and what is kept.

2. **Master password.** With no `config.json` present, the application asks you to choose a master password.

   ![Master password setting window](images/master_password_set.png "Master Password Setting Window")

   On every later start it asks for that password instead.

   ![Master password asking window](images/master_password.png "Master Password Asking Window")

   > **There is no recovery.** The master password is never stored, so if you forget it the encrypted fields in `config.json` cannot be read back. After three wrong attempts the application offers to start over with a default configuration; your old file is moved aside as a `.bak` rather than deleted.

3. **Try the sample connection.** The default configuration comes with one ready-made connection so you have something to generate from immediately:

   | Setting | Value |
   |---|---|
   | Connection name | `Sample H2 Embedded` |
   | Driver | `H2 Embedded` |
   | Connection URL | `jdbc:h2:./sample_h2.db` |
   | Output directory | `output` |
   | Templates | `Java Model`, `MyBatis mapper`, `PHP CI Model` (from `templates/`) |
   | Author | your OS login name |

   The URL is relative, so it resolves to the bundled `sample_h2.db.mv.db` as long as the working directory is the installation directory. You still need the H2 driver jar — download it with the Maven Repository Explorer (search for `h2database`), then pick a schema, tick the templates and press **Generate**. Files land in `output/`. See [ui-guide.md](ui-guide.md) for a walkthrough of the windows.

## Where jdbgen keeps its data

### `config.json`

All state — connections, drivers, template presets, abbreviation rules, the Maven search endpoints and the light/dark UI flag — lives in a single `config.json` in the working directory. It is written whenever you confirm a change in one of the manager windows.

### Backups

When an existing `config.json` cannot be loaded and you choose to start from the default configuration, the old file is not deleted. It is renamed to:

```
config.json.<yyyyMMdd_HHmmss>.bak
```

next to the original, and the application tells you the exact path. Rename it back to `config.json` to restore it. Note that a backup is only useful if you still know the master password it was encrypted with.

### What is encrypted, and what is not

Only three fields are encrypted, all on the connection object:

- `connectionUrl`
- `userName`
- `userPassword`

Everything else in `config.json` — connection names, driver definitions, custom SQL, template paths, output directories, author names, custom variables — is **stored as plain JSON**. Do not put secrets in those fields, and treat the file as sensitive if the connection URL alone would be revealing.

The current format (v2) for an encrypted value is:

```
"ENC2:" + Base64( salt(16 bytes) ‖ iv(12 bytes) ‖ AES-256-GCM ciphertext‖tag )
```

The key is derived from your master password with PBKDF2WithHmacSHA256, 210,000 iterations, 256-bit key, using the salt embedded in the value itself. Because the salt travels with the value, values written by earlier sessions stay readable.

Configurations written by older releases used a weaker scheme: plain Base64 (no prefix) of AES-128/CBC, with the key and IV taken from the two halves of `SHA-256(master)`. Those values are still readable. If **any** legacy value is decrypted during load, the whole configuration is re-encrypted in the v2 format and saved immediately, so an old configuration upgrades itself the first time you open it with a current build.

### `drivers/`

JDBC jars fetched through the Maven Repository Explorer are saved under `drivers/`, again relative to the working directory. You can also point a driver at a jar anywhere on disk.

## Bundled JDBC drivers

The default configuration defines ten stock driver entries. **The driver jars themselves are not included in the distribution** — for licensing and size reasons you download them yourself, either through the built-in Maven Repository Explorer (which stores them in `drivers/`) or by pointing the **JDBC Jar** field at a file you already have. See [ui-guide.md](ui-guide.md) for both.

| Driver Name | Driver Class | URL Template | Bundled custom queries |
|---|---|---|---|
| Oracle | `oracle.jdbc.OracleDriver` | `jdbc:oracle:thin:@<databaseHost>:1521:<database>` | – |
| PostgreSQL | `org.postgresql.Driver` | `jdbc:postgresql://<databaseHost>:5432/<database>` | – |
| MySQL | `com.mysql.jdbc.Driver` | `jdbc:mysql://<databaseHost>:3306/<database>` | – |
| SQLite | `org.sqlite.JDBC` | `jdbc:sqlite:<database file>` | – (no authentication) |
| H2 Embedded | `org.h2.Driver` | `jdbc:h2:<database file>` | Get table list (no authentication) |
| H2 Server | `org.h2.Driver` | `jdbc:h2:tcp://<databaseHost>[:9092]/<database file>` | Get table list |
| Microsoft SQL Server | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `jdbc:sqlserver://<databaseHost>:1433;databaseName=<database>` | Get table comments, Get column comments |
| MariaDB | `org.mariadb.jdbc.Driver` | `jdbc:mariadb://<databaseHost>:3306/<database>` | – |
| MongoDB | `com.mongodb.jdbc.MongoDriver` | `jdbc:mongodb://<databaseHost>:3306/<database>` | – |
| CUBRID | `cubrid.jdbc.driver.CUBRIDDriver` | `jdbc:cubrid:<databaseHost>:33000:<database>:public::` | – |

Two entries also carry default connection properties:

- **Oracle** — `remarksReporting=true`, so that table and column comments are returned by the JDBC metadata calls.
- **Microsoft SQL Server** — `encrypt=true` and `trustServerCertificate=true`.

The "no authentication" entries (SQLite, H2 Embedded) hide the user name and password fields.

All ten are marked as *stock* items: they cannot be fully edited or deleted in the Driver Manager. To adapt one — a different driver class, extra connection properties, your own metadata SQL — clone it and edit the copy. Custom queries are only needed when a driver's JDBC metadata is incomplete; see [custom-queries.md](custom-queries.md).

## Updating

When the startup check reports a new release and you accept, jdbgen updates itself in place:

1. The release archive is downloaded into `<installation>/.update/`, with a progress window you can cancel at any time.
2. The archive is unpacked and jdbgen exits, handing over to a small updater process that runs from a copy of the jar in `.update/`.
3. The updater replaces the installed files, starts the new version and removes its working files. `.update/updater.jar` and `.update/update.log` are left behind and cleaned up the next time jdbgen starts; the log is where to look if an update went wrong.

The update runs entirely inside the installation directory, so it needs write access there — an installation below `C:\Program Files` or `/opt` owned by another user cannot update itself.

What the updater does with each file:

| Path | |
|---|---|
| `jdbgen-<version>.jar`, `lib/` | replaced — the old jar and the whole `lib/` are moved to `.update/backup/` first |
| `jdbgen.cmd`, `jdbgen.sh`, `resource/` | overwritten, they belong to the release |
| `templates/`, `sample_h2.db.mv.db` | kept; only files that are missing are added, so edited templates and the sample database survive |
| `config.json`, `config.json.*.bak`, `drivers/`, `output/` | never touched |

If anything fails halfway, the previous jar and `lib/` are moved back from `.update/backup/`, the new version is not started, and that backup is kept until the next start — should the restore have failed too, it is the only copy left. The old version keeps running in the meantime.

**Updating by hand.** If the automatic update fails, jdbgen offers to open the releases page. To upgrade manually:

1. Download and unpack the new `jdbgen-<version>.zip` next to the old one.
2. Copy `config.json` (and `drivers/`, if you keep jars there) from the old directory into the new one.
3. Start the new version and enter your master password. If your configuration was written by an older release, the encrypted fields are silently upgraded to the current format on first load.
4. Once you are satisfied, delete the old directory.

Keep a copy of `config.json` before upgrading — it is the only file that holds your work.

## Uninstalling

Delete the unpacked `jdbgen-<version>/` directory. Nothing is installed elsewhere: no registry keys, no files under your home directory, no services. If you ran the jar from other working directories, remove the stray `config.json`, `config.json.*.bak` and `drivers/` those runs created.

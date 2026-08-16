# Installation

jdbgen ships in two forms: a portable ZIP archive for every platform, and a Windows installer (MSI) that carries its own Java runtime. Both contain everything the application needs except the JDBC drivers themselves. This page covers what you need before you start, how to install and run the application, where it keeps its configuration, and what the bundled sample connection and drivers give you out of the box.

[← Documentation index](README.md)

## Requirements

| | |
|---|---|
| Java | Only for the ZIP distribution. The application is compiled for Java 11 (`options.release = 11`), so a **JRE 11 or newer** is enough to run the jar. **The Windows MSI needs no Java at all** — it installs a runtime of its own alongside the application. |
| Operating system | Anything with a desktop Java runtime — Linux, macOS, Windows. The UI is Swing with the [FlatLaf](https://www.formdev.com/flatlaf/) look and feel. The MSI is Windows only. |
| Disk space | About 10 MB for the unpacked ZIP distribution. The MSI is around 55 MB because of the bundled runtime. Add whatever JDBC driver jars you download. |
| Network | Optional. It is only used for the update check, the Maven Repository Explorer, and remote (`http`/`https`) icon URLs. |

If you use the ZIP, make sure the `bin` directory of your Java installation is on `PATH`, or that `JAVA_HOME` points at the installation.

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

Both the ZIP and the MSI are attached to every [release](https://github.com/xcomart/jdbgen/releases/latest). Which one to take depends on the platform and on whether you want a real installation or a directory you can carry around.

### Windows: the installer

1. Download `jdbgen-<version>.msi` from the [latest release](https://github.com/xcomart/jdbgen/releases/latest) and run it.
2. It installs into `C:\Program Files\jdbgen` by default and adds a Start menu entry and a desktop shortcut.

The MSI bundles a Java runtime, so nothing else has to be installed first. Once the package is available in winget, the same installer can be fetched from the command line:

```bat
winget install Xcomart.Jdbgen
```

The Windows Installer entry is keyed on a fixed upgrade code, so a later MSI upgrades the installation in place rather than putting a second copy next to it.

### Every platform: the ZIP archive

1. Download `jdbgen-<version>.zip` from the [latest release](https://github.com/xcomart/jdbgen/releases/latest). This one needs a JRE 11 or newer.
2. Unpack it wherever you like. The archive already contains a top-level `jdbgen-<version>/` directory, so it will not scatter files into the current directory.
3. On Linux/macOS, make the launcher executable if your unzip tool dropped the permission bit:

   ```bash
   chmod +x jdbgen-<version>/jdbgen.sh
   ```

Nothing is written into the unpacked directory beyond what the archive brought; your configuration and downloaded drivers go into the [user data directory](#where-jdbgen-keeps-its-data) instead.

## Directory layout

The installation directory — the unpacked archive, or the `app` directory below the MSI's install location — holds only the files of the release, and jdbgen only ever reads from it:

```
jdbgen-<version>/
├── jdbgen-<version>.jar     the application
├── jdbgen.sh                launcher for Linux/Unix/macOS   (ZIP only)
├── jdbgen.cmd               launcher for Windows            (ZIP only)
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

Everything the application writes — `config.json`, its backups, the `drivers/` directory, the generated files — lives in a per-user directory outside the installation. See [Where jdbgen keeps its data](#where-jdbgen-keeps-its-data). That is what makes an installation below `C:\Program Files` work: it never has to be written to.

> **Do not move the jar out of this directory.** Its manifest declares a `Class-Path` of `lib/*.jar` relative to the jar's own location, so a jar without its sibling `lib/` directory fails to start.

## Running

### From the Start menu (MSI)

The installer generates a native `jdbgen.exe` launcher that starts the bundled runtime. Use the Start menu entry or the desktop shortcut; there is nothing to configure and no console window appears.

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
java -jar /path/to/jdbgen-<version>/jdbgen-<version>.jar
```

This is the recommended route if you only have a JRE, or if you want to pass extra JVM options:

```bash
java -Duser.language=en -jar jdbgen-<version>.jar
```

The directory you run this from does not matter — see below.

### The working directory does not matter

Two directories are told apart, and neither of them is the directory you happen to start jdbgen from:

| Directory | Holds |
|---|---|
| the **user data directory**, a per-user location of the operating system | `config.json`, its `.bak` backups, `drivers/`, the sample database copy — everything the application writes |
| the **installation**, the directory of the running jar | `resource/icon.png`, `resource/loading.gif`, `templates/`, `sample_h2.db.mv.db` — the read-only files of the release |

Starting the jar from an unrelated directory therefore no longer produces a second, empty configuration, and the window icon and busy animation are found either way. The launcher scripts still `cd` into the installation directory, but only because the jar and its `lib/` live there; a plain shell alias works just as well:

```bash
alias jdbgen='java -jar /opt/jdbgen-<version>/jdbgen-<version>.jar'
```

Both locations can be overridden with a system property, which is how you get the old portable behaviour back — everything next to the application, nothing under your home directory:

```bash
java -Djdbgen.dataDir=/media/usb/jdbgen-data -jar jdbgen-<version>.jar
```

`-Djdbgen.resourceBase=<dir>` does the same for the read-only side, for the rare case that `templates/` and `resource/` do not sit next to the jar.

## First run

1. **Update check.** On startup the application queries the GitHub Releases API for `xcomart/jdbgen`. If a newer tag than the running version is found and the installation directory is writable, it offers to update; accepting downloads the new archive, installs it over the current directory and restarts jdbgen. Cancelling the download simply continues the startup. If the installation cannot be written to — an MSI installation below `C:\Program Files`, or a directory owned by another user — jdbgen only reports the new version and tells you how to install it. If the network is unreachable the check fails quietly and startup continues. See [Updating](#updating) for what is replaced and what is kept.

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
   | Connection URL | `jdbc:h2:<user data directory>/sample_h2.db` |
   | Output directory | `<user data directory>/output` |
   | Templates | `Java Model`, `MyBatis mapper`, `PHP CI Model`, as absolute paths into the installation's `templates/` |
   | Author | your OS login name |

   The paths are absolute, so the sample works from anywhere. Building the default configuration copies the release's `sample_h2.db.mv.db` into the user data directory first, because an embedded H2 database has to be writable — the copy in the installation is left alone. The H2 driver jar ships with the release below `drivers/` of the installation, so nothing has to be downloaded first: pick a schema, tick the templates and press **Generate**. Files land in the output directory. See [ui-guide.md](ui-guide.md) for a walkthrough of the windows.

## Where jdbgen keeps its data

Everything the application writes goes into a single per-user directory:

| Platform | Location |
|---|---|
| Windows | `%APPDATA%\jdbgen` — normally `C:\Users\<you>\AppData\Roaming\jdbgen` |
| macOS | `~/Library/Application Support/jdbgen` |
| Linux and other Unix | `$XDG_CONFIG_HOME/jdbgen`, or `~/.config/jdbgen` when that variable is not set |

It holds `config.json`, its `.bak` backups, `drivers/`, the default output directory `output/`, and the copy of the sample database. The directory is created on first use, and `-Djdbgen.dataDir=<dir>` moves it somewhere else — see [The working directory does not matter](#the-working-directory-does-not-matter).

### Upgrading from 0.3.0 or older

Releases up to and including 0.3.0 kept these files next to the application. On the first start of a newer build, if the user data directory has no `config.json` yet and the installation directory (or the directory jdbgen was started from) has one, the configuration, its backups and the whole `drivers/` directory are **copied** across. Nothing is moved or deleted: the old installation keeps working exactly as it did, but from that point on it is the copy in the user data directory that gets updated, so changes made with the new build will not show up in the old one.

The copy happens once. Once the user data directory has a `config.json`, it is never overwritten.

Relative paths inside a configuration — a driver jar as `drivers/h2.jar`, a template as `templates/java_model.java`, the output directory as `output` — are looked for in the user data directory first and in the installation afterwards, which is what keeps a carried-over configuration working. Absolute paths are used as they are. The directory a file chooser picks is stored back the same way: relative when it sits below one of the two directories, absolute otherwise, so it names the same place on the next start whatever directory jdbgen is started from.

### `config.json`

All state — connections, drivers, template presets, abbreviation rules, the Maven search endpoints and the light/dark UI flag — lives in a single `config.json` in the user data directory. It is written whenever you confirm a change in one of the manager windows.

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

JDBC jars fetched through the Maven Repository Explorer are saved under `drivers/` in the user data directory. You can also point a driver at a jar anywhere on disk.

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

The startup check behaves differently depending on whether jdbgen can replace its own files. It finds out by actually creating a file in the installation directory, not by looking at permission bits — a directory below `C:\Program Files` reports itself as writable while every write is denied or redirected.

### An MSI installation, or any read-only installation

There is nothing to accept: the dialog reports the new version, explains that jdbgen is installed where it may not write, and — on Windows — gives you the command to run:

```bat
winget upgrade Xcomart.Jdbgen
```

It then offers to open the [releases page](https://github.com/xcomart/jdbgen/releases/latest), where the MSI of the new version can be downloaded and run by hand. Installing a newer MSI upgrades the existing installation; your configuration and drivers are untouched by it, because they live outside the installation directory.

### An unpacked ZIP installation

When the installation directory is writable and you accept the update, jdbgen updates itself in place:

1. The release archive is downloaded into `<installation>/.update/`, with a progress window you can cancel at any time.
2. The archive is unpacked and jdbgen exits, handing over to a small updater process that runs from a copy of the jar in `.update/`.
3. The updater replaces the installed files, starts the new version and removes its working files. `.update/updater.jar` and `.update/update.log` are left behind and cleaned up the next time jdbgen starts; the log is where to look if an update went wrong.

The update runs entirely inside the installation directory. Your data is not part of it: `config.json`, the backups and `drivers/` live in the user data directory and are never seen by the updater.

What the updater does with each file:

| Path | |
|---|---|
| `jdbgen-<version>.jar`, `lib/` | replaced — the old jar and the whole `lib/` are moved to `.update/backup/` first |
| `jdbgen.cmd`, `jdbgen.sh`, `resource/` | overwritten, they belong to the release |
| `templates/`, `sample_h2.db.mv.db` | kept; only files that are missing are added, so edited templates and the sample database survive |
| anything else in the directory, including a `config.json` left behind by an older release | never touched |

If anything fails halfway, the previous jar and `lib/` are moved back from `.update/backup/`, the new version is not started, and that backup is kept until the next start — should the restore have failed too, it is the only copy left. The old version keeps running in the meantime.

**Updating by hand.** If the automatic update fails, jdbgen offers to open the releases page. To upgrade manually:

1. Download and unpack the new `jdbgen-<version>.zip` next to the old one.
2. Start the new version and enter your master password. There is nothing to copy across: it reads the same `config.json` from the same user data directory. If your configuration was written by an older release, the encrypted fields are silently upgraded to the current format on first load.
3. Once you are satisfied, delete the old directory.

Keep a copy of `config.json` before upgrading — it is the only file that holds your work.

## Uninstalling

**MSI.** Remove *jdbgen* through Settings → Apps → Installed apps, or with `winget uninstall Xcomart.Jdbgen`. The installation directory goes away with it.

**ZIP.** Delete the unpacked `jdbgen-<version>/` directory.

Neither removes your data. `config.json`, its backups, `drivers/` and the generated files stay in the [user data directory](#where-jdbgen-keeps-its-data) — delete that directory too if you want nothing left. Reinstalling later picks the configuration back up. Beyond that directory nothing is installed anywhere: no registry keys beyond the installer's own entry, and no services.

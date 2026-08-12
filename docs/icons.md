# Icons

Connections and drivers each carry an icon, shown next to their name in the lists of the Connection Manager, the Driver Manager and the main window. The icon is stored as a single string, and the prefix of that string decides how it is resolved. This page documents every accepted form, what happens when a value cannot be resolved, and how icons adapt to the current theme.

[← Documentation index](README.md)

You type these strings into the **Icon** field of the Connection Manager and of the Driver Manager; both fields also have a `...` button that opens a file chooser for the local-file case. See [ui-guide.md](ui-guide.md) for where those fields sit.

## Icon string formats

There are six cases, tried in this order:

| # | Form | Example | Resolved as |
|---|---|---|---|
| 1 | `stock:<file>` | `stock:oracle.png` | An icon bundled with the application |
| 2 | `http…` / `https…` | `https://example.com/db.png` | Downloaded from the URL |
| 3 | `fa:<name>` | `fa:table` | A FontAwesome v4.7 glyph |
| 4 | `color:<name>` | `color:blue` | A filled circle in that colour |
| 5 | *anything else* | `/home/user/sample.png` | A path to a local image file |
| 6 | *empty string* | | Falls back to `stock:generic.png` |

**Prefix matching is case-insensitive.** `FA:paw`, `Fa:paw` and `fa:paw` are equivalent, as are `STOCK:`, `Color:` and `HTTPS://`. Only the prefix is case-insensitive — the value after it is not (see the individual sections below).

## Local image file

Any value that does not start with one of the recognised prefixes is treated as a filesystem path, absolute or relative. A relative path is looked for in the [user data directory](installation.md#where-jdbgen-keeps-its-data) first and in the installation afterwards, so it stays valid wherever jdbgen is started from. Picking an icon with the `...` button stores it relative to whichever of the two it sits below, and absolute otherwise.

```
/home/user/sample.png
icons/my-database.png
```

Loading is done by `ImageIO.read()`, so the formats that actually work are the ones the JDK's standard ImageIO readers support:

**JPEG/JPG, PNG, GIF, BMP, WBMP, TIFF.**

> **The file chooser's filter is not the same list.** The `...` button offers `jpg`, `jpeg`, `tiff`, `tif`, `gif`, `png` and `ico`, but that filter only controls which files are *shown*. Two consequences:
>
> - **`.ico` files do not work.** ImageIO has no reader for them, so a `.ico` you picked in the dialog silently falls back to the generic icon.
> - **`.bmp` and `.wbmp` do work**, even though the dialog hides them. Type the path in by hand.

## FontAwesome icons

The application bundles [FontAwesome v4.7](https://fontawesome.com/v4/icons/) through [`jiconfont-font_awesome`](https://jiconfont.github.io/fontawesome) (`jiconfont-font_awesome:4.7.0.1`). Prefix an icon name with `fa:`.

> **Use underscores, not hyphens.** The name after `fa:` is upper-cased and looked up as a Java enum constant (`FontAwesome.valueOf(name.toUpperCase())`), and those constants use underscores. So `fa:window_restore` is correct and **`fa:window-restore` fails** and falls back to the generic icon. Take the name from the FontAwesome site and replace every `-` with `_`.

The glyph is drawn in the current button foreground colour, so it follows the theme — see [Theme and sizing](#theme-and-sizing).

Examples:

|Icon|Field String|
|:---:|:---|
|<img src="images/table.svg" width="17" height="17"/>|`fa:table`|
|<img src="images/eye.svg" width="17" height="17"/>|`fa:eye`|
|<img src="images/window-restore.svg" width="17" height="17"/>|`fa:window_restore`|

## Color bullets

A colour bullet is a simple filled circle — it is the `FontAwesome.CIRCLE` glyph drawn in a fixed colour. Prefix a colour name with `color:`.

The name is upper-cased and resolved as a constant of `java.awt.Color`, which gives exactly these thirteen:

`white`, `light_gray`, `gray`, `dark_gray`, `black`, `red`, `pink`, `orange`, `yellow`, `green`, `magenta`, `cyan`, `blue`.

Unlike `fa:` icons, a colour bullet keeps its colour in both light and dark themes — which is what makes it useful for marking, say, production connections in red regardless of the theme. Bear in mind that `color:white` is nearly invisible on a light theme and `color:black` on a dark one.

Examples:

|Icon|Field String|
|:---:|:---|
|$${\color{blue}&#x2B24;}$$|`color:blue`|
|$${\color{green}&#x2B24;}$$|`color:green`|
|$${\color{red}&#x2B24;}$$|`color:red`|

## Stock icons

These are the images bundled inside the application jar, mostly database brand icons. They are what the ten built-in driver definitions use. Prefix the file name — **including the `.png` extension** — with `stock:`.

|Icon|Field String|
|:---:|:---|
|<img src="../src/main/resources/icons/altibase.png" width="17" height="17"/>|`stock:altibase.png`|
|<img src="../src/main/resources/icons/cubrid.png" width="17" height="17"/>|`stock:cubrid.png`|
|<img src="../src/main/resources/icons/generic.png" width="17" height="17"/>|`stock:generic.png`|
|<img src="../src/main/resources/icons/h2.png" width="17" height="17"/>|`stock:h2.png`|
|<img src="../src/main/resources/icons/mariadb.png" width="17" height="17"/>|`stock:mariadb.png`|
|<img src="../src/main/resources/icons/mongodb.png" width="17" height="17"/>|`stock:mongodb.png`|
|<img src="../src/main/resources/icons/mssql.png" width="17" height="17"/>|`stock:mssql.png`|
|<img src="../src/main/resources/icons/mysql.png" width="17" height="17"/>|`stock:mysql.png`|
|<img src="../src/main/resources/icons/oracle.png" width="17" height="17"/>|`stock:oracle.png`|
|<img src="../src/main/resources/icons/postgresql.png" width="17" height="17"/>|`stock:postgresql.png`|
|<img src="../src/main/resources/icons/sqlite.png" width="17" height="17"/>|`stock:sqlite.png`|

That is the complete list — there is no way to add to it without rebuilding the application; use a local file or a remote URL instead.

`altibase.png` is present even though no built-in driver uses it. It is there for a user-defined Altibase driver: clone a stock driver, set the driver class and URL template, and put `stock:altibase.png` in its **Icon** field.

`generic.png` is the fallback used whenever an icon string cannot be resolved.

> The images in the table above are referenced from the repository sources (`../src/main/resources/icons/`), so they render on GitHub but not from a copy of `docs/` taken out of a release archive, which contains no `src/` directory.

## Remote URL

An icon string starting with `http` or `https` (case-insensitive) is fetched over HTTP with the application's shared OkHttp client and decoded with `ImageIO.read()`, so the same format list as for [local files](#local-image-file) applies.

```
https://example.com/icons/my-database.png
```

Notes:

- The download happens the first time the icon is needed and the result is cached in memory. It is **not** written to disk, so it is fetched again on the next start — and also after a theme change, which clears the icon cache.
- An unreachable host, a non-image response or an unsupported format all end in the generic icon.
- Nothing works offline. If you routinely work without a network, save the image locally and reference it as a file.

## Fallback behavior

Every resolution failure is handled the same way: the exception is caught, a line is written to the log, and the generic icon is used instead.

```
Icon not found. Falling back to use default icon.
```

The failure is **not** shown in the UI. A typo, a hyphen where an underscore belongs, a missing file, a `.ico`, or a dead URL all look identical — the entry simply shows the grey generic icon. If an icon is not what you expected, that is the first thing to check.

The only case that produces a louder complaint is when `stock:generic.png` itself cannot be read, which means the installation is damaged; that is logged as an error. See [troubleshooting.md](troubleshooting.md).

Resolved icons are cached by their exact string, so a corrected value takes effect as soon as you re-enter it — but note that the *misspelled* string stays cached as the generic icon until the theme changes or the application restarts.

## Theme and sizing

Icon geometry follows the UI font, so icons scale with the rest of the interface:

- **Image files and remote URLs** are scaled to `1.2 ×` the point size of the `Button.font` (square).
- **Colour bullets** are drawn at `1.2 ×` that size as well.
- **FontAwesome glyphs** are drawn at the plain `Button.font` size, so they read slightly smaller than the other kinds at the same setting.

Colour follows the theme:

- **FontAwesome icons** are drawn in `Button.foreground`, which is dark on the light theme and light on the dark theme. This is what makes them the safest choice for a custom icon — they stay legible either way. It also means an `fa:` icon is not a fixed colour; you cannot pick one.
- **Colour bullets** use the exact `java.awt.Color` constant you named, in both themes.
- **Image files, remote URLs and stock icons** are used as they are; a PNG with a dark, transparent-background logo will be hard to see on the dark theme.

Switching between the light and dark theme with the **Dark UI** toggle in the main window re-reads `Button.foreground` and `Button.font` and clears the icon cache, so every icon is rebuilt with the new colour and size — including a fresh download for remote URLs.

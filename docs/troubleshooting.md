# Troubleshooting

This page collects the failure modes that are easy to hit and hard to diagnose,
grouped by where they show up. Each entry follows the same shape: the symptom you
observe, the cause behind it, and what to do about it. If your problem is not listed
here, the last section explains how to gather the information needed to report it.

[← Documentation index](README.md)

---

## Startup and launcher

### `./jdbgen.sh` prints "Java too old!" even though Java is recent

**Symptom.** On Linux or macOS the launcher exits immediately with:

```
Java too old!
You need to install JRE(Java Runtime Environment) version 11 or above.
```

even though `java -version` reports 11 or newer.

**Cause.** Releases up to and including 0.2.5 checked the class file version by
running `javap`, not `java`:

```sh
JAVA_VER=$(${JAVA}p -verbose java.lang.String | grep "major version" | cut -d " " -f5)
if [[ "$JAVA_VER" < "55" ]]; then
```

`javap` ships with the JDK but not with a plain JRE. When it is missing, `JAVA_VER`
ends up empty, the string comparison `"" < "55"` is true, and the script aborts.
The same comparison is lexicographic, so it would also have rejected a future
Java 100.

Current releases read `java -version` instead, compare the major version
numerically, and only warn when the version string cannot be parsed.

**Fix.** Upgrade to a release newer than 0.2.5. On an affected release, either
install a full JDK so that `javap` sits next to `java`, or skip the launcher
entirely and start the jar directly from the installation directory:

```sh
cd /path/to/jdbgen-<version>
java -jar jdbgen-<version>.jar
```

The Windows launcher `jdbgen.cmd` performs the same check but only prints a warning
and carries on, so Windows is unaffected.

### Moving the jar somewhere else makes it fail to start

**Symptom.** Copying `jdbgen-<version>.jar` to another directory and running it
there fails with `NoClassDefFoundError` or a similar missing-dependency error.

**Cause.** The jar is not self-contained. Its manifest declares a `Class-Path`
pointing at `lib/<dependency>.jar` — relative entries that are resolved next to the
jar.

**Fix.** Keep the whole extracted distribution together (`jdbgen-<version>.jar`
alongside `lib/`, `templates/` and `resource/`). If you want a launcher elsewhere,
make it `cd` into the installation directory first, as the bundled scripts do.

### Icons are missing, or settings reset every time

**Symptom.** The window icon and the loading animation do not appear, bundled
templates cannot be found, or the app asks to create a new master password on every
start even though you already have a configuration.

**Cause.** `config.json`, the `drivers/` download directory and the `resource/`
directory are all resolved as **relative paths against the current working
directory**, not against the location of the jar. Started from the wrong directory,
the application simply looks for them somewhere else — and finding no
`config.json`, it treats the run as a first run.

**Fix.** Always launch with the installation directory as the working directory.
The bundled `jdbgen.sh` and `jdbgen.cmd` do this for you; if you invoke
`java -jar` yourself, `cd` there first.

---

## Configuration and master password

### I forgot the master password

**Symptom.** The password prompt keeps returning with `Password Incorrect!`.

**Cause and behaviour.** There is no attempt limit. After every three consecutive
failures a dialog appears offering two choices:

- **OK** — try the password again. The counter resets, so you can keep trying
  indefinitely.
- **Cancel** — start with a default configuration.

Choosing the default configuration does **not** delete your existing file. It is
moved aside to `config.json.<yyyyMMdd_HHmmss>.bak` in the same directory, and a
dialog tells you the absolute path it was moved to.

**Fix.** To go back to the old configuration, rename the backup file to
`config.json` and restart. Be aware, however, that three fields of every connection
— **Connection URL**, **User Name** and **User Password** — are stored encrypted
with a key derived from the master password. Without the original password those
values cannot be recovered, so a restored file still needs the password that
encrypted it.

If the password is genuinely lost, the practical path is to start from the default
configuration and re-enter the connection details by hand. Driver definitions,
templates, presets and abbreviation rules are not encrypted and survive in the
backup file, so you can copy them across in a text editor.

### A `config.json` copied from another machine will not open

**Symptom.** An imported configuration always reports an incorrect password, even
though the password you type is the one you use locally.

**Cause.** The encryption key is derived from the master password that was in use
where the file was written. It is not tied to the machine, but it *is* tied to that
password.

**Fix.** Use the master password from the original installation. If it is not
available, see the previous entry.

---

## Databases and drivers

### The table list is empty

**Symptom.** The connection succeeds and the schema tree appears, but no tables are
listed.

**Causes and fixes**, in the order worth checking:

1. **No schema is selected.** Tables are only fetched when a **leaf** node of the
   schema tree is selected — that is, a schema, not the catalog above it. Expand the
   catalog and click a schema.
2. **The tables are views.** Views are hidden unless **Show Views** is checked.
3. **The table type is not recognized.** Types are normalized to `TABLE` or `VIEW`;
   anything that is neither of those, and does not *contain* either word, is dropped
   from the list entirely. Synonyms, aliases and sequences typically disappear this
   way.
4. **The driver does not implement `DatabaseMetaData` properly.** This is the
   classic case. Define a **Get table list** query in the Driver Manager's Custom
   Queries tab — see [custom queries](custom-queries.md#get-table-list-sql).

### Reading the column list fails with an SQLException

**Symptom.** Opening a table shows `Cannot get columns: <message>`, where the
message is an "invalid column name" error such as `Column "IS_KEY" not found`. The
same failure during generation appears in the Progress window as
`process failed! : <message>`, right after `reading table columns...`.

**Cause.** A custom **Get table column list** query is enabled and its result set is
missing one of the required labels. All of `TABLE_CAT`, `TABLE_SCHEM`, `TABLE_NAME`,
`COLUMN_NAME`, `DATA_TYPE`, `TYPE_NAME`, `COLUMN_SIZE`, `NULLABLE`, `REMARKS`,
`COLUMN_DEF` and `IS_KEY` are read by label, and `IS_KEY` is the one most often
forgotten because JDBC has no equivalent for it.

**Fix.** Alias every required column in the query. If the table has no primary key,
still return `IS_KEY` — just make it `0` for every row. See the
[Get Column List SQL contract](custom-queries.md#get-column-list-sql).

### Primary keys are not detected

**Symptom.** With a custom column list query in place, `${keys}` is empty in
templates and every column ends up in `${notKeys}`.

**Cause.** When a custom column list query is enabled,
`DatabaseMetaData.getPrimaryKeys()` is never called; the `IS_KEY` field is the only
source of truth. That field is read as a string and then parsed as an integer, and
the parser returns `0` for anything containing a non-numeric character. `'Y'`,
`'true'` and `'YES'` therefore all mean "not a key".

**Fix.** Return numeric `0` or `1`. If your query naturally produces a boolean,
wrap it: `CASE WHEN ... THEN 1 ELSE 0 END AS "IS_KEY"`.

### Column comments disappear

**Symptom.** Comments that the driver reports correctly on its own are blank once a
custom **Get table column comments** query is enabled — sometimes for all columns,
sometimes for a subset.

**Cause.** The comment query's rows are collected into a map, and that map is then
applied to *every* column of the table. A column with no matching row gets `null`.
This is different from table comments, which are only assigned when a name matches
and therefore never erase anything.

**Fix.** Return one row per column, or disable the query. Also check letter case:
the join between the comment query and the column list is case sensitive, so a query
returning upper-case names against a lower-case column list wipes every comment.

### The Driver Class list does not appear

**Symptom.** Clicking the **Driver Class** field in the Driver Manager does nothing.

**Cause.** The popup is built by scanning the jar named in the **JDBC Jar** field.
With that field empty, or with a jar containing no `java.sql.Driver` implementation,
there is nothing to show and no popup is displayed.

**Fix.** Fill in **JDBC Jar** first — with the **...** button, or by downloading the
driver through **Download jdbc driver from Maven Repository** — then click the
**Driver Class** field. You can also type the class name by hand.

### Keep Alive does not seem to fire

**Symptom.** The connection still times out despite **Keep connection alive using
below statement every N seconds** being enabled with a statement filled in.

**Cause.** Keep-alive is skipped entirely unless the interval parses as a positive
whole number of seconds and the statement is non-blank. Values such as `30s`,
`1.5`, `0` or a blank line disable it, with a warning in the log. A tick is also
skipped whenever the connection is already busy with a metadata query or a
generation run — which is harmless, since the connection is plainly not idle then.

**Fix.** Check the log for `invalid keep-alive interval` and
`keep-alive statement failed`. Enter the interval as a plain integer, and use a
statement the driver accepts on its own (`select 1` on most databases, `select 1
from dual` on Oracle).

---

## Templates

### `${remark}` (or another field name) renders as an empty string

**Symptom.** A placeholder produces nothing at all, with no error.

**Cause.** Unknown fields are not an error. The generator looks the name up on the
metadata object, then in the connection's custom variables, and if neither has it,
substitutes an empty string. Typos therefore fail silently. The table comment field
is `remarks`, not `remark`.

**Fix.** Check the log for the warning that is emitted for every failed lookup:

```
cannot find 'remark' information from database/custom variables
```

The full list of valid field names is in the
[template reference](template-reference.md).

### `.abbr` does nothing on upper-case names

**Symptom.** Abbreviation rules apply to some names and not others — typically they
work on lower-case names and are ignored on the upper-case names that most databases
report.

**Cause.** There are two kinds of rule and they behave differently:

- **Total name rules** compare the whole name after lower-casing it, so they are
  case insensitive and work as expected.
- **Word rules** split the name on `_` and `-` and look each segment up in a map
  whose keys were stored lower-cased — but the segment itself is **not**
  lower-cased before the lookup. A segment like `CUST` never matches the stored key
  `cust`.

**Fix.** Lower-case the value before abbreviating, then re-case it afterwards:

```
${name.lower.abbr.pascal}
```

### A `matches` condition never fires

**Symptom.** `${if:item=name,matches=...}` behaves as if the pattern never matched.

**Cause.** The condition uses `Pattern.matches`, which requires the **entire** value
to match, and the comparison is case sensitive. Unlike `contains`, `startsWith` and
`endsWith` — which all lower-case both sides — `matches` does not.

**Fix.** Anchor the pattern to the whole string. To test for a substring, write
`.*X.*` rather than `X`, and spell the case out (`[Cc]ode`) or use an inline flag
(`(?i)code`).

### Only one of two identical conditions is evaluated

**Symptom.** An `if` statement carrying the same condition name twice, for example
`${if:item=name,contains=cd,contains=no}`, applies only the last one.

**Cause.** The statement's options are parsed into a map keyed by option name, so a
repeated name overwrites the earlier value before any evaluation happens.

**Fix.** Use each condition name at most once per `if`. Nest a second `if` inside
the first when you need two tests on the same key.

### `padSize` misaligns columns with non-ASCII names

**Symptom.** Padding lines up for ASCII identifiers but drifts for Korean (or other
multi-byte) column names and comments.

**Cause.** The padding width is computed from the **EUC-KR byte length** of the
value, not its character count. A Hangul syllable counts as two, which matches a
fixed-width terminal font but not a proportional one — and does not match at all for
scripts outside EUC-KR.

**Fix.** Budget two per Hangul character when choosing `padSize`. For text where the
byte model does not apply, avoid `padSize` and align with an explicit separator
instead.

### Indentation inside a `for` loop comes out wrong

**Symptom.** The first iteration is placed correctly but subsequent lines are flush
left, or the body of the loop is indented in a way `indent` will not correct.

**Cause.** `indent` only applies to the fragments that follow a line break **inside
`inStr`**, the separator emitted between iterations. Line breaks written inside the
loop body itself are copied verbatim and are never re-indented.

**Fix.** Use the established idiom: keep the body on the same line as the `for`
statement and let the separator carry the line break.

```
SELECT  ${for:item=columns, inStr="\n,", indent=-1}${column} AS "${name.camel}"${endfor}
```

`templates/mybatis_mapper.xml` in the distribution is a working reference for this
pattern, including how to break a long `for` header across lines by putting the
closing `}` on the next line.

### `${'a\nb'}` does not produce a line break

**Symptom.** A backslash escape inside a text-escape statement is emitted as the
plain character.

**Cause.** The two contexts treat backslashes differently:

- Inside a **text escape statement** (`${'...'}` or `${"..."}`), a backslash means
  "emit the next character literally". `\n` therefore yields the letter `n`, and
  `\'` yields a quote. This is what makes it possible to write `${` and `}` as text.
- Inside an **option value** (`inStr='\n,'`, `replace('\t','')`), the usual escapes
  are decoded: `\n`, `\r` and `\t` become real control characters, and any other
  escaped character is passed through unchanged.

**Fix.** For a literal newline in output, put it in an option value, or write an
actual line break in the template. A trailing backslash at the end of an option
value is a parse error (`Dangling escape character at end of`).

### An `if` inside a `for` cannot test a table field

**Symptom.** Inside a `for` loop over `columns`, conditions can only be written
against the column being iterated.

**Cause.** `${super:...}` reaches the enclosing table object when emitting a value,
but there is no `super` variant of the `if` statement — conditions always resolve
against the current item.

**Fix.** Move the table-level test outside the loop, wrapping the whole `for` in an
`if`, and keep the inner `if` for column-level tests.

### Generation stops part-way through

**Symptom.** The Progress window shows

```
process failed! : <message>
```

and some files were written while others were not.

**Cause.** An exception during template processing aborts that template. The message
after the colon is the underlying error — commonly a parse error in the template, or
a missing member such as `Model has no 'xxx' member`.

**Fix.** Read the message, fix the template, and run again. Files already written
are not rolled back, so clear the output directory if a partial result would be
confusing.

---

## Known issues

These are confirmed defects in the current release rather than configuration
mistakes. Each has a workaround.

### Items created with **+**, **c**/**C** or **New Preset from Current Connection** survive **Cancel**

New and cloned connections, drivers and presets are appended to the in-memory list
the moment the button is pressed. Closing the dialog with **Cancel** does not remove
them, so they are still present afterwards and get written to `config.json` by the
next save from anywhere in the application.

**Workaround:** if you created an entry by mistake, delete it explicitly with the
**-** button rather than relying on **Cancel**.

---

## Reporting a problem

Please open an issue at <https://github.com/xcomart/jdbgen/issues>.

**Include the log.** jdbgen logs to standard output, so what you see depends on how
you started it:

- **Linux/macOS** — run `./jdbgen.sh` from a terminal, or
  `java -jar jdbgen-<version>.jar` from the installation directory. The log appears
  in that terminal. Capture it with
  `java -jar jdbgen-<version>.jar > jdbgen.log 2>&1`.
- **Windows** — `jdbgen.cmd` starts the application with `javaw.exe`, which has no
  console and discards the output. To see it, open a Command Prompt in the
  installation directory and run `java -jar jdbgen-<version>.jar` instead.

The warnings that most often explain a problem are:

| Log message | Meaning |
|:---|:---|
| `cannot find '<x>' information from database/custom variables` | A template referenced a field that does not exist; it rendered as an empty string. |
| `primary key column '<x>' of table '<y>' not found in column list` | The driver reports primary key columns with different letter case than the column list. |
| `cannot load configuration 'config.json'` | Wrong master password, or a corrupted configuration file. |
| `existing configuration could not be loaded, backed up to '<path>'` | Your previous `config.json` was preserved at that path. |

**Also include:** the jdbgen version (shown in the About dialog), your operating
system, `java -version` output, the JDBC driver and database version, and — for
metadata problems — the custom queries you have configured. Remove passwords and
host names from anything you paste.

---

## Related documentation

- [Installation](installation.md) — supported Java versions and distribution layout.
- [UI guide](ui-guide.md) — where each control mentioned above lives.
- [Custom queries](custom-queries.md) — the full contract for driver-specific
  metadata SQL.
- [Template reference](template-reference.md) — statements, decorators and available
  fields.
- [Icons](icons.md) · [Building from source](building.md)

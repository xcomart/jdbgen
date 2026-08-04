# Template Reference

A jdbgen template is an ordinary text file with placeholders in it. jdbgen reads
the metadata of every table you selected, applies the template once per table,
and writes the result to a file whose name is itself a small template. This page
documents every statement the template engine understands, the fields it exposes,
and what happens when something goes wrong.

[← Documentation index](README.md)

- [Quick example](#quick-example)
- [Syntax overview](#syntax-overview)
- [`item` statement](#item-statement)
- [Table and column fields](#table-and-column-fields)
- [Control statements](#control-statements)
- [Other statements](#other-statements)
- [Custom variables](#custom-variables)
- [Abbreviations](#abbreviations)
- [Error handling](#error-handling)
- [Recipes](#recipes)

## Quick example

Every example on this page uses the same table. Create it with:

```sql
create table t_sample_album (
  album_id int not null,
  album_name varchar(256) not null,
  artist_name varchar(512) not null,
  publish_date DATE,
  primary key (album_id)
);
comment on table t_sample_album is 'Music Album';
comment on column t_sample_album.album_id is 'Album identifier';
comment on column t_sample_album.album_name is 'Album display name';
comment on column t_sample_album.artist_name is 'Creator artist name';
comment on column t_sample_album.publish_date is 'Published date';
```

With this template:

```java
/**
 * ${remarks} Model class
 *
 * @author ${author}
 * @version 1.0 ${date:yyyy-MM-dd}
 */
class ${table.suffix.pascal}Model {
    ${for:item=columns}// ${remarks}
    private ${item:key=javaType, padSize=10, padDir=right} ${name.camel};
    ${endfor}
    // Getters and Setters
    ${for:item=columns}
    // get ${remarks}
    public ${javaType} ${if:item=javaType, equals='boolean'}is${else}get${endif}${name.pascal}() {
        return ${name.camel};
    }

    // set ${remarks}
    public void set${name.pascal}(${javaType} ${name.camel}) {
        this.${name.camel} = ${name.camel};
    }
    ${endfor}
}
```

jdbgen generates:

```java
/**
 * Music Album Model class
 *
 * @author John Doe
 * @version 1.0 2026-08-04
 */
class SampleAlbumModel {
    // Album identifier
    private Integer    albumId;
    // Album display name
    private String     albumName;
    // Creator artist name
    private String     artistName;
    // Published date
    private Date       publishDate;
    
    // Getters and Setters
    
    // get Album identifier
    public Integer getAlbumId() {
        return albumId;
    }

    // set Album identifier
    public void setAlbumId(Integer albumId) {
        this.albumId = albumId;
    }
    
    // get Album display name
    public String getAlbumName() {
        return albumName;
    }

    // set Album display name
    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }
    
    // get Creator artist name
    public String getArtistName() {
        return artistName;
    }

    // set Creator artist name
    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
    
    // get Published date
    public Date getPublishDate() {
        return publishDate;
    }

    // set Published date
    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }
    
}
```

Note the lines that contain nothing but the four spaces preceding `${endfor}`:
the body of a `for` loop is copied out exactly as written, line breaks and
indentation included. See [`for` statement](#for-statement) for the details, and
[Recipes](#recipes) for layouts that avoid stray blank lines.

## Syntax overview

A statement starts with `${` and ends at the **first** `}` after it. Everything
outside statements is copied to the output byte for byte.

```
${<statement type>:<name>=<value>[, <name>=<value> ...]}
${<field name>[<decorators>]}
```

The available statement types are:

| Statement | Form | Purpose |
|:---|:---|:---|
| [`item`](#item-statement) | `${<field>[<decorators>]}`<br>`${item:key=<field>[<decorators>][, <extra decorators>]}` | Insert a table field, a column field or a custom variable |
| [`super`](#super) | `${super:key=<table field>[<decorators>][, <extra decorators>]}` | Reach the table object from inside a `for` loop |
| [`if`](#if-statement) | `${if:item=<field>, <condition>}` … `${elif:…}` … `${else}` … `${endif}` | Branch on a field value |
| [`for`](#for-statement) | `${for:item=<collection field>[, <controls>]}` … `${endfor}` | Repeat over a collection |
| [`author`](#author-user-and-date) | `${author[:<extra decorators>]}` | The `Author Name` field of the Generator window |
| [`user`](#author-user-and-date) | `${user[:<extra decorators>]}` | The OS login user id |
| [`date`](#author-user-and-date) | `${date[:<format>]}`, `${date[:format=<format>[, <extra decorators>]]}` | The current date |
| [Text escape](#text-escape) | `${'any text'}`, `${"any text"}` | Emit text that itself contains `${` or `}` |

A statement without a `:` is shorthand for `item:key=`, so `${name.camel}` and
`${item:key=name.camel}` are the same thing.

### Whitespace, quoting and escapes

- Whitespace inside a statement is insignificant, including line breaks. Both
  `${item:key=name}` and

  ```
  ${
    item : key = name , padSize = 15
  }
  ```

  behave identically — the line breaks inside the braces do not reach the
  output. Long `for` headers in the bundled templates use this to stay readable.
- Option values may be quoted with `'` or `"`. One matching pair of surrounding
  quotes is removed, so `equals='TABLE'`, `equals="TABLE"` and `equals=TABLE`
  are equivalent. Quote a value when it contains a space, a comma or an `=`.
- Inside an option value, `\n`, `\r` and `\t` become the real control
  characters, and `\<any other char>` becomes that character. This is what makes
  `inStr="\n,"` work.

  > **Note**
  > A [text escape](#text-escape) statement uses a *different* rule: there,
  > `\<char>` always yields the character itself, so `${'a\nb'}` prints `anb`,
  > not `a`, newline, `b`.

- A `}` inside a quoted option value still ends the statement, because the
  engine looks for the first `}` without tracking quotes. Use a text escape
  statement to emit a literal `}`.

### Case sensitivity

| Element | Case |
|:---|:---|
| Statement names (`item`, `super`, `if`, `for`, `date`, `user`, `author`) | Ignored — `${IF:ITEM=type, EQUALS='TABLE'}` works |
| Option names (`key`, `item`, `padSize`, `inStr`, `indent`, …) | Ignored |
| Decorator names (`.suffix`, `.pascal`, …) | Ignored — `${name.SUFFIX.PASCAL}` works |
| Field names (`remarks`, `javaType`, …) | Ignored (resolved through Java accessors) |
| `endif`, `endfor`, `else`, `elif:` | **Lower case only** |

> **Note**
> `${ENDIF}` and `${ENDFOR}` are not recognised as terminators, so the statement
> stays open and parsing fails with `if statements not closed` /
> `for statements not closed`. `${ELSE}` is worse: it is silently parsed as an
> `item` statement for a field named `ELSE`, which resolves to an empty string,
> so the `else` branch simply disappears without any error. Always write these
> four in lower case.

### Escaping `${`

Anything a template must emit literally — including `${` and `}` — goes into a
text escape statement:

| Template | Output |
|:---|:---|
| `${'Test sample with ${author}'}` | `Test sample with ${author}` |
| `${"literal ${author} and }"}` | `literal ${author} and }` |

## `item` statement

`item` inserts the value of a field of the object the template is currently
applied to — the table, or the column when inside a
[`for` loop](#for-statement).

```
${<field name>[<decorators>]}
${item:key=<field name>[<decorators>][, <extra decorators>]}
${item:item=<field name>…}
```

`key=` and `item=` are interchangeable in **every** statement that takes a field
name (`item`, `super`, `if` and `for`). The shorthand form cannot take extra
decorators; use the `item:key=` form for those.

> **Note**
> The shorthand form must not contain a `:`, because the engine splits the
> statement at the first colon to find the statement type.
> `${name.replace('_',':')}` fails with `Unknown template: …`; write
> `${item:key=name.replace('_',':')}` instead.

### Decorators

Decorators are appended to the field name with a `.` and are applied left to
right, each one receiving the previous one's result.

| Decorator | Description |
|:---|:---|
| `.abbr` | Apply the [abbreviation rules](#abbreviations) to the value |
| `.suffix` | Remove everything up to and including the **first** `_` (`T_SAMPLE_ALBUM` → `SAMPLE_ALBUM`) |
| `.prefix` | Remove everything from the **last** `_` onwards (`SAMPLE_ALBUM_T` → `SAMPLE_ALBUM`) |
| `.camel` | Camel case (`SAMPLE_ALBUM` → `sampleAlbum`) |
| `.pascal` | Pascal case (`SAMPLE_ALBUM` → `SampleAlbum`) |
| `.snake` | Snake case (`SAMPLE_ALBUM` → `sample_album`) |
| `.screaming` | Screaming snake case (`sampleAlbum` → `SAMPLE_ALBUM`) |
| `.skewer` | Skewer case (`SAMPLE_ALBUM` → `sample-album`) |
| `.kebab` | Alias of `.skewer` |
| `.lower` | Lower case |
| `.upper` | Upper case |
| `.replace(<find>, <replacement>)` | Replace every occurrence of `<find>` with `<replacement>` |

`.suffix` and `.prefix` return the value **unchanged** when it contains no `_`,
so `${type.suffix}` on a `TABLE` type yields `TABLE`.

`.replace` takes its two arguments quoted, unquoted or mixed:
`${name.replace('SAMPLE','TEST')}`, `${name.replace(_, -)}` and
`${name.replace(ghi, 'xyz')}` all work.

Any decorator may be repeated and combined with any other — the chain is a
plain pipeline with no restrictions. `${name.upper.lower}` and
`${name.replace('SAMPLE','TEST').suffix.pascal}` are both valid.

Examples on `T_SAMPLE_ALBUM`:

| Template | Output |
|:---|:---|
| `${name.suffix}` | `SAMPLE_ALBUM` |
| `${name.prefix}` | `T_SAMPLE` |
| `${name.suffix.pascal}` | `SampleAlbum` |
| `${name.suffix.camel}` | `sampleAlbum` |
| `${name.suffix.kebab}` | `sample-album` |
| `${name.replace('SAMPLE','TEST').suffix.pascal}` | `TestAlbum` |
| `${item:key=name.suffix.camel.screaming}` | `SAMPLE_ALBUM` |

### Extra decorators

Extra decorators are comma-separated options of the `item:key=` form. They also
work on [`super`](#super), [`author`, `user` and `date`](#author-user-and-date).

| Extra decorator | Description |
|:---|:---|
| `padSize=<size>` | Pad the value with spaces up to `<size>` |
| `padDir=<direction>` | `left` or `right` — the side the spaces go to. Default `right` |
| `quote=<quote>` | Wrap the value in `<quote>` on both sides |
| `prepend=<text>` | Put `<text>` in front of the value |
| `postpend=<text>` | Put `<text>` behind the value |

Order of application:

1. `prepend` (or `quote`) is put in front, `postpend` (or `quote`) behind.
   `prepend`/`postpend` **override** `quote` on their side, so
   `${item:key=type, quote='"', prepend='<'}` yields `<TABLE"`.
2. Padding is then computed on the **already decorated** string — quotes count
   towards `padSize`.

| Template | Output |
|:---|:---|
| `[${item:key=name.suffix, padSize=20, quote="'", padDir=right}]` | `['SAMPLE_ALBUM'      ]` |
| `[${item:key=type, padSize=10, padDir=left}]` | `[     TABLE]` |
| `[${item:key=type, padSize=10}]` | `[TABLE     ]` |
| `[${item:key=name, padSize=3}]` | `[T_SAMPLE_ALBUM]` |

> **Note**
> Padding counts **EUC-KR bytes, not characters**, so one Hangul syllable counts
> as two. `[${item:key=remarks, padSize=10}]` on a table whose comment is `음반`
> produces `[음반      ]` — two characters plus six spaces. The same byte-based
> measurement is used for the `for` loop indentation base.

A value longer than `padSize` is never truncated.

An unknown or empty field still goes through the extra decorators, because it is
resolved to an empty string first: `[${item:key=nvlColName, padSize=8,
quote='#'}]` produces `[##      ]`. The one exception is a statement whose value
is genuinely absent — `${author:quote='#', padSize=8}` with no `Author Name`
entered emits nothing at all, not even the quotes.

### Resolution order

For every `item` (and `super`) statement the engine looks for the name in this
order:

1. a field or accessor of the current object (table or column),
2. a [custom variable](#custom-variables),
3. otherwise it logs a warning and substitutes an **empty string**.

An unknown *field* therefore never stops generation; an unknown *decorator*
does. See [Error handling](#error-handling).

## Table and column fields

### Table fields

| Field | Type | Description |
|:---|:---|:---|
| `catalog` | String | Database catalog containing this table |
| `schema` | String | Database schema containing this table |
| `name` | String | Table name |
| `table` | String | Alias of `name` |
| `title` | String | Accessor that also returns the table name |
| `type` | String | `TABLE` or `VIEW` |
| `remarks` | String | Table comment |
| `columns` | List of column | All columns |
| `keys` | List of column | Primary key columns |
| `notKeys` | List of column | All columns except the primary keys |
| `icon` | String | The icon key the table list uses (`fa:TABLE` or `fa:EYE`) — see [Icons](icons.md) |
| `no` | int | Always `0` for tables; nothing assigns it |
| `source` | String | Declared but never populated — **always empty** |

> **Note**
> The table comment field is `remarks`, **not** `remark`. `${remark}` is not a
> field, so it logs a warning and expands to an empty string.

### Column fields

| Field | Type | Description |
|:---|:---|:---|
| `catalog` | String | Database catalog containing this column |
| `schema` | String | Database schema containing this column |
| `table` | String | Table this column belongs to |
| `name` | String | Column name |
| `column` | String | Alias of `name` |
| `typeName` | String | Database type name as reported by the driver (`VARCHAR`, `INT`, …) |
| `typeString` | String | `typeName` in upper case, with `(<length>)` appended for `CHAR`/`BINARY` types (`VARCHAR(256)`). Lengths above 1,000,000 render as `(max)` |
| `length` | int | Column size |
| `nullable` | short | `0` = not nullable, `1` = nullable |
| `isKey` | boolean | `true` when the column is part of the primary key |
| `isCharType` | boolean | `true` when `typeName` contains `CHAR`, `CLOB` or `TEXT` |
| `remarks` | String | Column comment |
| `defVal` | String | Default value |
| `dataType` | short | Raw `java.sql.Types` constant (`4`, `12`, `91`, …) |
| `jdbcType` | String | JDBC type name derived from `dataType` |
| `javaType` | String | Java type name derived from `dataType` |
| `no` | int | 1-based position, see [`for` statement](#for-statement) |
| `nvlColName` | String | Declared but never populated — **always empty** |

For the sample table:

| Template | Output |
|:---|:---|
| `${remarks}` | `Music Album` |
| `${name}` / `${table}` / `${type}` | `T_SAMPLE_ALBUM` / `T_SAMPLE_ALBUM` / `TABLE` |
| `${for:item=columns, inStr=","}${name}:${javaType}:${jdbcType}:${typeString}${endfor}` | `ALBUM_ID:Integer:INTEGER:INT,ALBUM_NAME:String:VARCHAR:VARCHAR(256),ARTIST_NAME:String:VARCHAR:VARCHAR(512),PUBLISH_DATE:Date:DATE:DATE` |

### JDBC and Java type mapping

`jdbcType` and `javaType` are looked up from the numeric `DATA_TYPE` the JDBC
driver reports (`dataType`), *not* from `typeName`.

| `java.sql.Types` | `dataType` | `jdbcType` | `javaType` |
|:---|---:|:---|:---|
| `ARRAY` | 2003 | `ARRAY` | `array` |
| `BIGINT` | -5 | `BIGINT` | `Long` |
| `BINARY` | -2 | `BINARY` | `byte[]` |
| `BIT` | -7 | `BIT` | `Boolean` |
| `BLOB` | 2004 | `BLOB` | `byte[]` |
| `BOOLEAN` | 16 | `BOOLEAN` | `Boolean` |
| `CHAR` | 1 | `CHAR` | `String` |
| `CLOB` | 2005 | `CLOB` | `String` |
| `DATALINK` | 70 | `DATALINK` | `String` |
| `DATE` | 91 | `DATE` | `Date` |
| `DECIMAL` | 3 | `DECIMAL` | `Integer` |
| `DISTINCT` | 2001 | `DISTINCT` | `String` |
| `DOUBLE` | 8 | `DOUBLE` | `Double` |
| `FLOAT` | 6 | `FLOAT` | `Float` |
| `INTEGER` | 4 | `INTEGER` | `Integer` |
| `JAVA_OBJECT` | 2000 | `JAVA_OBJECT` | `String` |
| `LONGNVARCHAR` | -16 | `LONGNVARCHAR` | `String` |
| `LONGVARBINARY` | -4 | `LONGVARBINARY` | `byte[]` |
| `LONGVARCHAR` | -1 | `LONGVARCHAR` | `String` |
| `NCHAR` | -15 | `NCHAR` | `String` |
| `NCLOB` | 2011 | `NCLOB` | `String` |
| `NULL` | 0 | `NULL` | `null` |
| `NUMERIC` | 2 | `NUMERIC` | `Integer` |
| `NVARCHAR` | -9 | `NVARCHAR` | `String` |
| `OTHER` | 1111 | `OTHER` | `String` |
| `REAL` | 7 | `REAL` | `Float` |
| `REF` | 2006 | `REF` | `ref` |
| `ROWID` | -8 | `ROWID` | `Integer` |
| `SMALLINT` | 5 | `SMALLINT` | `Short` |
| `SQLXML` | 2009 | `SQLXML` | `String` |
| `STRUCT` | 2002 | `STRUCT` | `struct` |
| `TIME` | 92 | `TIME` | `Time` |
| `TIMESTAMP` | 93 | `TIMESTAMP` | `String` |
| `TINYINT` | -6 | `TINYINT` | `Short` |
| `VARBINARY` | -3 | `VARBINARY` | `byte[]` |
| `VARCHAR` | 12 | `VARCHAR` | `String` |

> **Note**
> The Java mapping has sharp edges you must work around in your templates:
>
> - `TIMESTAMP` maps to `String`, not to a date type. `DATE` maps to `Date` and
>   `TIME` to `Time`, neither of them qualified with a package.
> - `DECIMAL` and `NUMERIC` map to `Integer`, ignoring precision and scale, so a
>   `NUMERIC(18,2)` column produces `Integer`.
> - `TINYINT` maps to `Short`, `ROWID` to `Integer`.
> - `ARRAY`, `REF`, `STRUCT` and `NULL` map to the lower-case words `array`,
>   `ref`, `struct` and `null`, which are not Java types at all.
>
> Codes outside the table above — `TIME_WITH_TIMEZONE` (2013),
> `TIMESTAMP_WITH_TIMEZONE` (2014), `REF_CURSOR` (2012) and vendor-specific
> codes — have no mapping, so both `${jdbcType}` and `${javaType}` expand to an
> empty string (with a warning in the log).
>
> When a mapping does not suit you, branch on `typeName` or `dataType` yourself:
> `${if:item=typeName, startsWith='timestamp'}LocalDateTime${else}${javaType}${endif}`.

## Control statements

### `if` statement

```
${if:item=<field>[<decorators>], <condition>[, <condition> ...]}
 ...                                   // conditions met
[${elif:item=<field>, <condition>[, <condition> ...]}]
 ...                                   // another condition met, repeatable
[${else}]
 ...                                   // nothing matched
${endif}
```

`item=` and `key=` are interchangeable, and the field name may carry
[decorators](#decorators): `${if:item=name.suffix.camel, startsWith='sample'}`.
Multiple conditions in one `if` are combined with **AND**. `elif` may be
repeated, `else` is optional, and `endif` is mandatory.

| Condition | True when the value … |
|:---|:---|
| `equals=<value>` / `value=<value>` | equals `<value>` |
| `notEquals=<value>` | does not equal `<value>` |
| `startsWith=<prefix>` | starts with `<prefix>` |
| `notStartsWith=<prefix>` | does **not** start with `<prefix>` |
| `endsWith=<suffix>` | ends with `<suffix>` |
| `notEndsWith=<suffix>` | does **not** end with `<suffix>` |
| `contains=<value>` | is a collection holding an element whose `name` is `<value>`, or is a string equal to one of the comma-separated tokens of `<value>` |
| `notContains=<value>` | the negation of `contains` |
| `matches=<regex>` | matches the [Java regular expression](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) `<regex>` **entirely** |
| `notMatches=<regex>` | does not match `<regex>` entirely |

#### Case sensitivity of conditions

This trips people up often enough to deserve its own list:

| Condition | Case | Matching |
|:---|:---|:---|
| `equals`, `value`, `notEquals` | **ignored** | whole value |
| `startsWith`, `notStartsWith` | **ignored** | prefix |
| `endsWith`, `notEndsWith` | **ignored** | suffix |
| `contains`, `notContains` | **ignored** | collection: each element's `name`; string: each `,`-separated token, compared whole |
| `matches`, `notMatches` | **respected** | whole value — `Pattern.matches`, not a search |
| `skipList` of [`for`](#for-statement) | **respected** | exact, whole column name |

> **Note**
> `matches` anchors at both ends. `matches='SAMPLE'` does **not** match
> `T_SAMPLE_ALBUM`; write `matches='.*SAMPLE.*'`. And unlike every other
> condition, it is case-sensitive: `matches='t_sample_album'` does not match
> `T_SAMPLE_ALBUM` either.

For a string value, `contains` reads as *"is one of"*:
`${if:item=type, contains='TABLE,VIEW'}` is true when `type` is exactly `TABLE`
or exactly `VIEW`. Tokens are trimmed, so `'TABLE, VIEW'` works too.

#### Multiple conditions

Conditions are stored under their name, so **the same condition name twice keeps
only the last one**:

```
${if:item=type, equals='VIEW', equals='TABLE'}A${else}B${endif}
```

evaluates only `equals='TABLE'` and prints `A` for a table. Use `elif` or
`contains='VIEW,TABLE'` when you mean "or".

Any option name that is neither a condition, `key` nor `item` is rejected while
parsing — `if` accepts no [extra decorators](#extra-decorators):

```
${if:item=type, weird='x'}A${endif}
→ ParseException: Unknown if condition: item=type, weird='x'
```

#### Examples

| Template | Output |
|:---|:---|
| `${if:item=type, equals='table'}YES${else}NO${endif}` | `YES` (case is ignored) |
| `${if:item=type, value='TABLE'}YES${endif}` | `YES` |
| `${if:item=type, equals='VIEW'}V${elif:item=type, equals='TABLE'}T${else}X${endif}` | `T` |
| `${if:item=name, startsWith='t_'}YES${endif}` | `YES` |
| `${if:item=columns, contains='album_id'}HAS${else}NO${endif}` | `HAS` |
| `${if:item=name, matches='SAMPLE'}FULL${else}NOTFULL${endif}` | `NOTFULL` |
| `${if:item=name, matches='.*SAMPLE.*'}M${else}N${endif}` | `M` |

### `for` statement

```
${for:item=<collection field>[, <controls>]}
 ...    // repeated once per element
${endfor}
```

Inside the loop the current object is the **element**, so `${name}` is the
column name. The table is still reachable through [`super`](#super).

| Control | Description |
|:---|:---|
| `inStr=<separator>` | Text inserted **between** iterations (not before the first or after the last) |
| `indent=<spaces>` | Integer, may be negative — adjusts the indentation applied to `inStr` fragments |
| `skipList=<names>` | Comma-separated element names to skip; matched case-**sensitively**, whole name, tokens are trimmed |

`item=` and `key=` are interchangeable here too. The collection field is any
member that is a `List`; on a table object those are:

| Collection field | Description |
|:---|:---|
| `columns` | All columns of the table |
| `keys` | Primary key columns |
| `notKeys` | All columns except the primary keys |

A member that exists but is not a list (`${for:item=type}`) fails with a
`ClassCastException`; a member that does not exist at all fails with
`Model has no '<name>' member`.

#### `${no}` — the iteration counter

At every iteration the engine assigns the element's 1-based position to its `no`
field, so `${no}` numbers the loop:

| Template | Output |
|:---|:---|
| `${for:item=columns, inStr=","}${no}:${name}${endfor}` | `1:ALBUM_ID,2:ALBUM_NAME,3:ARTIST_NAME,4:PUBLISH_DATE` |
| `${for:item=notKeys, inStr=","}${no}:${name}${endfor}` | `1:ALBUM_NAME,2:ARTIST_NAME,3:PUBLISH_DATE` |

The number is the position inside the collection **being iterated**, so in a
`notKeys` loop `ALBUM_NAME` is `1`, even though it is the second column of the
table. Numbers are taken before `skipList` is applied, so skipping leaves gaps:
`${for:item=columns, inStr=',', skipList='ALBUM_NAME'}${no}:${name}${endfor}`
yields `1:ALBUM_ID,3:ARTIST_NAME,4:PUBLISH_DATE`.

#### Indentation — read this before laying out a loop

`indent` does **not** re-indent the loop body. It only affects line breaks that
are part of `inStr`.

- Line breaks *inside the loop body* are emitted exactly as they appear in the
  template. No extra indentation is added and none is removed.
- Each line break *inside `inStr`* is normalised to the template's own line
  ending — jdbgen decides between `\r\n` and `\n` by looking at the **first**
  line break in the template file, so a CRLF template keeps producing CRLF — and
  the fragment after it is indented to
  `<output column where the for statement started> + <indent>`. The column is
  measured in EUC-KR bytes of the current output line.
- `indent` defaults to `0`, and a non-numeric value raises a
  `NumberFormatException`.

This is why the idiomatic layout puts the whole body on the same line as the
`for` header and moves the line break into `inStr`:

```sql
SELECT  ${for:item=columns, inStr="\n,", indent=-1}${column} AS "${name.camel}"${endfor}
```

```sql
SELECT  ALBUM_ID AS "albumId"
       ,ALBUM_NAME AS "albumName"
       ,ARTIST_NAME AS "artistName"
       ,PUBLISH_DATE AS "publishDate"
```

The `for` starts at output column 8, `indent=-1` pulls the continuation lines
back to column 7, and the `,` of `inStr` lands under the second space of
`SELECT  `. The same pattern with `inStr="\nAND ", indent=-4` produces a
`WHERE` clause:

```sql
 WHERE ${for:item=keys, inStr="\nAND ", indent=-4}${column} = #{${name.camel}}${endfor}
```

```sql
 WHERE ALBUM_ID = #{albumId}
   AND TRACK_NO = #{trackNo}
```

> **Note**
> Writing the body on its own line, as in
> `SELECT ${for:item=columns, inStr=","}` ⏎ `       ${name}…${endfor}`, keeps the
> leading line break of the body in the output — the generated statement starts
> with an empty line after `SELECT`, and the closing `${endfor}` line contributes
> its own indentation. It is not an error, but it is rarely what you want.

Since line breaks inside a statement are insignificant, a long `for` header can
be broken up without affecting the output. The bundled MyBatis template uses
this to keep an `INSERT` readable:

```xml
               ${for:item=columns,inStr="\n,",indent=-1
               }${
               if:item=name,endsWith="date"
               }CURRENT_DATE()${
               else
               }#{${name.camel}}${
               endif}${
               endfor}
```

## Other statements

### `super`

```
${super:key=<table field>[<decorators>][, <extra decorators>]}
```

Inside a `for` loop, `super` resolves against the **table** instead of the
current column:

| Template | Output |
|:---|:---|
| `${for:item=keys}${super:key=name.suffix.pascal}.${name.camel}${endfor}` | `SampleAlbum.albumId` |
| `${for:item=keys}${super:key=remarks}${endfor}` | `Music Album` |

Nesting is one level deep: the outer object of a column loop is the table, and
there is nothing above the table. Used **outside** a loop, `super` is not an
error — it falls back to [custom variables](#custom-variables) and, failing
that, to an empty string.

> **Note**
> There is no `super` version of `if`. Inside a `for` loop you cannot branch on
> a table field directly; `${if:super=type, …}` is rejected as an unknown `if`
> condition. Move the `if` outside the loop, or duplicate the loop in both
> branches.

### `author`, `user` and `date`

| Statement | Value |
|:---|:---|
| `${author}` | The `Author Name` field of the Generator window (see the [User Interface Guide](ui-guide.md)) |
| `${user}` | The OS login user id |
| `${date}` | The current date |

All three accept [extra decorators](#extra-decorators):

| Template | Output |
|:---|:---|
| `${author:quote='"'}` | `"John Doe"` |
| `${user:prepend='@'}` | `@comart` |

`date` takes a [`SimpleDateFormat`](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html)
pattern in either of two ways:

| Template | Meaning |
|:---|:---|
| `${date}`, `${date:}` | Default format `yyyy-MM-dd` |
| `${date:yyyy/MM/dd HH:mm}` | Shorthand — the whole option string is the format |
| `${date:format=yyyy/MM/dd}` | Explicit form |
| `${date:format=yyyy, quote='!'}` | Explicit form with extra decorators → `!2026!` |

> **Note**
> The shorthand and extra decorators cannot be mixed, and the shorthand must not
> contain a `,` or an `=`. `${date:yyyy, quote='!'}` and
> `${date:EEE, d MMM yyyy}` both fail with `Name value pair not matched`. Use
> the explicit form and quote the pattern:
> `${date:format='EEE, d MMM yyyy'}`.

### Text escape

```
${'any text'}
${"any text"}
```

The text is emitted verbatim, `${` and `}` included. A `\` makes the next
character literal, which is how you embed the closing quote.

| Template | Output |
|:---|:---|
| `${'Test sample with ${author}'}` | `Test sample with ${author}` |
| `${'It\'s a test'}` | `It's a test` |
| `${'a\\b'}` | `a\b` |
| `${"say \"hi\""}` | `say "hi"` |
| `${'a\nb'}` | `anb` |

> **Note**
> Two traps. First, `\n` here is **not** a line break — the backslash simply
> escapes the `n` (option values behave the opposite way, see
> [Whitespace, quoting and escapes](#whitespace-quoting-and-escapes)). Second,
> anything between the closing quote and the `}` is silently discarded:
> `${'abc' ignored }` prints `abc`.

## Custom variables

Custom variables are the name/value pairs you enter in the Generator window.

![Generator Main Window](images/generator_main.png)

They are used exactly like fields, decorators included:

```
${package}                        → com.abc.sample
${package.replace('.','/')}       → com/abc/sample
```

Points to keep in mind:

- **Fields win.** The lookup order is object field first, custom variable
  second. A custom variable named `name` or `remarks` is shadowed by the table
  and column fields of the same name and can never be read.
- **`author`, `user` and `date` are statement names.** `${date}` is always the
  date statement, never your variable. The `item` form still reaches the
  variable: `${item:key=date}`.
- `${author}` *is* a custom variable — the Generator window stores the
  `Author Name` field under that name — which is why it also answers to
  `${item:key=author}`.
- Custom variables apply to the **output file name template** as well as to the
  template body. The bundled presets use `${name.suffix.pascal}Model.java`,
  `${name.suffix.camel}-mapper.xml` and `${name.suffix.lower}_ci_model.php`,
  which for the sample table produce `SampleAlbumModel.java`,
  `sampleAlbum-mapper.xml` and `sample_album_ci_model.php`. A file name template
  such as `${package.replace('.','/')}/${name.suffix.pascal}.java` writes into a
  package directory tree.
- An undefined name is not an error: it logs a warning and expands to an empty
  string.

## Abbreviations

The Abbreviation Mapping window holds a list of rules, each of which replaces a
word — or a whole name — with a replacement string.

![Abbreviation Mapping Window](images/abbreviation.png)

The `.abbr` decorator applies them in two steps:

1. **Total-name rules** (rules with `Total Name` checked) are looked up with the
   value converted to lower case. On a hit, the entire value is replaced and
   processing stops.
2. Otherwise the value is split on `_` and `-`, and each word is looked up in
   the word rules — **without lower-casing the word**.

Rule keys are always stored in lower case. That mismatch in step 2 has a
consequence worth remembering:

> **Note**
> Word rules never match an upper-case name. With a rule `album` → `DISC`,
> `${name.abbr}` on `T_SAMPLE_ALBUM` returns `T_SAMPLE_ALBUM` unchanged, while
> `${name.lower.abbr}` returns `t_sample_DISC`. Lower-case the value first, or
> use a total-name rule (those *are* matched case-insensitively).

The Generator window has a checkbox, `Apply abbreviation rule to all name
fields.` When it is on, `.abbr` is inserted automatically as the **first**
decorator of every `item` reference whose first key is `name` — you do not have
to write `.abbr` at all, and `${name.lower}` silently becomes
`${name.abbr.lower}`. Aliases are not covered: `${table}` and `${column}` are
never abbreviated.

## Error handling

| Situation | Behaviour |
|:---|:---|
| Unknown field or variable | Warning in the log, expands to an empty string — not an error |
| Unknown decorator | `RuntimeException` listing the valid decorators |
| `replace` with fewer than 2 arguments | `RuntimeException` |
| Unknown condition name in `if` | `ParseException` at parse time |
| Missing `key`/`item` | `ParseException` |
| `for` over a member that does not exist | `RuntimeException: Model has no 'x' member` |
| `for` over a member that is not a list | `ClassCastException` |
| `contains`/`notContains` on a value that is neither a list nor a string (`length`, `isKey`, …) | `RuntimeException` |
| Unterminated `}` | `ParseException: '}' not found, before: …` |
| Missing `endif` / `endfor` | `ParseException: if statements not closed` / `for statements not closed` |
| Unknown statement type | `ParseException: Unknown template: …` |
| Non-numeric `padSize` or `indent` | `NumberFormatException` |

Everything except the first row aborts the template being processed. The
Progress window shows `process failed! : <message>` and the full stack trace
goes to the log; files already written for earlier tables or templates stay on
disk.

![Progress Window](images/progress.png)

Because a missing field is silent, an unexpectedly empty spot in the output is
almost always a misspelled field name — check the log for
`cannot find 'x' information from database/custom variables`. See
[Troubleshooting](troubleshooting.md) for more.

## Recipes

The three templates shipped in the `templates/` directory (see
[Installation](installation.md) for where jdbgen puts them) are working
examples; the patterns below are taken from them. See the
[User Interface Guide](ui-guide.md) for how to register a template, and
[Custom Queries](custom-queries.md) if your driver does not report metadata
correctly.

### A column list, one per line

```sql
        SELECT  ${for:item=columns, inStr="\n,", indent=-1}${column} AS "${name.camel}"${endfor}
          FROM ${table} A
```

```sql
        SELECT  ALBUM_ID AS "albumId"
               ,ALBUM_NAME AS "albumName"
               ,ARTIST_NAME AS "artistName"
               ,PUBLISH_DATE AS "publishDate"
          FROM T_SAMPLE_ALBUM A
```

### A primary key predicate

```sql
         WHERE ${for:item=keys, inStr="\nAND ", indent=-4}${column} = #{${name.camel}}${endfor}
```

For a two-column primary key:

```sql
         WHERE ALBUM_ID = #{albumId}
           AND TRACK_NO = #{trackNo}
```

### An UPDATE that leaves the key columns alone

```sql
        UPDATE ${table}
           SET  ${for:item=notKeys, inStr="\n,", indent=-1}${column} = #{${name.camel}}${endfor}
         WHERE ${for:item=keys, inStr="\nAND ", indent=-4}${column} = #{${name.camel}}${endfor}
```

```sql
        UPDATE T_SAMPLE_ALBUM
           SET  ALBUM_NAME = #{albumName}
               ,ARTIST_NAME = #{artistName}
               ,PUBLISH_DATE = #{publishDate}
         WHERE ALBUM_ID = #{albumId}
```

### An INSERT whose date columns default to the server clock

The `for` header and the `if` inside it are broken across lines to keep the XML
readable; those line breaks are inside statements and never reach the output.

```xml
        VALUES
             (
               ${for:item=columns,inStr="\n,",indent=-1
               }${
               if:item=name,endsWith="date"
               }CURRENT_DATE()${
               else
               }#{${name.camel}}${
               endif}${
               endfor}
             )
```

```xml
        VALUES
             (
               #{albumId}
              ,#{albumName}
              ,#{artistName}
              ,CURRENT_DATE()
             )
```

### A Java model with aligned types and validation annotations

From `templates/java_model.java`. `padSize=10` aligns the type column,
`${if:key=nullable,value=0}` marks the mandatory fields, and
`${if:key=typeName,startsWith="char"}` adds a length constraint to character
columns only.

```java
public class ${name.suffix.pascal}Model
{
    ${for:item=keys}// ${remarks}
    @NotBlank(message="${remarks}: Required Item.")
    private ${item:key=javaType,padSize=10,padDir=right} ${name.camel};
    ${endfor}

    ${for:item=notKeys}// ${remarks}
    ${if:key=nullable,value=0}@NotBlank(message="${remarks}: Required Item.")
    ${endif}${if:key=typeName,startsWith="char"}@Size(max=${length}, message="${remarks}: Cannot exceeds ${length}.")
    ${endif}private ${item:key=javaType,padSize=10,padDir=right} ${name.camel};
    ${endfor}
}
```

```java
public class SampleAlbumModel
{
    // Album identifier
    @NotBlank(message="Album identifier: Required Item.")
    private Integer    albumId;
    

    // Album display name
    @NotBlank(message="Album display name: Required Item.")
    private String     albumName;
    // Creator artist name
    private String     artistName;
    // Published date
    private Date       publishDate;
    
}
```

### A qualified class name from a custom variable

Define a custom variable `package` = `com.abc.sample` in the Generator window,
then use it in both the template and the output file name:

| Template | Output |
|:---|:---|
| `package ${package}.${name.suffix.camel};` | `package com.abc.sample.sampleAlbum;` |
| Output file name `${package.replace('.','/')}/${name.suffix.pascal}.java` | `com/abc/sample/SampleAlbum.java` |

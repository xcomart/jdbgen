# Custom Queries

Some JDBC drivers do not implement `DatabaseMetaData` completely, so jdbgen cannot
discover tables, columns or comments through the standard JDBC calls. For those
drivers you can supply your own database specific SQL and jdbgen will use it
instead. Custom queries are configured per driver, in the **Custom Queries** tab of
the Driver Manager (see the [UI guide](ui-guide.md)); each of the four queries is a
checkbox plus a SQL text area, and each can be enabled independently.

[← Documentation index](README.md)

---

## How substitution works

Before a custom query is executed, jdbgen replaces every `${...}` placeholder in it
with a value taken from a metadata object:

| Query | Object the placeholders are read from |
|:---|:---|
| Get table list | the schema being expanded |
| Get table comments | the schema being expanded |
| Get table column list | the table being expanded |
| Get table column comments | the table being expanded |

The lookup is done by reflection: the text between `${` and `}` is treated as a
property name and resolved against the object's public getters. This means the set
of usable placeholders is simply *every getter of that object*, not only the two or
three that are interesting.

### Placeholders for schema level queries

Available in **Get table list** and **Get table comments**:

| Placeholder | Description |
|:---:|:---|
| `${catalog}` | Catalog the schema belongs to. See the note about missing catalogs below. |
| `${schema}` | Schema name as reported by the driver. |
| `${name}` | Alias of `${schema}` — both are filled from the same value. |
| `${tables}` | The already loaded table list. It is still empty while the table list query runs, so this is of no practical use there. |

### Placeholders for table level queries

Available in **Get table column list** and **Get table column comments**:

| Placeholder | Description |
|:---:|:---|
| `${catalog}` | Catalog the table belongs to (`TABLE_CAT`). |
| `${schema}` | Schema the table belongs to (`TABLE_SCHEM`). |
| `${table}` | Table name (`TABLE_NAME`). |
| `${name}` | Alias of `${table}` — both are filled from the same value. |
| `${type}` | Normalized table type, either `TABLE` or `VIEW`. |
| `${remarks}` | Table comment, if one is already known at that point. |

Nested access through `.` is also supported (`${schema.name}` style), because each
segment is resolved as a getter on the result of the previous one.

### Substitution pitfalls

Substitution here is deliberately simple, and that simplicity has consequences:

- **A `null` value is not substituted.** The placeholder is left in the SQL exactly
  as written, so the statement is sent to the database containing the literal text
  `${schema}`. The resulting error usually looks like a syntax error rather than a
  missing value, so check the generated SQL first.
- **There is no quoting and no escaping.** The value is pasted in verbatim. An
  identifier containing a single quote will break the statement, and jdbgen does
  nothing to prevent it.
- **A missing closing brace is a parse error.** Every `${` must have a matching `}`,
  otherwise the query fails with `End delimiter not presented.`
- **Template decorators do not work here.** `${name.camel}`, `${name.upper}` and the
  rest of the [template decorators](template-reference.md) belong to the code
  generator, not to custom queries. In a custom query, `.camel` is looked up as a
  getter named `camel` and resolves to nothing.

### When the database has no catalog or no schema

Not every database reports both levels, and the fallbacks are visible in your SQL:

- If the driver reports no catalog at all, jdbgen substitutes the **literal string
  `Default Catalog`** for `${catalog}`. Comparing a real catalog column against that
  string will simply match nothing.
- If the driver reports no schema, `${schema}` is `null` and therefore **left in the
  SQL as `${schema}`**, which is almost certainly a syntax error.

For such databases, do not reference `${catalog}` or `${schema}` in the query at
all — hard-code the value or drop the predicate.

---

## Mixing custom and standard metadata

The four queries are independent switches. Whatever you leave unchecked keeps using
the standard JDBC `DatabaseMetaData` path, so you only need to override the parts
your driver gets wrong. A driver that lists tables and columns correctly but exposes
no comments only needs the two comment queries — that is exactly how the bundled
Microsoft SQL Server driver entry is configured.

One combination changes behaviour rather than just the data source, and it is worth
knowing about:

- **Without** a custom column list query, primary keys are detected with
  `DatabaseMetaData.getPrimaryKeys()`.
- **With** a custom column list query, `getPrimaryKeys()` is never called. The only
  thing that marks a column as a key is the `IS_KEY` field of your result set.

---

### Get Table Comments SQL

A query returning the comment of every table in a schema.

The result set is read **positionally**: the first column is the table name, the
second is its comment. Column labels are irrelevant here, so you can name them
anything.

Table names are matched against the table list that was loaded just before, and a
comment is applied **only when the name matches a known table**. Tables missing from
the result set keep whatever comment they already had, so a query returning only
some of the tables is safe.

Supplied parameters: the [schema level placeholders](#placeholders-for-schema-level-queries).

Result set fields:

| Field | Type | Description |
|:---:|:---:|:---|
| First column | String | Table name. Must match the name in the table list exactly, including letter case. |
| Second column | String | Table comment. |

Example for Microsoft SQL Server (this is the query shipped with the built-in
`Microsoft SQL Server` driver entry):

```sql
SELECT OBJNAME, cast(value as varchar(8000)) as VALUE
FROM fn_listextendedproperty ('MS_DESCRIPTION','schema','${schema}','table',null,null,null)
```

---

### Get Column Comments SQL

A query returning the comment of every column of one table. It is executed once per
table, after the column list has been loaded.

As with table comments, the result set is read **positionally**: first column is the
column name, second is the comment.

> **This query is destructive.** Unlike table comments, the comments are collected
> into a map and then applied to *every* column: a column that does not appear in
> your result set has its comment overwritten with `null`. A partial column comments
> query therefore erases the comments that the JDBC driver had already reported.
> Either return a row for every column, or do not enable this query at all.

Supplied parameters: the [table level placeholders](#placeholders-for-table-level-queries).

Result set fields:

| Field | Type | Description |
|:---:|:---:|:---|
| First column | String | Column name. Must match the name in the column list exactly, including letter case. |
| Second column | String | Column comment. |

Example for Microsoft SQL Server (this is the query shipped with the built-in
`Microsoft SQL Server` driver entry):

```sql
SELECT OBJNAME, cast(value as varchar(8000)) as VALUE
FROM fn_listextendedproperty ('MS_DESCRIPTION','schema','${schema}','table','${table}','column',null)
```

---

### Get Table List SQL

A query returning all tables and views of a schema.

Unlike the two comment queries, this result set is read **by column label**, and the
labels below are mandatory. If a label is missing the query fails with an
`SQLException` and no tables are listed at all — so alias your columns when their
natural names differ.

Supplied parameters: the [schema level placeholders](#placeholders-for-schema-level-queries).

Result set fields:

| Field | Type | Description |
|:---:|:---:|:---|
| `TABLE_CAT` | String | Catalog the table belongs to. May be null. |
| `TABLE_SCHEM` | String | Schema the table belongs to. May be null. |
| `TABLE_NAME` | String | Table name. |
| `TABLE_TYPE` | String | Table type. May be null — see below. |
| `REMARKS` | String | Table comment. May be null. |

#### How `TABLE_TYPE` is interpreted

`TABLE_TYPE` is **not required to have a value** (the column must exist, but null is
accepted) and it is normalized before use:

1. A null value is treated as `TABLE`.
2. `TABLE` and `VIEW` are used as they are.
3. Any other value is scanned: if it *contains* `TABLE` it becomes `TABLE`, if it
   contains `VIEW` it becomes `VIEW`. This is what makes `BASE TABLE` and
   `SYSTEM VIEW` work without any extra effort.
4. **A value that matches none of these is dropped.** The table is loaded but then
   filtered out of the list, so it never appears in the Generator window.

Point 4 is the usual reason for a mysteriously short table list. Types such as
`SYNONYM`, `ALIAS` or `SEQUENCE` will silently disappear; map them to `TABLE`
yourself if you want to generate code from them.

Views are additionally hidden unless **Show Views** is checked in the Generator
window.

Example for H2 (this is the query shipped with the built-in `H2 Embedded` and
`H2 Server` driver entries):

```sql
select TABLE_CATALOG as "TABLE_CAT",
       TABLE_SCHEMA as "TABLE_SCHEM",
       TABLE_NAME,
       CASE WHEN TABLE_TYPE='BASE TABLE' THEN 'TABLE' ELSE TABLE_TYPE END AS "TABLE_TYPE",
       REMARKS
  from information_schema.tables
 where TABLE_CATALOG='${catalog}'
   and TABLE_SCHEMA='${schema}'
```

---

### Get Column List SQL

A query returning all columns of one table. It is executed once per table.

This result set is read **by column label** as well, and every label below is
mandatory — including `IS_KEY`, which has no JDBC equivalent. Omitting any one of
them raises an `SQLException` and the table's columns cannot be read at all.

Supplied parameters: the [table level placeholders](#placeholders-for-table-level-queries).

Result set fields:

| Field | Type | Description | Template field |
|:---:|:---:|:---|:---:|
| `TABLE_CAT` | String | Catalog the table belongs to. | `catalog` |
| `TABLE_SCHEM` | String | Schema the table belongs to. | `schema` |
| `TABLE_NAME` | String | Table name. | `table` |
| `COLUMN_NAME` | String | Column name. | `column`, `name` |
| `DATA_TYPE` | Integer | Value of the matching [java.sql.Types](https://docs.oracle.com/javase/8/docs/api/java/sql/Types.html) constant. | `dataType` |
| `TYPE_NAME` | String | Database specific type name. Null is tolerated and becomes an empty string. | `typeName` |
| `COLUMN_SIZE` | Integer | Column length. | `length` |
| `NULLABLE` | Integer | `1` when the column is nullable, `0` when it is not. | `nullable` |
| `REMARKS` | String | Column comment. | `remarks` |
| `COLUMN_DEF` | String | Column default value. | `defVal` |
| `IS_KEY` | Integer | Non-zero when the column is part of the primary key — see below. | `isKey` |

Columns are numbered in the order the query returns them, so add an `ORDER BY` if
your database does not already return them in declaration order.

#### `IS_KEY` must be numeric

`IS_KEY` is read as a **string** and then parsed as an integer; any value other than
zero marks the column as part of the primary key. The parser is strict: if the text
contains any character other than a digit, a sign, `,` or `.`, it **returns `0`**
rather than raising an error.

The practical consequence is that boolean-looking values silently mean "not a key":

| Returned value | Interpreted as |
|:---:|:---|
| `1` | primary key |
| `0` | not a primary key |
| `'Y'` | **not a primary key** — non-numeric, parsed as 0 |
| `'true'` | **not a primary key** — non-numeric, parsed as 0 |
| `null` | not a primary key |

Always return `0` or `1`. If the table has no primary key at all, return `0` for
every row; do not drop the column.

#### `DATA_TYPE` decides the generated types

`DATA_TYPE` is not merely informative. It is looked up in an internal table to
derive the `jdbcType` and `javaType` fields that templates use, so an incorrect
value produces incorrect code:

- A value with no mapping — for instance `TIMESTAMP_WITH_TIMEZONE` (2014) — leaves
  both fields empty, and `${javaType}` renders as an empty string.
- `0` is **not** a neutral value: it is `java.sql.Types.NULL`, which maps to the
  Java type `null`. Using it as a catch-all produces model fields declared as
  `null`.

If you cannot classify a type, `1111` (`java.sql.Types.OTHER`) is a better fallback
than `0`. The full list of mappings is in the
[template reference](template-reference.md).

Example for H2:

```sql
select TABLE_CATALOG as "TABLE_CAT",
       TABLE_SCHEMA as "TABLE_SCHEM",
       TABLE_NAME,
       COLUMN_NAME,
       CASE WHEN DATA_TYPE LIKE 'CHAR%' THEN 12
            WHEN DATA_TYPE='INTEGER' THEN 4
            WHEN DATA_TYPE='DATE' THEN 91
            WHEN DATA_TYPE='BIGINT' THEN -5
            WHEN DATA_TYPE='BOOLEAN' THEN 16
            ELSE 1111 END AS "DATA_TYPE",
       DATA_TYPE as "TYPE_NAME",
       CHARACTER_MAXIMUM_LENGTH as "COLUMN_SIZE",
       CASE WHEN IS_NULLABLE='YES' THEN 1 ELSE 0 END as "NULLABLE",
       REMARKS,
       COLUMN_DEFAULT as "COLUMN_DEF",
       CASE WHEN exists(select 1
                          from information_schema.index_columns B
                         where TABLE_CATALOG='${catalog}'
                           and TABLE_SCHEMA='${schema}'
                           and TABLE_NAME='${table}'
                           and COLUMN_NAME=A.COLUMN_NAME
                           and INDEX_NAME=(select INDEX_NAME from information_schema.indexes
                                            where TABLE_CATALOG='${catalog}'
                                              and TABLE_SCHEMA='${schema}'
                                              and TABLE_NAME='${table}'
                                              and INDEX_TYPE_NAME='PRIMARY KEY'))
           THEN 1 ELSE 0 END AS "IS_KEY"
  from information_schema.columns A
 where TABLE_CATALOG='${catalog}'
   and TABLE_SCHEMA='${schema}'
   and TABLE_NAME='${table}'
```

This example is illustrative — it is not part of the shipped configuration, and its
`CASE` covers only a handful of H2 types. Extend the `WHEN` branches to every type
you actually use before relying on it; whatever falls through to the `ELSE` branch
generates unusable Java types.

---

## Result set contracts at a glance

| Query | Read by | Required fields | Effect of a missing row |
|:---|:---:|:---|:---|
| Get table comments | position (1, 2) | table name, comment | Table keeps its previous comment |
| Get table column comments | position (1, 2) | column name, comment | **Column comment is set to null** |
| Get table list | label | `TABLE_CAT`, `TABLE_SCHEM`, `TABLE_NAME`, `TABLE_TYPE`, `REMARKS` | Table is not listed |
| Get table column list | label | `TABLE_CAT`, `TABLE_SCHEM`, `TABLE_NAME`, `COLUMN_NAME`, `DATA_TYPE`, `TYPE_NAME`, `COLUMN_SIZE`, `NULLABLE`, `REMARKS`, `COLUMN_DEF`, `IS_KEY` | Column is not listed |

For the label-based queries, a missing *label* is fatal — the whole query throws an
`SQLException`. A missing *value* (null) is tolerated for every field except the
names themselves.

Built-in drivers shipping with a custom query out of the box:

| Driver entry | Custom queries enabled |
|:---|:---|
| H2 Embedded | Get table list |
| H2 Server | Get table list |
| Microsoft SQL Server | Get table comments, Get table column comments |

Every other bundled driver relies entirely on standard JDBC metadata.

---

## Writing queries for a new database

A checklist that avoids most of the traps above:

1. **Start with the smallest override.** Enable only the query whose data is
   actually wrong; leave the rest on the standard JDBC path.
2. **Run the SQL in a database client first**, with the placeholder values filled in
   by hand. Confirm it works before pasting it into jdbgen.
3. **Alias every column** so the labels match the contract exactly. `TABLE_SCHEM`
   has no `A`; `COLUMN_DEF` is not `COLUMN_DEFAULT`.
4. **Check whether your database has catalogs.** If it does not, drop `${catalog}`
   from the query instead of comparing against it — it will be the literal
   `Default Catalog`.
5. **Return real `java.sql.Types` numbers** for `DATA_TYPE`, and cover every type
   your schema uses. Avoid `0`.
6. **Return `0`/`1` for `IS_KEY`**, never `'Y'`/`'N'` or `'true'`/`'false'`.
7. **Return one row per column** from the column comments query, or leave that query
   disabled — anything else deletes comments.
8. **Normalize `TABLE_TYPE` yourself** for anything that is not literally `TABLE` or
   `VIEW` and that you still want to see.
9. **Match letter case.** The comment queries join on names, and the join is
   case sensitive.
10. **Watch the log** while connecting; failed queries and unmatched names are
    reported there. See [troubleshooting](troubleshooting.md).

---

## Related documentation

- [UI guide](ui-guide.md) — where the Custom Queries tab lives and how driver
  entries are managed.
- [Template reference](template-reference.md) — the fields your queries populate and
  the `DATA_TYPE` mapping table.
- [Troubleshooting](troubleshooting.md) — symptoms caused by incorrect custom
  queries.
- [Installation](installation.md) · [Building from source](building.md) ·
  [Icons](icons.md)

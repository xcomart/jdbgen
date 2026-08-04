# jdbgen Documentation

jdbgen generates text and source files from database table metadata, using a
built-in template engine. You connect to a database, select tables and
templates, and jdbgen writes one output file per table per template.

This is the documentation index. For a short overview of the tool, see the
[project README](../README.md).

## Getting started

| Document | Read this when you want to |
|:---|:---|
| [Installation](installation.md) | Install and run jdbgen, understand what it puts where, or see which JDBC drivers are built in |
| [User Interface Guide](ui-guide.md) | Find out what a window, field or button does |

New to jdbgen? Install it, start it, and generate from the bundled
`Sample H2 Embedded` connection — it needs no database of your own. The
[Installation](installation.md) guide walks through it.

## Writing templates

| Document | Read this when you want to |
|:---|:---|
| [Template Reference](template-reference.md) | Write or debug a template — statements, decorators, table and column fields, type mappings, error behaviour |
| [Icons](icons.md) | Set the icon shown for a connection or a driver |

## Connecting to databases

| Document | Read this when you want to |
|:---|:---|
| [Custom Queries](custom-queries.md) | Make jdbgen work with a driver that does not report table or column metadata properly |
| [Troubleshooting](troubleshooting.md) | Fix a problem, or check whether it is a known issue |

## Contributing

| Document | Read this when you want to |
|:---|:---|
| [Building from Source](building.md) | Build, test or release jdbgen yourself |

Issues and pull requests are welcome at
[github.com/xcomart/jdbgen](https://github.com/xcomart/jdbgen).

## Quick reference

Template statements, in brief — the full rules are in the
[Template Reference](template-reference.md).

| Statement | Purpose |
|:---|:---|
| `${<field>}` / `${item:key=<field>}` | Insert a table or column field, or a custom variable |
| `${if:item=<field>, <condition>}` … `${elif:…}` … `${else}` … `${endif}` | Branch on a field value |
| `${for:item=<collection>}` … `${endfor}` | Repeat over `columns`, `keys` or `notKeys` |
| `${super:key=<field>}` | Reach the table object from inside a `for` loop |
| `${author}`, `${user}`, `${date:<format>}` | Author name, OS login id, current date |
| `${'literal text'}` | Emit text containing `${` or `}` literally |

Name decorators chain left to right: `${name.suffix.pascal}` turns
`T_SAMPLE_ALBUM` into `SampleAlbum`.

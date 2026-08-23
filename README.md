# jdbgen

[![Build](https://github.com/xcomart/jdbgen/actions/workflows/build.yml/badge.svg)](https://github.com/xcomart/jdbgen/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/xcomart/jdbgen)](https://github.com/xcomart/jdbgen/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/xcomart/jdbgen/total)](https://github.com/xcomart/jdbgen/releases)
[![Java](https://img.shields.io/badge/java-11%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/github/license/xcomart/jdbgen)](LICENSE)

jdbgen generates text and source files from database table metadata, using a
built-in template engine.

Point it at a database, pick some tables, pick some templates, and it writes one
file per table per template — model classes, DML statements, mapper XML,
documentation, or anything else you can express as text.

![Generator main window](docs/images/generator_main.png "Generator Main Window")

## Why

Data objects carry information between layers in MVC-style architectures. They
are intuitive and they keep typos out of your code, but writing them by hand is
repetitive work that follows directly from the table definition you already
have. jdbgen turns that definition into the files you would have typed.

## How it works

Given a table:

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

and a template:

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

jdbgen writes:

```java
/**
 * Music Album Model class
 *
 * @author John Doe <john.doe@abc.com>
 * @version 1.0 2024-08-12
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

The template language has loops, conditionals, name-case decorators, padding,
abbreviation rules and custom variables. See the
[Template Reference](docs/template-reference.md).

## Installation

**On Windows**, download `jdbgen-<version>.msi` from the
[latest release](https://github.com/xcomart/jdbgen/releases/latest) and run it —
or, once the package is available in winget, `winget install Xcomart.Jdbgen`.
The installer carries its own Java runtime, so nothing else is needed.

**On every platform**, the ZIP archive works too:

> **Requirements** — a Java runtime, version 11 or above. Make sure its `bin`
> directory is on `PATH`, or that `JAVA_HOME` points at the installation. See
> [Installation](docs/installation.md) for the details.

1. Download the archive from the [latest release](https://github.com/xcomart/jdbgen/releases/latest).
2. Unzip it wherever you like. It expands into a `jdbgen-<version>/` directory.
3. Run `jdbgen.sh` (Linux/macOS) or `jdbgen.cmd` (Windows). You can also run
   `java -jar jdbgen-<version>.jar` directly, from any directory.

Your configuration and the JDBC drivers you download are kept in a per-user
directory (`%APPDATA%\jdbgen`, `~/Library/Application Support/jdbgen`, or
`~/.config/jdbgen`), not in the installation. An unpacked archive keeps itself up
to date: when a newer release is published it offers to install it on the next
start, downloads it and restarts itself. An installation that may not write to
its own directory instead tells you how to update it — with `winget upgrade` on
Windows. See [Updating](docs/installation.md#updating).

The user interface follows your operating system language and can be switched
by hand in the main window: English, 한국어, Español, 日本語 and 简体中文 are
included.

Both downloads ship with a small H2 sample database, its H2 driver and three example templates,
and the default configuration includes a `Sample H2 Embedded` connection that
uses them, so you can generate something on the first run without setting up a
database.

The other JDBC drivers are not bundled. jdbgen downloads them from Maven Central
for you — see the [User Interface Guide](docs/ui-guide.md).

## Documentation

| Document | Contents |
|:---|:---|
| [Installation](docs/installation.md) | Requirements, directory layout, where jdbgen keeps its data, bundled drivers |
| [User Interface Guide](docs/ui-guide.md) | Every window, field and button, and what they do |
| [Template Reference](docs/template-reference.md) | The complete template language |
| [Custom Queries](docs/custom-queries.md) | Metadata SQL for drivers that need it |
| [Icons](docs/icons.md) | How to specify icons for connections and drivers |
| [Troubleshooting](docs/troubleshooting.md) | Known issues, pitfalls and fixes |
| [Building from Source](docs/building.md) | Gradle build, project layout, tests |

Start at the [documentation index](docs/README.md).

## License

Distributed under the terms in [LICENSE](LICENSE).

# jdbgen

jdbgen is a tool for generating text(source) files from database table
informations, using in-house template engine.

## Introduction

According to recent programming trends, data object is used for information
exchange in MVC like architecture.
These approach reduces typo error and intuitive, but requires many repeatitive
works.
This tool makes easy to create model like objects, DML SQLs and many other
applicable text files.

![Generator main window](docs/images/generator_main.png "Generator Main Window")

Here is a sample which shows how does it works.

If we have a database table created with below SQL script:
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

and we have a template like:
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

then will generate below code:
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

Awesome!

## Installation

> Note! JRE 11 or above is required to run this application.
> Make sure JRE `bin` directory in `PATH` or `JAVA_HOME` has been set.

1. Download application from [latest release](https://github.com/xcomart/jdbgen/releases/latest).
1. Unzip downloaded archive to desired location.
1. Run jdbgen.sh(for Linux/Unix/Mac) or jdbgen.cmd(for Windows) or run jdbgen-xx.jar directly as java application.


If you need more information, read [full document](docs/README.md)

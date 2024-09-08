# jdbgen

## Introduction

jdbgen is a tool for generating text(source) files from database table
informations, using in-house template engine.

If you want to create model class in java or table headers in html
from database table, this tool will fit perfectly as you need.

## Installation

> Note! JRE 11 or above is required to run this application.
> Make sure JRE `bin` directory in `PATH` or `JAVA_HOME` has been set.

1. Download application from [latest release](https://github.com/xcomart/jdbgen/releases/latest).
1. Unzip downloaded archive to desired location.
1. Run jdbgen.sh(for Linux/Unix/Mac) or jdbgen.cmd(for Windows) or run jdbgen-xx.jar directly as java application.

## UI Workflow

### Master Password

Master password is used to encrypt/decrypt your database connection informations.

At the first time you run application, master password setting window appeared.

![Master password setting window](images/master_password_set.png "Master Password Setting Window")

Next time you run the program, master password asking window appeared.

![Master password asking window](images/master_password.png "Master Password Asking Window")

> Note! If you forgot master password, all customized informations will be lost.

### Connection Manager Window

![Connection manager window](images/connection_manager.png "Connection Manager Window")

Managing database connection informations, connection dependent templates and
options.

#### General Tab

|Name|Descriptions|
|---:|:---|
|Connection Name|Unique connection name which will be shown in left list|
|Driver|JDBC driver(can be managed by [Driver Manager Window](#driver-manager-window))|
|Connection URL|JDBC driver specific connection URL to database|
|User Name|Database user name|
|User Password|Database user password|
|Icon|Icon used in connection list(see [Icons Usage](#icons-usage))|
|Connection Props|Additional connection properties|
|Keep Alive|Execute dummy query every N seconds to keep alive connection|

Managing basic connection informations.

#### Templates Tab

![Connection manager templates](images/connection_templates.png "Connection Manager Templates Tab")

|Name|Descriptions|
|---:|:---|
|Template Name|A name for identifing template file|
|Template File|Template file location([Template Instructions](#template-instructions))|
|Output Name Template|Output file name template([Template Instructions](#template-instructions))|

You can create/manage template presets in [Template Preset Window](#template-preset-window).

#### Options Tab

![Connection manager options](images/connection_options.png "Connection Manager Options Tab")

|Name|Descriptions|
|---:|:---|
|Output Directory|Where the generated output files are written.|
|Author Name|Author text(`${author}` in template) which used in template.<br/>(ex. John Doe &lt;john.doe@abc.com&gt;)|
|Custom Variables|User defined `item type` variables which used in templates.|

### Driver Manager Window

![Driver manager window](images/driver_manager.png "Driver Manager Window")

Managing JDBC driver information, driver dependent properties.

Built-in drivers are cannot be modified in this window, but you can
create/clone driver informations.

#### General Tab

|Name|Descriptions|
|---:|:---|
|Driver Name|Unique driver name which will be shown in left list|
|JDBC Jar|JDBC driver(can be downloaded by [Maven Repository Window](#maven-repository-window)|
|URL Template|Driver specific JDBC connection URL template,<br/>which shown when creating new connection|
|Driver Class|`java.sql.Driver` implementation class<br/>(when click this field, implementation class list will be shown)|
|User Password|Database user password|
|Icon|Icon used in driver list(see [Icons Usage](#icons-usage))|
|Connection Props|Additional connection properties for this driver|

#### Custom Queries Tab

![Driver custom queries](images/driver_custom.png "Driver Custom Queries Tab")

|Name|Descriptions|
|---:|:---|
|Get table comments|Getting table comments from schema in SQL(`${catalog}` and `${schema}` available)|
|Get table column comments|Getting table column comments from table in SQL(`${catalog}`, `${schema}` and `${table}` available)|
|Get table list|Getting tables from schema in SQL(`${catalog}` and `${schema}` available)|
|Get table column list|Getting table column list from table in SQL(`${catalog}`, `${schema}` and `${table}` available)|

If table list does not shows, you need to write these database dependent queries,
refer [Custom Queries](#custom-queries).

### Template Presets Window

![Template preset window](images/template_preset.png "Driver Manager Window")

Managing set of templates for specific usage.

|Name|Descriptions|
|---:|:---|
|Template Presets|List of defined presets|
|Preset Name|Unique name of preset which will be shown in left list|
|New Preset from Current Connection|When clicked, all templates of current connection<br/>will be copied int current preset with new preset name|
|Apply to Current Connection|When clicked, templates of current connection<br/>will be replaced with current preset templates|
|Template Name|A name for identifing template file|
|Template File|Template file location([Template Instructions](#template-instructions))|
|Output Name Template|Output file name template([Template Instructions](#template-instructions))|

### Maven Repository Window

![Maven repository window](images/maven_repository.png "Maven Repository Window")

JDBC driver search and download.

### Generator Main Window

![Generator main window](images/generator_main.png "Generator Main Window")

Generating text files using table information and templates.

|Name|Descriptions|
|---:|:---|
|Connection|Current selected database connection|
|Catalogs/Schemas|Database catalog and schema tree, select leaf node to be generated.|
|Show Views|Toggle to show/hidden views in table list|
|Tables|Table/View list of current schema|
|Templates|Select template to be generated, click `Select` table header to select/deselect all template|
|Output Directory|Output location of generated text/source files|
|Author Name|Author text(`${author}` in template) which used in template.<br/>(ex. John Doe &lt;john.doe@abc.com&gt;)|
|Custom Variables|User defined `item type` variables which used in templates.|
|Dark UI|Change UI theme to dark/light.|
|Generate|Start generate process, [Progress Window](#progress-window) will be shown.|
|Close|Close program|

### Abbreviation Mapping Window

![Abbreviation Mapping Window](images/abbreviation.png "Abbreviation Mapping Window")

Manage abbreviation mapping rules.
This abbreviation mapping will be applied in [`.abbr` decorator of `item` statement](#item-statement).


### Progress Window

![Progress window](images/progress.png "Progress Window")

Shows progress/logs of generating process.


## Icons Usage

Icon can be specified with four types:
1. Local image/icon file.
1. [FontAwesome v4.7 icons](https://fontawesome.com/v4/icons/).
1. Color bullet.
1. Stock icons.

### Local image/icon file

This type can be specified by local filesystem absolute/relative path,
"jpg", "jpeg", "tiff", "tif", "gif", "png" and "ico" files can be used.

example: `/home/user/sample.png`

### FontAwesome v4.7 icons

FontAwesome icon is used for many web/application products recent years.
v4.7 version is used in this application.
Actually [`jiconfont-font_awesome`](https://jiconfont.github.io/fontawesome)
is used in this application.

You can specify FontAwesome icon using 'fa:' prefix in `Icon` field.
Icon color will be same as default font color.

Examples:

|Icon|Field String|
|:---:|:---|
|<img src="images/table.svg" width="17" width="17"/>|`fa:table`|
|<img src="images/eye.svg" width="17" width="17"/>|`fa:eye`|
|<img src="images/window-restore.svg" width="17" width="17"/>|`fa:window_restore`|

### Color bullet

Color bullet is simple color circle icon,
"white", "light_gray", "gray", "dark_gray", "black", "red", "pink", "orange",
"yellow", "green", "magenta", "cyan" and "blue" can be used.

You can specify color bullet icon using 'color:' prefix in `Icon` field.

Examples:

|Icon|Field String|
|:---:|:---|
|$${\color{blue}&#x2B24;}$$|`color:blue`|
|$${\color{green}&#x2B24;}$$|`color:green`|
|$${\color{red}&#x2B24;}$$|`color:red`|

### Stock icons

This icons used for application internal needs, database brand icon mostly.

All available stock icons are:

|Icon|Field String|
|:---:|:---|
|<img src="../src/main/resources/icons/altibase.png" width="17" width="17"/>|`stock:altibase.png`|
|<img src="../src/main/resources/icons/cubrid.png" width="17" width="17"/>|`stock:cubrid.png`|
|<img src="../src/main/resources/icons/generic.png" width="17" width="17"/>|`stock:generic.png`|
|<img src="../src/main/resources/icons/h2.png" width="17" width="17"/>|`stock:h2.png`|
|<img src="../src/main/resources/icons/mariadb.png" width="17" width="17"/>|`stock:mariadb.png`|
|<img src="../src/main/resources/icons/mongodb.png" width="17" width="17"/>|`stock:mongodb.png`|
|<img src="../src/main/resources/icons/mssql.png" width="17" width="17"/>|`stock:mssql.png`|
|<img src="../src/main/resources/icons/mysql.png" width="17" width="17"/>|`stock:mysql.png`|
|<img src="../src/main/resources/icons/oracle.png" width="17" width="17"/>|`stock:oracle.png`|
|<img src="../src/main/resources/icons/postgresql.png" width="17" width="17"/>|`stock:postgresql.png`|
|<img src="../src/main/resources/icons/sqlite.png" width="17" width="17"/>|`stock:sqlite.png`|



## Template Instructions

Template is a text which uses predefined character sequence,
basically opened with `${` and closed with `}` characters.

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

### Control Statements

Contol statements branchs process with condition or iterates items.

#### `if` Statement

`if` statement branchs process with conditional statement.

```
${if:item=<field>, <condition>[, <condition>]}
 ...    // conditions met
[${elif:item=<field>, <conditions>[, <condition>]} ...]
 ...    // another condition met, multiple elif can be used
[${else}]
 ...    // condition not met
${endif}
```

Where `field` is a member of current object(table or column) [decorator](#item-statement)(like .camel, .suffix etc) can be used.
`conditions` can be added multiple times, in this case all conditions must met to execute `true` part.

`conditions` can be one of -

|Condition|Description|
|:---:|:---|
|`[value\|equals]=<value>`|When item value is equals `<value>`|
|`notEquals=<value>`|When item value is not equals `<value>`|
|`startsWith=<prefix>`|When item value starts with `<prefix>`|
|`notStartsWith=<prefix>`|When item value not starts with `<prefix>`|
|`endsWith=<suffix>`|When item value ends with `<suffix>`|
|`notEndsWith=<suffix>`|When item value ends with `<suffix>`|
|`contains=<item>`|When item collection contains `<item>` or when item is string and `<item>` is `,` separated string then item contained in `<item>`|
|`notContains=<item>`|When item collection not contains `<item>` or when item is string and `<item>` is `,` separated string then item not contained in `<item>`|
|`matches=<regex>`|When item value matches with regular expression `<regex>`, regex conformant with [Java Regex](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)|
|`notMatches=<regex>`|When item value not matches with regular expression `<regex>`, regex conformant with [Java Regex](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)|

Like many other language, multiple `elif` statement can be used and `else` statement is optional.
`if` statement must be closed with `endif` statement.

Examples:
```
This is ${if:item="type", startsWith="TABLE"}physical table${else}not table${endif}.
This is
${if:item="type", equals="TABLE"}
pysical table
${elif:item="type", equals="VIEW"}
virtual view
${else}
unknown object ${type}
${endif}.
```

#### `for` Statement

`for` statement repeats inside contents using collection items.

```
${for:item=<collection field>[, <controls>]}
 ...    // repeats
${endfor}
```

Where `collection field` is a collection type member of current object(obviously table object),
`for` statement must be closed with `endfor` statement.

`collection field` can be one of -

|Collection Field|Description|
|:---:|:---|
|`columns`|All columns in current table object|
|`keys`|Primary key fields in current table object|
|`notKeys`|All columns except primary keys in current table object|

and `controls` can be a combination of -

|Control|Description|
|:---:|:---|
|`inStr=<infix>`|Appends `<infix>` between iterations.|
|`indent=<spaces>`|An integer value applied after line break, can be negative.|
|`skipList=<skips>`|`,` separated column names string to skip loop|

Start location of `for` statement is important, every line after inside line break
will be indented to the start position, `indent` control will applied relatively
with this position.

> Note! Only collection item(obviously column item) is accessible inside `for` statement, if you want
> to access outer object use `super` statement.

Examples:
```sql
SELECT ${for:item=columns, inStr=","}
       ${name} AS "${name.camel}"${endfor}
  FROM ${table}
 WHERE ${for:item=keys, inStr="AND ", indent=-4}${name} = #{${name.camel}}
       ${endfor}
```

results like:
```sql
SELECT ALBUM_ID AS "albumId",
       ALBUM_NAME AS "albumName",
       ARTIST_NAME AS "artistName",
       PUBLISH_DATE AS "publishDate"
  FROM T_SAMPLE_ALBUM
 WHERE ALBUM_ID = #{albumId}

```


### `item` Statement

Item means member field of table/column object in this application.

```
${(<field name>|<custom variable>)[<decorators>]} or
${item:key=(<field name>|<custom variable>)[<decorators>][, <extra decorators>]}
```

Where `field name` is member field name of table/column, and `custom variable`
is user supplied variable in [Generator Main Window](#generator-main-window).

`decorators` can be a combination of -

|Decorator|Repeatable|Description|
|:---:|:---:|:---|
|`.abbr`|&#x2715;|Apply abbreviation mapping to this item(see [Abbreviation Mapping Window](#abbreviation-mapping-window). Abbreviations will be replaced to replacement word according to mapping rules.|
|`.suffix`|&#x25EF;|Remove prefix including first `_` in value(ex. `T_SAMPLE_ALBUM` -> `SAMPLE_ALBUM`)|
|`.prefix`|&#x25EF;|Remove suffix including last `_` in value(ex. `SAMPLE_ALBUM_T` -> `SAMPLE_ALBUM`)|
|`.camel`|&#x2715;|Change value to camel case(ex. `SAMPLE_ALBUM` -> `sampleAlbum`)|
|`.pascal`|&#x2715;|Change value to pascal case(ex. `SAMPLE_ALBUM` -> `SampleAlbum`)|
|`.snake`|&#x2715;|Change value to snake case(ex. `SAMPLE_ALBUM` -> `sample_album`)|
|`.screaming`|&#x2715;|Change value to screaming snake case(ex. `sample_album` -> `SAMPLE_ALBUM`)|
|`.skewer`|&#x2715;|Change value to skewer case(ex. `sample_album` -> `sample-album`)|
|`.kebab`|&#x2715;|Alias of `.skewer`|
|`.lower`|&#x2715;|Change value to lower case(ex. `SAMPLE_ALBUM` -> `sample_album`)|
|`.upper`|&#x2715;|Change value to upper case(ex. `sample_album` -> `SAMPLE_ALBUM`)|
|`.replace('X','Y')`|&#x25EF;|Replace `X` string to 'Y' string(ex. `sample_album` `name.replace('album', 'music')` -> `sample_music`)|

> Note! multiple decorators are processed in pipelined manner,
> in the other words, former decorator result will be the input of latter decorator.

`extra decorators` can be a combination of -

|Decorator|Description|
|:---:|:---|
|`padSize=<size>`|Appends spaces to make value length fit to `size`.|
|`padDir=<direction>`|`left` or `right`, where the spaces appended to.|
|`quote=<quote>`|Wrap value with `quote` string.|
|`prepend=<prepend>`|Prepend `prepend` string to value.|
|`postpend=<postpend>`|Postpend `postpend` string to value.|

Examples:
```
// if table like : T_SAMPLE_ALBUM ( ALBUM_ID, ALBUM_NAME )

${name.suffix.pascal}   // same as ${item:key=name.suffix.pascal}
// results SampleAlbum

${name.replace('SAMPLE', 'TEST').suffix.pascal} // same as ${item:key=name.replace('SAMPLE', 'TEST').suffix.pascal}
// results TestAlbum

[${for:item=columns, inStr=", "}${item:key=name.camel, quote="\""}${endfor}]
// results ["albumId", "albumName"]
```

Databse table object member fields:

|Member Field|Type|Description|
|:---:|:---:|:---|
|`catalog`|String|Database catalog where this table included.|
|`schema`|String|Database schema where this table included.|
|`name`|String|Table name.|
|`table`|String|Alias of `name`|
|`type`|String|JDBC compliant table type(`TABLE`, `VIEW`)|
|`remark`|String|Table comments|
|`columns`|Collection of column object|All columns in current table|
|`keys`|Collection of column object|Primary key columns of current table|
|`notKeys`|Collection of column object|All columns of current table except primary keys.|

Database column object member fields:

|Member Field|Type|Description|
|:---:|:---:|:---|
|`catalog`|String|Database catalog where this column included.|
|`schema`|String|Database schema where this column included.|
|`table`|String|Database table where this column included.|
|`name`|String|Column name.|
|`column`|String|Alias of `name`|
|`typeName`|String|Column data type name|
|`length`|Integer|Column data type length|
|`remarks`|String|Column comments|
|`nullable`|Integer|`0` or `1`. `1` is nullable|
|`isKey`|Boolean|`true` if this column is primary key|
|`defVal`|String|Default value of this column|
|`typeString`|String|Combination of `typeName` and `length`(ex. `VARCHAR(40)`).|
|`jdbcType`|String|JDBC compliant type name.|
|`javaType`|String|Corresponding java type name|

User created custom variables can be used anywhere with same usage of `item` statement.


### Other Statements

There are several utility statements for convenience like,

|Statement|Usage|Description|
|:---:|:---:|:---|
|`author`|`${author[:<extra decorator>]}`|User supplied `Author Name` field in [Generator Main Window](#generator-main-window).|
|`date`|`${date[:format=<date format>]}`|Current date with `date format` which compliant with [SimpleDateFormat](https://docs.oracle.com/en%2Fjava%2Fjavase%2F11%2Fdocs%2Fapi%2F%2F/java.base/java/text/SimpleDateFormat.html).|
|`user`|`${user[:<extra decorator>]}`|OS login user ID.|
|`super`|`${super:key=<table member field>[<decorators>][, <extra decorator>]}`|Only available inside of `for` statement, you can access table object using this statement|
|Text|`${"any string can include '${' or '}'"}`|Statement for escaping `${` or `}`|


## Custom Queries

If JDBC driver is not fully compliant with JDBC specifications, getting information
of table or column will not performed successfully.
So database dependent query must be supplied to get these informations.

### Get Table Comments SQL

A SQL to get all table name and it's comments.

Results must contain table name and it's comments with ordered.

Supplied parameters:

|Parameter|Description|
|:---:|:---|
|`catalog`|Database catalog where schema included|
|`schema`|Database schema where table included|

Result set must contain these fields:

|Fields|Type|Description|
|:---:|:---:|:---|
|First field|String|Table name|
|Second field|String|Table comments|

Example for MS SQL Server:
```sql
SELECT OBJNAME, cast(value as varchar(8000)) as VALUE 
FROM fn_listextendedproperty ('MS_DESCRIPTION','schema','${schema}','table',null,null,null)
```


### Get Column Comments SQL

A SQL to get all column name of a table and it's comments.

Results must contain column name and it's comments with ordered.

Supplied parameters:

|Parameter|Description|
|:---:|:---|
|`catalog`|Database catalog where schema included|
|`schema`|Database schema where table included|
|`table`|Database table where columns included|

Result set must contain these fields:

|Fields|Type|Description|
|:---:|:---:|:---|
|First field|String|Table column name|
|Second field|String|Table column comments|

Example for MS SQL Server:
```sql
SELECT OBJNAME, cast(value as varchar(8000)) as VALUE 
FROM fn_listextendedproperty ('MS_DESCRIPTION','schema','${schema}','table','${table}','column',null)
```

### Get Table List SQL

A SQL to get all table informations.

Results must contain correct field name and informations.

Supplied parameters:

|Parameter|Description|
|:---:|:---|
|`catalog`|Database catalog where schema included|
|`schema`|Database schema where table included|

Result set must contain these fields:

|Fields|Type|Description|
|:---:|:---:|:---|
|`TABLE_CAT`|String|Database catalog where schema included|
|`TABLE_SCHEM`|String|Database schema where table included|
|`TABLE_NAME`|String|Table name|
|`TABLE_TYPE`|String|JDBC table type(ex. `TABLE`, `VIEW)|
|`REMARKS`|String|Table comments|

Example for H2 database:
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

### Get Column List SQL

A SQL to get all column informations of a table.

Results must contain correct field name and informations.

Supplied parameters:

|Parameter|Description|
|:---:|:---|
|`catalog`|Database catalog where schema included|
|`schema`|Database schema where table included|
|`table`|Database table where columns included|

Result set must contain these fields:

|Fields|Type|Description|
|:---:|:---:|:---|
|`TABLE_CAT`|String|Database catalog where schema included|
|`TABLE_SCHEM`|String|Database schema where table included|
|`TABLE_NAME`|String|Table name|
|`COLUMN_NAME`|String|Column name|
|`DATA_TYPE`|Integer|Integer value corresponding to [java.sql.Types](https://docs.oracle.com/javase/8/docs/api/java/sql/Types.html)|
|`TYPE_NAME`|String|Database specific data type name|
|`COLUMN_SIZE`|Integer|Column size(length)|
|`NULLABLE`|Integer|`0` or `1`. `1` means nullable|
|`REMARKS`|String|Column comments|
|`COLUMN_DEF`|String|Column default value|
|`IS_KEY`|Integer|`0` or `1`. `1` means this column is member of primary key|

Example for H2 database:
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
            ELSE 0 END AS "DATA_TYPE",
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
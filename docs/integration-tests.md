# Database Integration Tests

The unit tests run against H2 and hand written result sets. On top of them, the
`integrationTest` source set opens a **real** PostgreSQL, MySQL, MariaDB, SQL
Server and Oracle server with
[Testcontainers](https://testcontainers.com/), seeds it, and reads it back
through `DBMeta` with the **stock driver definitions** of
`defaultConfig.json` — the definitions a user gets out of the box, with nothing
filled in but the path of the driver jar.

Each database is checked the same way:

* the stock driver definition opens the connection,
* the seeded schema shows up in `getSchemas()` and in `getSchemaTree()`,
* the table list holds the two seeded tables, and the seeded view only when
  views are asked for,
* the columns arrive in order, with the primary key flags and the key/non-key
  split the templates iterate over — including a two column key,
* the table and column comments of the seed are reported, through the driver
  metadata or through the custom comment queries of the definition (SQL Server).

On PostgreSQL the generator is additionally driven end to end: the shipped
`templates/java_model.java` is applied to a table read out of the container and
the generated source is checked.

## Requirements

* **Docker** has to be running and able to start Linux containers — Docker
  Desktop with its Linux engine on Windows, a Docker daemon anywhere else.
  Nothing else has to be installed: the JDBC drivers are downloaded by the build
  and the database servers come as container images.
* Roughly **4 GB of disk** for the five images, and enough memory for the SQL
  Server and Oracle containers (about 2 GB each while they run).

The images are pulled on the first run, which takes a while; every run after
that starts them from the local image cache.

## Running them

```console
> .\gradlew.bat integrationTest
```

They are deliberately **not** part of `check` or `build`, so a machine without
Docker still builds and tests the application.

One database at a time — which is the way to work on a single dialect, and
avoids starting five servers at once:

```console
> .\gradlew.bat integrationTest --tests "*PostgresDatabaseIT*"
> .\gradlew.bat integrationTest --tests "*MySqlDatabaseIT*"
> .\gradlew.bat integrationTest --tests "*MariaDbDatabaseIT*"
> .\gradlew.bat integrationTest --tests "*SqlServerDatabaseIT*"
> .\gradlew.bat integrationTest --tests "*OracleDatabaseIT*"
```

A single test method works the same way:

```console
> .\gradlew.bat integrationTest --tests "*PostgresDatabaseIT.theShippedTemplateIsRenderedFromTheDatabase"
```

The report of a run is written to
`build/reports/tests/integrationTest/index.html`.

## How it is put together

| Where | What |
|:---|:---|
| `src/integrationTest/java/comart/tools/jdbgen/it/AbstractDatabaseIT.java` | the assertions every database shares, plus the helpers that read a stock driver definition and open a `DBMeta` with it |
| `src/integrationTest/java/.../<Database>DatabaseIT.java` | the container, the connection settings and the DDL dialect of one database |
| `build/int-drivers/` | the JDBC driver jars, copied there by the `copyIntDrivers` task; the tests point the `jdbcJar` of the driver definition at them, and find the directory through the `intdrivers.dir` system property |

The driver versions are declared in `build.gradle` as `jdbcDriverCoordinates`,
the Testcontainers version as `testcontainersVersion`.

## If it does not run

**`Could not find a valid Docker environment`** — the Docker daemon is not
running, or the current user cannot talk to it. `docker version` has to work in
the same shell.

**`client version 1.32 is too old`** — the Docker client inside Testcontainers
asks for an API version the engine no longer serves. The build already asks for
1.40; a different one can be selected with
`.\gradlew.bat integrationTest -PdockerApiVersion=1.44`.

**A container times out while starting** — SQL Server and Oracle need a couple
of minutes on a cold machine. The startup timeouts are set in the test classes;
a machine that is short on memory is the usual reason for a container that never
becomes healthy.

Left over containers are removed by the Testcontainers reaper, `testcontainers/
ryuk`, which runs as a container of its own — it is expected to show up in
`docker ps` while the tests run.

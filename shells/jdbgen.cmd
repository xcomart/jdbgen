@echo off

cd /d "%~dp0"

IF "%JAVA_HOME%" == "" (
    SET JAVA=java
) ELSE (
    SET JAVA=%JAVA_HOME%\bin\java
)

SET JAVA_VER=
SET JAVA_MAJOR=
SET JAVA_MINOR=

REM javap ships with the JDK only, so the version is read from java -version,
REM which a plain JRE provides as well. It is printed on stderr.
REM The nested "cmd /c" keeps the quotes around a JAVA_HOME containing spaces
REM from being eaten by the command line FOR /F builds.
FOR /f "tokens=3" %%A IN ('cmd /c ""%JAVA%.exe" -version" 2^>^&1 ^| findstr /i "version"') DO IF NOT DEFINED JAVA_VER SET JAVA_VER=%%A

IF NOT DEFINED JAVA_VER GOTO :NOVERSION
SET JAVA_VER=%JAVA_VER:"=%
FOR /f "tokens=1 delims=._-+" %%A IN ("%JAVA_VER%") DO SET JAVA_MAJOR=%%A
FOR /f "tokens=2 delims=._-+" %%A IN ("%JAVA_VER%") DO SET JAVA_MINOR=%%A
REM Legacy "1.x" scheme: the major version is x.
IF "%JAVA_MAJOR%" == "1" SET JAVA_MAJOR=%JAVA_MINOR%
IF NOT DEFINED JAVA_MAJOR GOTO :NOVERSION

IF %JAVA_MAJOR% LSS 11 (
    ECHO "Java 11 or above required to run this application!"
    ECHO "You need to install JRE(Java Runtime Environment) version 11 or above."
)
GOTO :FINDJAR

:NOVERSION
ECHO "Unable to determine the Java version, running anyway."

:FINDJAR
SET JDBGEN=
for %%I in (jdbgen-*.jar) do set JDBGEN=%%~fI

IF NOT DEFINED JDBGEN (
    ECHO "Cannot find jdbgen-*.jar in %~dp0!"
    EXIT /B 1
)

start "" "%JAVA%w.exe" -jar "%JDBGEN%"


cd %~dp0

IF "%JAVA_HOME%" == "" (
    SET JAVA=java
) ELSE (
    SET JAVA=%JAVA_HOME%\bin\java
)

FOR /f "tokens=*" %%I IN ('%JAVA%p -verbose java.lang.String | grep "major version"') DO FOR %%A IN (%%~I) DO SET JAVA_VER=%%A

IF "%JAVA_VER%" < "55" (
    ECHO "Java 11 or above required to run this application!"
    ECHO "You need to install JRE(Java Runtime Environment) version 11 or above."
)

for /r %%i in (jdbgen-*.jar) do set JDBGEN=%%i

start %JAVA%w -jar %JDBGEN%

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  docker startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and DOCKER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\docker-0.1-SNAPSHOT.jar;%APP_HOME%\lib\plugin-sdk-2.1.2.jar;%APP_HOME%\lib\spring-context-4.2.3.RELEASE.jar;%APP_HOME%\lib\google-java-format-1.1.jar;%APP_HOME%\lib\docker-java-3.0.6.jar;%APP_HOME%\lib\exception-2.1.2.jar;%APP_HOME%\lib\monitoring-2.1.2.jar;%APP_HOME%\lib\amqp-client-3.5.6.jar;%APP_HOME%\lib\gson-2.5.jar;%APP_HOME%\lib\vim-drivers-2.1.2.jar;%APP_HOME%\lib\spring-aop-4.2.3.RELEASE.jar;%APP_HOME%\lib\spring-beans-4.2.3.RELEASE.jar;%APP_HOME%\lib\spring-core-4.2.3.RELEASE.jar;%APP_HOME%\lib\spring-expression-4.2.3.RELEASE.jar;%APP_HOME%\lib\guava-19.0.jar;%APP_HOME%\lib\org.eclipse.jdt.core-3.10.0.jar;%APP_HOME%\lib\jackson-jaxrs-json-provider-2.6.4.jar;%APP_HOME%\lib\jersey-apache-connector-2.23.1.jar;%APP_HOME%\lib\httpcore-4.4.5.jar;%APP_HOME%\lib\jersey-client-2.23.1.jar;%APP_HOME%\lib\junixsocket-common-2.0.4.jar;%APP_HOME%\lib\junixsocket-native-common-2.0.4.jar;%APP_HOME%\lib\commons-compress-1.12.jar;%APP_HOME%\lib\commons-codec-1.10.jar;%APP_HOME%\lib\commons-lang-2.6.jar;%APP_HOME%\lib\commons-io-2.5.jar;%APP_HOME%\lib\jcl-over-slf4j-1.7.21.jar;%APP_HOME%\lib\bcpkix-jdk15on-1.54.jar;%APP_HOME%\lib\netty-codec-http-4.1.3.Final.jar;%APP_HOME%\lib\netty-handler-4.1.3.Final.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.3.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.3.Final-linux-x86_64.jar;%APP_HOME%\lib\catalogue-2.1.2.jar;%APP_HOME%\lib\plugin-2.1.2.jar;%APP_HOME%\lib\aopalliance-1.0.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\org.eclipse.core.resources-3.7.100.jar;%APP_HOME%\lib\org.eclipse.core.runtime-3.7.0.jar;%APP_HOME%\lib\org.eclipse.core.filesystem-1.3.100.jar;%APP_HOME%\lib\org.eclipse.text-3.5.101.jar;%APP_HOME%\lib\jackson-jaxrs-base-2.6.4.jar;%APP_HOME%\lib\jackson-core-2.6.4.jar;%APP_HOME%\lib\jackson-databind-2.6.4.jar;%APP_HOME%\lib\jackson-module-jaxb-annotations-2.6.4.jar;%APP_HOME%\lib\jersey-common-2.23.1.jar;%APP_HOME%\lib\javax.ws.rs-api-2.0.1.jar;%APP_HOME%\lib\hk2-api-2.4.0-b34.jar;%APP_HOME%\lib\javax.inject-2.4.0-b34.jar;%APP_HOME%\lib\hk2-locator-2.4.0-b34.jar;%APP_HOME%\lib\log4j-1.2.17.jar;%APP_HOME%\lib\native-lib-loader-2.0.2.jar;%APP_HOME%\lib\bcprov-jdk15on-1.54.jar;%APP_HOME%\lib\netty-codec-4.1.3.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.3.Final.jar;%APP_HOME%\lib\netty-transport-4.1.3.Final.jar;%APP_HOME%\lib\netty-codec-socks-4.1.3.Final.jar;%APP_HOME%\lib\netty-common-4.1.3.Final.jar;%APP_HOME%\lib\hibernate-jpa-2.1-api-1.0.0.Final.jar;%APP_HOME%\lib\spring-boot-1.2.5.RELEASE.jar;%APP_HOME%\lib\utils-2.1.2.jar;%APP_HOME%\lib\org.eclipse.core.expressions-3.4.300.jar;%APP_HOME%\lib\org.eclipse.osgi-3.7.1.jar;%APP_HOME%\lib\org.eclipse.equinox.common-3.6.0.jar;%APP_HOME%\lib\org.eclipse.core.jobs-3.5.100.jar;%APP_HOME%\lib\org.eclipse.equinox.registry-3.5.101.jar;%APP_HOME%\lib\org.eclipse.equinox.preferences-3.4.1.jar;%APP_HOME%\lib\org.eclipse.core.contenttype-3.4.100.jar;%APP_HOME%\lib\org.eclipse.equinox.app-1.3.100.jar;%APP_HOME%\lib\org.eclipse.core.commands-3.6.0.jar;%APP_HOME%\lib\javax.annotation-api-1.2.jar;%APP_HOME%\lib\jersey-guava-2.23.1.jar;%APP_HOME%\lib\osgi-resource-locator-1.0.1.jar;%APP_HOME%\lib\hk2-utils-2.4.0-b34.jar;%APP_HOME%\lib\aopalliance-repackaged-2.4.0-b34.jar;%APP_HOME%\lib\javassist-3.18.1-GA.jar;%APP_HOME%\lib\netty-resolver-4.1.3.Final.jar;%APP_HOME%\lib\spring-boot-autoconfigure-1.2.5.RELEASE.jar;%APP_HOME%\lib\spring-boot-starter-logging-1.2.5.RELEASE.jar;%APP_HOME%\lib\javax.inject-1.jar;%APP_HOME%\lib\snakeyaml-1.14.jar;%APP_HOME%\lib\jul-to-slf4j-1.7.12.jar;%APP_HOME%\lib\log4j-over-slf4j-1.7.12.jar;%APP_HOME%\lib\logback-classic-1.1.3.jar;%APP_HOME%\lib\logback-core-1.1.3.jar;%APP_HOME%\lib\slf4j-api-1.7.21.jar;%APP_HOME%\lib\jackson-annotations-2.6.0.jar;%APP_HOME%\lib\httpclient-4.5.1.jar

@rem Execute docker
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %DOCKER_OPTS%  -classpath "%CLASSPATH%" org.openbaton.docker.DockerVim %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable DOCKER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%DOCKER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

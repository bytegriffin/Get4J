@echo off

if "%1"=="" goto Usage
if "%1" == "start" goto doStart
if "%1" == "restart" goto doRestart
if "%1" == "stop" goto doStop


:Usage
echo """""""""""""""""""""""""""""""""""""""""""""""""""""
echo "               Get4J    Console                    "
echo """""""""""""""""""""""""""""""""""""""""""""""""""""
echo "Usage:    spider  <Commands>                       "
echo "Commands:       start       start the spider       "
echo "                restart     restart the spider     "
echo "                stop        stop the spider        "
echo """""""""""""""""""""""""""""""""""""""""""""""""""""
goto end

:doStart
shift
set ACTION=start
goto run

:doRestart
shift
set ACTION=start
goto run

:doStop
shift
set ACTION=stop
goto run

:end
exit /b 0

:exit
exit /b 1

:noJreHome
rem Needed at least a JRE
echo The JRE_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto exit


:ADDLiB
SET CLASSPATH=%1;%CLASSPATH%
goto :EOF

:run
set "JRE_HOME=%JAVA_HOME%"
if not exist "%JRE_HOME%\bin\java.exe" goto noJreHome
set RUNJAVA="%JRE_HOME%\bin\java.exe"
set "GET4J_BIN_DIR=%cd%"
cd ..
set "GET4J_HOME=%cd%"

SetLocal EnableDelayedExpansion
set "CLASSPATH=.;%GET4J_HOME%\conf"
for /R %GET4J_HOME%\lib\ %%i in (*.jar) do (
call :ADDLiB %%i
)
set MAINCLASS=com.bytegriffin.get4j.Spider
set JAVA_OPTS=-server -Xms512m -Xmx512m -XX:+UseParallelGC -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -Djcore.parser=SAX -Djcore.logger=org.apache.log4j
%RUNJAVA% -classpath "%CLASSPATH%" %JAVA_OPTS% %MAINCLASS%
EndLocal
goto end

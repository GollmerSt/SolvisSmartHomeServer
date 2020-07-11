@echo off

cd /d "%~dp0"
set paras=
set java=java
set pause=pause

if "%1" == "learn" (
	set paras="--server-learn"
	set java=java
	set pause=pause
)

if "%1" == "terminate" (
	set paras="--server-terminate"
	set java=javaw
	set pause=
)

if "%1" == "crypt" (
	set /p "toCrypt=Zu verschluesselnder String eingeben: "
	set paras="--string-to-crypt=%toCrypt%"
	set java=java
	set pause=pause
)
if "%1" == "dos" (
	set java=java
	set pause=
)

if "%1" == "" (
	ping localhost -n 5 > NUL
	set java=javaw
	set pause=
)

%java% -jar SolvisSmartHomeServer.jar %paras%

%pause%

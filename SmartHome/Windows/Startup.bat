@echo off

cd /d "%~dp0"
set paras=
set java=java
set pause=pause
set out=0
set file=""

goto start_%1

:start_learn
	set paras="--server-learn"
	set java=java
	set pause=pause
	goto continue

:start_csv
	set paras="--channels --csvSemicolon"
	set java=java
	set pause=pause
	goto continue

:start_iobroker
	set paras="--iobroker"
	set java=java
	set pause=pause
	goto continue

:start_terminate
	set paras="--server-terminate"
	set java=javaw
	set pause=
	goto continue

:start_crypt
	set /p toCrypt="Zu verschluesselnden String eingeben: "
	set paras=--string-to-crypt="%toCrypt%"
	set java=java
	set pause=pause
	goto continue

:start_dos
	set java=java
	set pause=pause
	goto continue

:start_
	ping localhost -n 5 > NUL
	set java=javaw
	set pause=
	goto continue

:continue
	if %out% GTR 0 (
		%java% -jar SolvisSmartHomeServer.jar %paras% > %file%
		echo Die Datei %file% wurde geschrieben.
	) else (
		%java% -jar SolvisSmartHomeServer.jar %paras%
	)

	%pause%

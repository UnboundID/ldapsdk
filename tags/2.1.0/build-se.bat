@echo off

rem Copyright 2008-2011 UnboundID Corp.
rem All Rights Reserved.
rem
rem -----
rem
rem Copyright (C) 2008-2011 UnboundID Corp.
rem This program is free software; you can redistribute it and/or modify
rem it under the terms of the GNU General Public License (GPLv2 only)
rem or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
rem as published by the Free Software Foundation.
rem
rem This program is distributed in the hope that it will be useful,
rem but WITHOUT ANY WARRANTY; without even the implied warranty of
rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
rem GNU General Public License for more details.
rem
rem You should have received a copy of the GNU General Public License


rem Copyright 2007-2011 UnboundID Corp.
rem All Rights Reserved.


setlocal
set SCRIPT_DIR=%~dP0

set ANT_HOME=%SCRIPT_DIR%\ext\ant
"%ANT_HOME%\bin\ant" -f "%SCRIPT_DIR%\build-se.xml" %*

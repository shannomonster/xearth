# Microsoft Developer Studio Project File - Name="xearth" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Application" 0x0101

CFG=xearth - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "xearth.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "xearth.mak" CFG="xearth - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "xearth - Win32 Release" (based on "Win32 (x86) Application")
!MESSAGE "xearth - Win32 Debug" (based on "Win32 (x86) Application")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "xearth - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "Release"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /YX /FD /c
# ADD CPP /nologo /MT /W3 /GX /O2 /D "NDEBUG" /D "WIN32" /D "_WINDOWS" /D "STRICT" /YX /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /o "NUL" /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /o "NUL" /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:windows /machine:I386
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib comctl32.lib wininet.lib /nologo /subsystem:windows /machine:I386

!ELSEIF  "$(CFG)" == "xearth - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "Debug"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /W3 /Gm /GX /Zi /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /YX /FD /c
# ADD CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "_DEBUG" /D "WIN32" /D "_WINDOWS" /D "STRICT" /YX /FD /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /o "NUL" /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /o "NUL" /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:windows /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib comctl32.lib wininet.lib /nologo /subsystem:windows /debug /machine:I386 /pdbtype:sept

!ENDIF 

# Begin Target

# Name "xearth - Win32 Release"
# Name "xearth - Win32 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "c,cpp"
# Begin Source File

SOURCE=.\bmp.cpp
# End Source File
# Begin Source File

SOURCE=.\dialog.cpp
# End Source File
# Begin Source File

SOURCE=.\dither.c
# End Source File
# Begin Source File

SOURCE=.\extarr.c
# End Source File
# Begin Source File

SOURCE=.\gif.c
# End Source File
# Begin Source File

SOURCE=.\gifout.c
# End Source File
# Begin Source File

SOURCE=.\mapdata.c
# End Source File
# Begin Source File

SOURCE=.\markerdlg.cpp
# End Source File
# Begin Source File

SOURCE=.\markers.c
# End Source File
# Begin Source File

SOURCE=.\ppm.c
# End Source File
# Begin Source File

SOURCE=.\properties.cpp
# End Source File
# Begin Source File

SOURCE=.\quake.cpp
# End Source File
# Begin Source File

SOURCE=.\registry.cpp
# End Source File
# Begin Source File

SOURCE=.\render.c
# End Source File
# Begin Source File

SOURCE=.\scan.c
# End Source File
# Begin Source File

SOURCE=.\settings.cpp
# End Source File
# Begin Source File

SOURCE=.\subclasswnd.cpp
# End Source File
# Begin Source File

SOURCE=.\sunpos.c
# End Source File
# Begin Source File

SOURCE=.\xearth.c
# End Source File
# Begin Source File

SOURCE=.\xearthwin.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h"
# Begin Source File

SOURCE=.\dialog.h
# End Source File
# Begin Source File

SOURCE=.\extarr.h
# End Source File
# Begin Source File

SOURCE=.\gifint.h
# End Source File
# Begin Source File

SOURCE=.\giflib.h
# End Source File
# Begin Source File

SOURCE=.\kljcpyrt.h
# End Source File
# Begin Source File

SOURCE=.\markerdlg.h
# End Source File
# Begin Source File

SOURCE=.\markers.h
# End Source File
# Begin Source File

SOURCE=.\port.h
# End Source File
# Begin Source File

SOURCE=.\properties.h
# End Source File
# Begin Source File

SOURCE=.\quake.h
# End Source File
# Begin Source File

SOURCE=.\registry.h
# End Source File
# Begin Source File

SOURCE=.\resource.h
# End Source File
# Begin Source File

SOURCE=.\settings.h
# End Source File
# Begin Source File

SOURCE=.\subclasswnd.h
# End Source File
# Begin Source File

SOURCE=.\xearth.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter ""
# Begin Source File

SOURCE=.\cursor1.cur
# End Source File
# Begin Source File

SOURCE=.\display.ico
# End Source File
# Begin Source File

SOURCE=.\dots.ico
# End Source File
# Begin Source File

SOURCE=.\earth.ico
# End Source File
# Begin Source File

SOURCE=.\labels.ico
# End Source File
# Begin Source File

SOURCE=.\quakes.ico
# End Source File
# Begin Source File

SOURCE=.\shading.ico
# End Source File
# Begin Source File

SOURCE=.\sun.ico
# End Source File
# Begin Source File

SOURCE=.\time.ico
# End Source File
# Begin Source File

SOURCE=.\viewpoint.ico
# End Source File
# Begin Source File

SOURCE=.\winearth.rc
# End Source File
# End Group
# Begin Source File

SOURCE=.\todo.txt
# End Source File
# End Target
# End Project

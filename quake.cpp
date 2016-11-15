/*
 * quake.cpp
 *
 * Copyright (C) 1998 Greg Hewgill
 *
 * Get earthquake information from:
 *   finger quake@gldfs.cr.usgs.gov
 *
 */

#include <windows.h>
#include <process.h>
#include <time.h>
#include <wininet.h>

#include <string>

#include "quake.h"

#include "registry.h"
#include "settings.h"

using namespace std;

CRITICAL_SECTION QuakeMutex;
std::list<quake> Quakes;
HANDLE UpdateNow;
bool Updated = false;

string GetUrl(HINTERNET session, const char url[], bool nocache = false)
{
    string r;
    HINTERNET request = InternetOpenUrl(session, url, NULL, 0, (nocache ? (INTERNET_FLAG_PRAGMA_NOCACHE|INTERNET_FLAG_NO_CACHE_WRITE) : 0) | INTERNET_FLAG_KEEP_CONNECTION, 0);
    if (request == NULL) {
        return r;
    }
    DWORD code;
    DWORD codelen = sizeof(code);
    if (!HttpQueryInfo(request, HTTP_QUERY_STATUS_CODE|HTTP_QUERY_FLAG_NUMBER, &code, &codelen, 0) || code != 200) {
        InternetCloseHandle(request);
        return r;
    }
    while (true) {
        char buf[4096+1];
        DWORD n;
        if (!InternetReadFile(request, buf, sizeof(buf)-1, &n) || n == 0) {
            break;
        }
        buf[n] = 0;
        r += buf;
    }
    InternetCloseHandle(request);
    return r;
}

void QuakeThread(void *)
{
  if (Settings.quakes && Settings.qdelay > 0) {
    SetEvent(UpdateNow);
  }
  while (true) {
    if (Settings.quakes && Settings.qdelay > 0) {
      WaitForSingleObject(UpdateNow, Settings.qdelay*3600*1000);
    } else {
      WaitForSingleObject(UpdateNow, INFINITE);
    }
    DWORD flags;
    while (!InternetGetConnectedState(&flags, 0)) {
      Sleep(60*1000);
    }
    HINTERNET session = InternetOpen("xearth for Windows/1.1", INTERNET_OPEN_TYPE_PRECONFIG, NULL, NULL, 0);
    string page = GetUrl(session, "http://neic.usgs.gov/neis/finger/quake.asc", true);
    if (!page.empty()) {
      EnterCriticalSection(&QuakeMutex);
      Quakes.clear();
      char *p = strstr(page.c_str(), "yy/mm/dd");
      if (p != NULL) {
        p = strchr(p, '\n');
        if (p != NULL) {
          p++;
        }
      }
      if (p) {
        while (1) {
          char *q = strchr(p, '\n');
          if (q == 0) {
            break;
          }
          *q++ = 0;
          int y, m, d, h, n, s;
          float lat, lon, dep, mag;
          char NS, WE;
          char Q;
          char name[80];
          if (sscanf(p, "%d/%d/%d %d:%d:%d %f%c %f%c %f %f%*c%*c%*c%c%s", &y, &m, &d, &h, &n, &s, &lat, &NS, &lon, &WE, &dep, &mag, &Q, name) == 14) {
            if (NS == 'S') {
              lat = -lat;
            }
            if (WE == 'W') {
              lon = -lon;
            }
            quake qk;
            qk.time.wYear = y < 100 ? y < 40 ? 2000+y : 1900+y : y;
            qk.time.wMonth = m;
            qk.time.wDay = d;
            qk.time.wHour = h;
            qk.time.wMinute = n;
            qk.time.wSecond = s;
            qk.time.wMilliseconds = 0;
            qk.lat = lat;
            qk.lon = lon;
            qk.dep = dep;
            qk.mag = mag;
            qk.location = p+49;
            Quakes.push_back(qk);
          }
          p = q;
        }
      }
      LeaveCriticalSection(&QuakeMutex);

      HKEY k;
      DWORD disp;
      if (RegOpenKeyEx(HKEY_CURRENT_USER, "Software\\Software Gems\\xearth for Windows", 0, KEY_ALL_ACCESS, &k) == ERROR_SUCCESS) {
        RegDeleteKey(k, "quakes");
        RegCloseKey(k);
        if (RegCreateKeyEx(HKEY_CURRENT_USER, "Software\\Software Gems\\xearth for Windows\\quakes", 0, NULL, 0, KEY_ALL_ACCESS, NULL, &k, &disp) == ERROR_SUCCESS) {
          for (std::list<quake>::const_iterator q = Quakes.begin(); q != Quakes.end(); q++) {
            char name[80];
            sprintf(name, "%04d%02d%02d%02d%02d%02d",
              q->time.wYear,
              q->time.wMonth,
              q->time.wDay,
              q->time.wHour,
              q->time.wMinute,
              q->time.wSecond);
            char buf[80];
            sprintf(buf, "%g:%g:%g:%g:%s",
              q->lat,
              q->lon,
              q->dep,
              q->mag,
              q->location.c_str());
            RegSetValueEx(k, name, 0, REG_SZ, (BYTE *)buf, strlen(buf)+1);
          }
          RegCloseKey(k);
        }
      }

      Settings.qupdated = time(0);
      SetRegistryDword("qupdated", Settings.qupdated);
      Updated = true;
    }
    InternetCloseHandle(session);
  }
}

std::list<quake> GetQuakes()
{
  EnterCriticalSection(&QuakeMutex);
  std::list<quake> quakes = Quakes;
  LeaveCriticalSection(&QuakeMutex);
  return quakes;
}

void UpdateQuakes()
{
  SetEvent(UpdateNow);
}

bool QuakesUpdated()
{
  if (!Updated) {
    return false;
  }
  Updated = false;
  return true;
}

void StartQuakeThread()
{
  InitializeCriticalSection(&QuakeMutex);
  UpdateNow = CreateEvent(NULL, FALSE, FALSE, NULL);
  _beginthread(QuakeThread, 0, NULL);
}

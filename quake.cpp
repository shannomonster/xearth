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

#include <regex>
#include <string>

#include "quake.h"

#include "registry.h"
#include "settings.h"

using namespace std;

static const char *URL = R"(https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.csv)";

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
  std::regex quakes_re(
    R"_(^(\d\d\d\d)-(\d\d)-(\d\d))_" // date yyyy-mm-dd
    R"_(T(\d\d):(\d\d):(\d\d).(\d\d\d)Z,)_" // time HH::MM::SS.sss
    R"_(([\d\.\-]*),)_" // lat
    R"_(([\d\.\-]*),)_" // lon
    R"_(([\d\.\-]*),)_" // depth
    R"_(([\d\.\-]*),)_" // mag
    R"_([^,]*,)_" // magType skipped
    R"_([^,]*,)_" // nst skipped
    R"_([^,]*,)_" // gap skipped
    R"_([^,]*,)_" // dmin skipped
    R"_([^,]*,)_" // rms skipped
    R"_([^,]*,)_" // net skipped
    R"_([^,]*,)_" // id skipped
    R"_([^,]*,)_" // update skipped
    R"_("([^,]*)",)_", // place
    std::regex_constants::ECMAScript | std::regex_constants::optimize);

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
    string page = GetUrl(session, URL, true);
    if (!page.empty()) {
      std::list<quake> parse_quakes;
      for (auto siter = std::sregex_iterator(page.begin(), page.end(), quakes_re); siter != std::sregex_iterator{}; ++siter)
      {
        quake qk;
        qk.time.wYear = std::atoi(siter->str(1).c_str());
        qk.time.wMonth = std::atoi(siter->str(2).c_str());
        qk.time.wDay = std::atoi(siter->str(3).c_str());
        qk.time.wHour = std::atoi(siter->str(4).c_str());
        qk.time.wMinute = std::atoi(siter->str(5).c_str());
        qk.time.wSecond = std::atoi(siter->str(6).c_str());
        qk.time.wMilliseconds = std::atoi(siter->str(7).c_str());
        qk.lat = std::atof(siter->str(8).c_str());
        qk.lon = std::atof(siter->str(9).c_str());
        qk.dep = std::atof(siter->str(10).c_str());
        qk.mag = std::atof(siter->str(11).c_str());
        qk.location = siter->str(12);
        parse_quakes.push_back(std::move(qk));
      }

      EnterCriticalSection(&QuakeMutex);
      Quakes.swap(parse_quakes);
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

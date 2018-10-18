/*
 * settings.cpp
 *
 * Copyright (C) 1996 Greg Hewgill
 *
 * Persistent settings support
 *
 */

#include <windows.h>
#pragma hdrstop

extern "C" {
#include "markers.h"
}

#include "settings.h"

#include "registry.h"

extern "C" {
#include "port.h"
#include "extarr.h"
}

TSettings Settings;

TSettings::TSettings()
{
  properties.x = 0;
  properties.y = 0;
  proj = 0;
  pos.type = Position::_default;
  pos.latitude = 0;
  pos.longitude = 0;
  pos.rlatitude = 0;
  pos.rlongitude = 0;
  pos.period = 1;
  pos.inclination = 0;
  rot = 0;
  rot_galactic = FALSE;
  sunpos.type = Position::_default;
  sunpos.latitude = 0;
  sunpos.longitude = 0;
  mag = 1;
  size.cx = 0;
  size.cy = 0;
  shift.x = 0;
  shift.y = 0;
  shade = TRUE;
  label = FALSE;
  labelpos = 0;
  markers = TRUE;
  //strcpy(markerfile, "built-in");
  stars = TRUE;
  starfreq = (float)0.002;
  bigstars = 0;
  grid = FALSE;
  grid1 = 6;
  grid2 = 15;
  day = 100;
  night = 5;
  term = 1;
  gamma = 1.0;
  wait = 5;
  timewarp = 1;
  time = 0;
  quakes = 1;
  qdelay = 3;
  ZeroMemory(&qupdated, sizeof(qupdated));
//  strcpy(font_name, "MS Sans Serif");
//  font_size = 8;
  disable_rdc = FALSE;
  save_png = TRUE;

  unsigned long long q;
  DWORD d;
  BOOL b;
  float f;
  if (GetRegistryDword("properties.x", d))       properties.x = d;
  if (GetRegistryDword("properties.y", d))       properties.y = d;
  if (GetRegistryDword("proj", d))               proj = d;
  if (GetRegistryDword("pos.type", d))           pos.type = (Position::Type)d;
  if (GetRegistryFloat("pos.latitude", f))       pos.latitude = f;
  if (GetRegistryFloat("pos.longitude", f))      pos.longitude = f;
  if (GetRegistryFloat("pos.rlatitude", f))      pos.rlatitude = f;
  if (GetRegistryFloat("pos.rlongitude", f))     pos.rlongitude = f;
  if (GetRegistryFloat("pos.period", f))         pos.period = f;
  if (GetRegistryFloat("pos.inclination", f))    pos.inclination = f;
  if (GetRegistryFloat("rot", f))                rot = f;
  if (GetRegistryBool("rot.galactic", b))        rot_galactic = b;
  if (GetRegistryDword("sunpos.type", d))        sunpos.type = (Position::Type)d;
  if (GetRegistryFloat("sunpos.latitude", f))    sunpos.latitude = f;
  if (GetRegistryFloat("sunpos.longitude", f))   sunpos.longitude = f;
  if (GetRegistryFloat("mag", f))                mag = f;
  if (GetRegistryDword("size.cx", d))            size.cx = d;
  if (GetRegistryDword("size.cy", d))            size.cy = d;
  if (GetRegistryDword("shift.x", d))            shift.x = d;
  if (GetRegistryDword("shift.y", d))            shift.y = d;
  if (GetRegistryBool("shade", b))               shade = b;
  if (GetRegistryBool("label", b))               label = b;
  if (GetRegistryDword("labelpos", d))           labelpos = d;
  if (GetRegistryBool("markers", b))             markers = b;
  LoadMarkers(false);
  if (GetRegistryBool("stars", b))               stars = b;
  if (GetRegistryFloat("starfreq", f))           starfreq = f;
  if (GetRegistryDword("bigstars", d))           bigstars = d;
  if (GetRegistryBool("grid", b))                grid = b;
  if (GetRegistryDword("grid1", d))              grid1 = d;
  if (GetRegistryDword("grid2", d))              grid2 = d;
  if (GetRegistryDword("day", d))                day = d;
  if (GetRegistryDword("night", d))              night = d;
  if (GetRegistryDword("term", d))               term = d;
  if (GetRegistryFloat("gamma", f))              gamma = f;
  if (GetRegistryDword("wait", d))               wait = d;
  if (GetRegistryFloat("timewarp", f))           timewarp = f;
  if (GetRegistryDword("time", d))               time = d;
  if (GetRegistryBool("quakes", b))              quakes = b;
  if (GetRegistryDword("qdelay", d))             qdelay = d;
  if (GetRegistryQword("qupdated", q))           qupdated = q;
  if (GetRegistryDword("disable_rdc", d))           disable_rdc = d;
  if (GetRegistryDword("save_png", d))              save_png = d;
}

void TSettings::Save()
{
  SetRegistryDword("properties.x", properties.x);
  SetRegistryDword("properties.y", properties.y);
  SetRegistryDword("proj", proj);
  SetRegistryDword("pos.type", pos.type);
  SetRegistryFloat("pos.latitude", pos.latitude);
  SetRegistryFloat("pos.longitude", pos.longitude);
  SetRegistryFloat("pos.rlatitude", pos.rlatitude);
  SetRegistryFloat("pos.rlongitude", pos.rlongitude);
  SetRegistryFloat("pos.period", pos.period);
  SetRegistryFloat("pos.inclination", pos.inclination);
  SetRegistryFloat("rot", rot);
  SetRegistryBool("rot.galactic", rot_galactic);
  SetRegistryDword("sunpos.type", sunpos.type);
  SetRegistryFloat("sunpos.latitude", sunpos.latitude);
  SetRegistryFloat("sunpos.longitude", sunpos.longitude);
  SetRegistryFloat("mag", mag);
  SetRegistryDword("size.cx", size.cx);
  SetRegistryDword("size.cy", size.cy);
  SetRegistryDword("shift.x", shift.x);
  SetRegistryDword("shift.y", shift.y);
  SetRegistryBool("shade", shade);
  SetRegistryBool("label", label);
  SetRegistryDword("labelpos", labelpos);
  SetRegistryBool("markers", markers);
  SaveMarkers();
  SetRegistryBool("stars", stars);
  SetRegistryFloat("starfreq", starfreq);
  SetRegistryDword("bigstars", bigstars);
  SetRegistryBool("grid", grid);
  SetRegistryDword("grid1", grid1);
  SetRegistryDword("grid2", grid2);
  SetRegistryDword("day", day);
  SetRegistryDword("night", night);
  SetRegistryDword("term", term);
  SetRegistryFloat("gamma", gamma);
  SetRegistryDword("wait", wait);
  SetRegistryFloat("timewarp", timewarp);
  SetRegistryDword("time", time);
  SetRegistryBool("quakes", quakes);
  SetRegistryDword("qdelay", qdelay);
  SetRegistryQword("qupdated", qupdated);
  SetRegistryBool("disable_rdc", disable_rdc);
  SetRegistryBool("save_png", save_png);
}

void TSettings::LoadMarkers(bool reset)
{
  markerlist.clear();
  HKEY k;
  if (reset || RegOpenKeyEx(HKEY_CURRENT_USER, "Software\\Software Gems\\xearth for Windows\\markers", 0, KEY_ALL_ACCESS, &k) != ERROR_SUCCESS) {
    load_marker_info("built-in");
    for (MarkerInfo *mi = marker_info; mi->label; mi++) {
      marker m;
      m.name = mi->label;
      m.latitude = mi->lat;
      m.longitude = mi->lon;
      markerlist.push_back(m);
    }
    return;
  }
  char valuename[40];
  DWORD valuenamesize;
  DWORD type;
  char value[40];
  DWORD valuesize;
  DWORD i = 0;
  while (true) {
    valuenamesize = sizeof(valuename);
    valuesize = sizeof(value);
    if (RegEnumValue(k, i, valuename, &valuenamesize, NULL, &type, (BYTE *)value, &valuesize) != ERROR_SUCCESS) {
      break;
    }
    if (type == REG_SZ) {
      marker m;
      m.name = valuename;
      if (sscanf(value, "%f,%f", &m.latitude, &m.longitude) == 2) {
        markerlist.push_back(m);
      }
    }
    i++;
  }
  RegCloseKey(k);
}

void TSettings::SaveMarkers()
{
  HKEY k;
  DWORD disp;
  if (RegOpenKeyEx(HKEY_CURRENT_USER, "Software\\Software Gems\\xearth for Windows", 0, KEY_ALL_ACCESS, &k) != ERROR_SUCCESS) {
    return;
  }
  RegDeleteKey(k, "markers");
  RegCloseKey(k);
  if (RegCreateKeyEx(HKEY_CURRENT_USER, "Software\\Software Gems\\xearth for Windows\\markers", 0, NULL, 0, KEY_ALL_ACCESS, NULL, &k, &disp) != ERROR_SUCCESS) {
    return;
  }
  for (std::list<marker>::const_iterator i = markerlist.begin(); i != markerlist.end(); i++) {
    char buf[40];
    sprintf(buf, "%g,%g", i->latitude, i->longitude);
    RegSetValueEx(k, i->name.c_str(), 0, REG_SZ, (BYTE *)buf, strlen(buf)+1);
  }
  RegCloseKey(k);
}

void LoadMarkers()
{
  MarkerInfo *newm;
  static ExtArr info = NULL;

  if (info == NULL)
  {
    /* first time through, allocate a new extarr
     */
    info = extarr_alloc(sizeof(MarkerInfo));
  }
  else
  {
    /* on subsequent passes, just clean it up for reuse
     */
    for (int i=0; i<(info->count-1); i++)
      free(((MarkerInfo *) info->body)[i].label);
    info->count = 0;
  }

  for (std::list<marker>::const_iterator i = Settings.markerlist.begin(); i != Settings.markerlist.end(); i++) {
    newm = (MarkerInfo *)extarr_next(info);
    newm->lat = i->latitude;
    newm->lon = i->longitude;
    newm->label = strdup(i->name.c_str());
    newm->align = 0; //MarkerAlignDefault;
  }

  newm = (MarkerInfo *) extarr_next(info);
  newm->lat   = 0;
  newm->lon   = 0;
  newm->label = NULL;

  marker_info = (MarkerInfo *) info->body;
}

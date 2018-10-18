/*
 * settings.h
 *
 * Copyright (C) 1996 Greg Hewgill
 *
 * Persistent settings support
 *
 */

#ifndef __SETTINGS_H
#define __SETTINGS_H

#include <windows.h>
#include <list>

// Image: proj, rot, rot_galactic, mag, size, shift
// Viewpoint: pos
// Sun: sunpos
// Labels: label, labelpos, markers, markerfile
// Quakes: quakes, qdelay, qupdated
// Dots: stars, starfreq, bigstars, grid, grid1, grid2
// Shading: shade, day, night, term
// Time: timewarp, time
// Display: wait, gamma

struct Position {
  enum Type {_default, fixed, sunrel, orbit, moon, random} type;
  float latitude, longitude;
  float rlatitude, rlongitude;
  float period, inclination;
};

struct marker {
  std::string name;
  float latitude;
  float longitude;
};

class TSettings {
public:
  TSettings();
  void Save();
  void LoadMarkers(bool reset);
  void SaveMarkers();

  POINT properties;
  DWORD proj; // 0=orthographic, 1=mercator
  Position pos;
  float rot;
  BOOL rot_galactic;
  Position sunpos;
  float mag;
  SIZE size;
  POINT shift;
  BOOL shade;
  BOOL label;
  DWORD labelpos;
  BOOL markers;
  //char markerfile[MAX_PATH];
  std::list<marker> markerlist;
  BOOL stars;
  float starfreq;
  DWORD bigstars;
  BOOL grid;
  DWORD grid1;
  DWORD grid2;
  DWORD day;
  DWORD night;
  DWORD term;
  float gamma;
  DWORD wait;
  float timewarp;
  DWORD time;
  BOOL quakes;
  DWORD qdelay;
  unsigned long long qupdated;
//  char font_name[MAX_PATH];
//  DWORD font_size;
  BOOL disable_rdc;
  BOOL save_png;
};

extern TSettings Settings;

#endif

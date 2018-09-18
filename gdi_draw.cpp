/*
 * gdi_draw.cpp
 *
 * Copyright (C) 1998 Greg Hewgill
 *
 * GDI drawing routines for xearth.
 */

#include "xearth.h"

// undo <xearth.h> things that interfere with C++
#undef isupper
#undef _tolower
#undef _P

#include "quake.h"
#include "settings.h"

extern void LoadMarkers();

void draw_outlined_string(HDC dc, COLORREF fg, COLORREF bg, int x, int y, const char *text, int len)
{
  SetBkMode(dc, TRANSPARENT);
  SetTextColor(dc, bg);
  TextOut(dc, x+1, y, text, len);
  TextOut(dc, x-1, y, text, len);
  TextOut(dc, x, y+1, text, len);
  TextOut(dc, x, y-1, text, len);
  SetTextColor(dc, fg);
  TextOut(dc, x, y, text, len);
}


void mark_location(HDC dc, const MarkerInfo *info, COLORREF color, int dia)
{
  int         x, y;
  int         len;
  double      lat, lon;
  double      pos[3];
  char       *text;
  SIZE        extents;
  HGDIOBJ     op;

  lat = info->lat * (M_PI/180);
  lon = info->lon * (M_PI/180);

  pos[0] = sin(lon) * cos(lat);
  pos[1] = sin(lat);
  pos[2] = cos(lon) * cos(lat);

  XFORM_ROTATE(pos, view_pos_info);

  if (proj_type == ProjTypeOrthographic)
  {
    /* if the marker isn't visible, return immediately
     */
    if (pos[2] <= 0) return;
  }
  else if (proj_type == ProjTypeMercator)
  {
    /* apply mercator projection
     */
    pos[0] = MERCATOR_X(pos[0], pos[2]);
    pos[1] = MERCATOR_Y(pos[1]);
  }
  else if (proj_type == ProjTypeCylindrical)
  {
    /* apply mercator projection
     */
    pos[0] = CYLINDRICAL_X(pos[0], pos[2]);
    pos[1] = CYLINDRICAL_Y(pos[1]);
  }
  else
  {
    assert(0);
  }

  x = (int)XPROJECT(pos[0]);
  y = (int)YPROJECT(pos[1]);

  op = SelectObject(dc, GetStockObject(BLACK_PEN));
  Arc(dc, x-(dia+1), y-(dia+1), x+(dia+1), y+(dia+1), 0, 0, 0, 0);
  Arc(dc, x-(dia-1), y-(dia-1), x+(dia-1), y+(dia-1), 0, 0, 0, 0);
  SelectObject(dc, CreatePen(PS_SOLID, 1, color));
  Arc(dc, x-dia, y-dia, x+dia, y+dia, 0, 0, 0, 0);

  text = info->label;
  if (text != NULL)
  {
    len = strlen(text);
    GetTextExtentPoint32(dc, text, len, &extents);

    switch (info->align)
    {
    case MarkerAlignLeft:
      x -= extents.cx + 4;
      y -= extents.cy / 2;
      break;

    case MarkerAlignRight:
    case MarkerAlignDefault:
      x += dia+1;
      y -= extents.cy / 2;
      break;

    case MarkerAlignAbove:
      x -= extents.cx / 2;
      y -= extents.cy + 4;
      break;

    case MarkerAlignBelow:
      x -= extents.cx / 2;
      y += 5;
      break;

    default:
      assert(0);
    }

    draw_outlined_string(dc, color, RGB(0, 0, 0), x, y, text, len);
  }

  DeleteObject(SelectObject(dc, op));
}


void draw_label(HDC dc)
{
  int         dy;
  int         x, y;
  int         len;
  char        buf[128];
  SIZE        extents;
  TEXTMETRIC  tm;
  //SYSTEMTIME  now;

  GetTextMetrics(dc, &tm);

  dy = tm.tmHeight + 1;

  if (labelpos < 2) /* top left or top right */
  {
    y = 5;
    if (hght == GetSystemMetrics(SM_CYSCREEN)) {
      RECT wa;
      SystemParametersInfo(SPI_GETWORKAREA, 0, &wa, 0);
      y += wa.top;
    }
  }
  else
  {
    y = hght - 5;
    y -= 3 * dy;                /* 3 lines of text */
    if (hght == GetSystemMetrics(SM_CYSCREEN)) {
      RECT wa;
      SystemParametersInfo(SPI_GETWORKAREA, 0, &wa, 0);
      y -= GetSystemMetrics(SM_CYSCREEN) - wa.bottom;
    }
  }

  strftime(buf, sizeof(buf), "%d %b %y %H:%M %z", localtime(&current_time));
  len = strlen(buf);
  GetTextExtentPoint32(dc, buf, len, &extents);
  if (labelpos == 0 || labelpos == 2) /* top left or bottom left */
    x = 5;
  else
    x = wdth - 5 - extents.cx;
  draw_outlined_string(dc, RGB(255, 255, 255), RGB(0, 0, 0), x, y, buf, len);
  y += dy;

  sprintf(buf, "view %.1f %c %.1f %c",
          fabs(view_lat), ((view_lat < 0) ? 'S' : 'N'),
          fabs(view_lon), ((view_lon < 0) ? 'W' : 'E'));
  len = strlen(buf);
  GetTextExtentPoint32(dc, buf, len, &extents);
  if (labelpos == 0 || labelpos == 2) /* top left or bottom left */
    x = 5;
  else
    x = wdth - 5 - extents.cx;
  draw_outlined_string(dc, RGB(255, 255, 255), RGB(0, 0, 0), x, y, buf, len);
  y += dy;

  sprintf(buf, "sun %.1f %c %.1f %c",
          fabs(sun_lat), ((sun_lat < 0) ? 'S' : 'N'),
          fabs(sun_lon), ((sun_lon < 0) ? 'W' : 'E'));
  len = strlen(buf);
  GetTextExtentPoint32(dc, buf, len, &extents);
  if (labelpos == 0 || labelpos == 2) /* top left or bottom left */
    x = 5;
  else
    x = wdth - 5 - extents.cx;
  draw_outlined_string(dc, RGB(255, 255, 255), RGB(0, 0, 0), x, y, buf, len);
  y += dy;
}

void draw_quakes(HDC dc)
{
  std::list<quake> quakes = GetQuakes();
  for (std::list<quake>::const_iterator q = quakes.begin(); q != quakes.end(); q++) {
    MarkerInfo mi;
    mi.lat = q->lat;
    mi.lon = q->lon;
    mi.label = strdup("");
    mi.align = 0; //MarkerAlignDefault;
    mark_location(dc, &mi, RGB(255, 255, 0), (int)(exp(q->mag)/30));
  }
}


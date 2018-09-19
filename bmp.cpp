/*
 * bmp.c
 *
 * Copyright (C) 1998 Greg Hewgill
 *
 * Windows BMP output routines for xearth.
 */

#include "xearth.h"

// undo <xearth.h> things that interfere with C++
#undef isupper
#undef _tolower
#undef _P

#include "quake.h"
#include "settings.h"
#include "gdi_draw.h"

extern void LoadMarkers();

extern "C" void bmp_output()
{
  char fn[MAX_PATH];
  HANDLE outf;
  HANDLE outmap;
  HBITMAP bmp;
  struct BitmapHeader *header;

  compute_positions();
  scan_map();
  do_dots();
  GetTempPath(sizeof(fn), fn);
  if (fn[strlen(fn)-1] != '\\') {
    strcat(fn, "\\");
  }
  strcat(fn, "xearth.bmp");
  outf = CreateFile(fn, GENERIC_READ|GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
  if (outf != INVALID_HANDLE_VALUE) {
    outmap = CreateFileMapping(outf, NULL, PAGE_READWRITE, 0, sizeof(struct BitmapHeader)+wdth*hght, NULL);
    if (outmap != NULL) {
      header = (BitmapHeader *)MapViewOfFile(outmap, FILE_MAP_ALL_ACCESS, 0, 0, sizeof(struct BitmapHeader));
      if (header != NULL) {
        bmp_setup(header);
        bmp = CreateDIBSection(NULL, (BITMAPINFO *)&header->bmih, DIB_RGB_COLORS, &BitmapBits, outmap, sizeof(struct BitmapHeader));
        if (bmp != NULL) {
          LoadMarkers();
          render(bmp_row);
          gdi_render(bmp);
          DeleteObject(bmp);
        }
        bmp_cleanup();
        UnmapViewOfFile(header);
      }
      CloseHandle(outmap);
    }
    CloseHandle(outf);
    SystemParametersInfo(SPI_SETDESKWALLPAPER, 0, fn, 0);
  }
}


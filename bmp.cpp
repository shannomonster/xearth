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

static void bmp_setup();
static int  bmp_row(u_char *);
static void bmp_cleanup();

static int bmp_line;

static u16or32 *dith;

#pragma pack(1)
static struct BitmapHeader {
  BITMAPFILEHEADER bfh;
  BITMAPINFOHEADER bmih;
  RGBQUAD cmap[256];
  WORD padding;
} *Header;
#pragma pack()

static void *BitmapBits;

extern "C" void bmp_output()
{
  char fn[MAX_PATH];
  HANDLE outf;
  HANDLE outmap;
  HDC dc;
  HBITMAP bmp;
  HGDIOBJ ob, of;
  MarkerInfo *minfo;

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
      Header = (BitmapHeader *)MapViewOfFile(outmap, FILE_MAP_ALL_ACCESS, 0, 0, sizeof(struct BitmapHeader));
      if (Header != NULL) {
        bmp_setup();
        bmp = CreateDIBSection(NULL, (BITMAPINFO *)&Header->bmih, DIB_RGB_COLORS, &BitmapBits, outmap, sizeof(struct BitmapHeader));
        if (bmp != NULL) {
          LoadMarkers();
          render(bmp_row);
          if (do_markers || do_label || Settings.quakes) {
            dc = CreateCompatibleDC(0);
            ob = SelectObject(dc, bmp);
            of = SelectObject(dc, CreateFont(-8, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, ANSI_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, DEFAULT_QUALITY, DEFAULT_PITCH, "MS Sans Serif"));
            if (do_markers) {
              minfo = marker_info;
              while (minfo->label != NULL)
              {
                mark_location(dc, minfo, RGB(255, 0, 0), 2);
                minfo += 1;
              }
            }
            if (do_label) {
              draw_label(dc);
            }
            if (Settings.quakes) {
              draw_quakes(dc);
            }
            DeleteObject(SelectObject(dc, of));
            SelectObject(dc, ob);
            DeleteDC(dc);
          }
          DeleteObject(bmp);
        }
        bmp_cleanup();
        UnmapViewOfFile(Header);
      }
      CloseHandle(outmap);
    }
    CloseHandle(outf);
    SystemParametersInfo(SPI_SETDESKWALLPAPER, 0, fn, 0);
  }
}


static void bmp_setup()
{
  int i, bmp_header_size;

  if (num_colors > 256)
    fatal("number of colors must be <= 256 with BMP output");

  dither_setup(num_colors);
  dith = (u16or32 *) malloc((unsigned) sizeof(u16or32) * wdth);
  assert(dith != NULL);

  for (i=0; i<dither_ncolors; i++)
  {
    Header->cmap[i].rgbRed   = dither_colormap[i*3+0];
    Header->cmap[i].rgbGreen = dither_colormap[i*3+1];
    Header->cmap[i].rgbBlue  = dither_colormap[i*3+2];
    Header->cmap[i].rgbReserved = 0;
  }
  Header->cmap[i].rgbRed   = 255;
  Header->cmap[i].rgbGreen = 0;
  Header->cmap[i].rgbBlue  = 0;
  Header->cmap[i].rgbReserved = 0;
  i++;
  Header->cmap[i].rgbRed   = 255;
  Header->cmap[i].rgbGreen = 255;
  Header->cmap[i].rgbBlue  = 0;
  Header->cmap[i].rgbReserved = 0;
  i++;

  bmp_header_size = sizeof(BITMAPFILEHEADER)+sizeof(BITMAPINFOHEADER)+256*sizeof(RGBQUAD);

  ZeroMemory(&Header->bfh, sizeof(BITMAPFILEHEADER));
  Header->bfh.bfType = 'MB';
  Header->bfh.bfSize = bmp_header_size+wdth*hght;
  Header->bfh.bfOffBits = bmp_header_size;;
  ZeroMemory(&Header->bmih, sizeof(BITMAPINFOHEADER));
  Header->bmih.biSize = sizeof(BITMAPINFOHEADER);
  Header->bmih.biWidth = wdth;
  Header->bmih.biHeight = hght;
  Header->bmih.biPlanes = 1;
  Header->bmih.biBitCount = 8;
  Header->bmih.biCompression = BI_RGB;
  Header->bmih.biClrUsed = 256;
  Header->bmih.biClrImportant = 256;
  bmp_line = hght - 1;
}


static int bmp_row(u_char *row)
{
  int i;
  u16or32 *tmp;
  u_char *p;

  tmp = dith;
  dither_row(row, tmp);

  p = ((u_char *)BitmapBits) + bmp_line*wdth;

  for (i = 0; i < wdth; i++) {
    *p++ = (u_char)tmp[i];
  }

  bmp_line--;

  return 0;
}


static void bmp_cleanup()
{
  dither_cleanup();
  free(dith);
}

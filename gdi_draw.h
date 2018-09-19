#pragma once
#include <Windows.h>

#pragma pack(1)
struct BitmapHeader {
   BITMAPFILEHEADER bfh;
   BITMAPINFOHEADER bmih;
   RGBQUAD cmap[256];
   WORD padding;
};
#pragma pack()

extern void bmp_setup(struct BitmapHeader *header);
extern int  bmp_row(u_char *);
extern void bmp_cleanup();
extern void gdi_render(HBITMAP bitmap);

extern void *BitmapBits;

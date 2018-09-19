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

#include <atlbase.h>
#include <wincodec.h>

extern void LoadMarkers();
static void save_png(HBITMAP bmp, wchar_t *filenam);

extern "C" void png_output()
{
  wchar_t fn[MAX_PATH + 1];
  HBITMAP bmp;
  struct BitmapHeader header = {};

  compute_positions();
  scan_map();
  do_dots();
  GetTempPathW(MAX_PATH, fn);
  if (fn[wcslen(fn)-1] != L'\\') {
     wcscat(fn, L"\\");
  }
  wcscat(fn, L"xearth.png");

  bmp_setup(&header);
  bmp = CreateDIBSection(NULL, (BITMAPINFO *)&header.bmih, DIB_RGB_COLORS, &BitmapBits, NULL, sizeof(struct BitmapHeader));
  if (bmp != NULL) {
    LoadMarkers();
    render(bmp_row);
    gdi_render(bmp);
    save_png(bmp, fn);
    DeleteObject(bmp);
  }
  bmp_cleanup();
  SystemParametersInfoW(SPI_SETDESKWALLPAPER, 0, fn, 0);
}

void save_png(HBITMAP bmp, wchar_t *filename)
{
   static CComPtr<IWICImagingFactory> pWICFactory;
   HRESULT hr = S_OK;

   if (nullptr == pWICFactory)
   {
      CoInitializeEx(NULL, COINIT_MULTITHREADED);
      hr = pWICFactory.CoCreateInstance(
         CLSID_WICImagingFactory, nullptr, CLSCTX_INPROC_SERVER);
   }

   CComPtr<IWICBitmap> pBitmap;
   if (SUCCEEDED(hr))
   {
      hr = pWICFactory->CreateBitmapFromHBITMAP(
         bmp, NULL, WICBitmapIgnoreAlpha, &pBitmap);
   }

   CComPtr<IWICBitmapEncoder> encoder;
   if (SUCCEEDED(hr))
   {
      hr = pWICFactory->CreateEncoder(
         GUID_ContainerFormatPng, NULL, &encoder);
   }

   CComPtr<IWICStream> pFileStream;
   if (SUCCEEDED(hr))
   {
      hr = pWICFactory->CreateStream(&pFileStream);
   }

   if (SUCCEEDED(hr))
   {
      hr = pFileStream->InitializeFromFilename(filename, GENERIC_WRITE);
   }

   if (SUCCEEDED(hr))
   {
      hr = encoder->Initialize(pFileStream, WICBitmapEncoderNoCache);
   }

   CComPtr<IWICBitmapFrameEncode> pBitmapFrameEncode;
   CComPtr<IPropertyBag2> pPropertyBag;
   if (SUCCEEDED(hr))
   {
      hr = encoder->CreateNewFrame(&pBitmapFrameEncode, &pPropertyBag);
   }

   if (SUCCEEDED(hr))
   {
      hr = pBitmapFrameEncode->Initialize(pPropertyBag);
   }

   if (SUCCEEDED(hr))
   {
      hr = pBitmapFrameEncode->WriteSource(pBitmap, NULL);
   }

   if (SUCCEEDED(hr))
   {
      hr = pBitmapFrameEncode->Commit();
   }

   if (SUCCEEDED(hr))
   {
      hr = encoder->Commit();
   }
}

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
static HRESULT save_png(HBITMAP bmp, wchar_t *filenam);

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

HRESULT save_png(HBITMAP bmp, wchar_t *filename)
{
   struct InitCom {
      InitCom() { CoInitializeEx(NULL, COINIT_MULTITHREADED); }
      ~InitCom() { CoUninitialize(); }
   } initcom;

   CComPtr<IWICImagingFactory> pWICFactory;
   HRESULT hr = pWICFactory.CoCreateInstance(
      CLSID_WICImagingFactory, nullptr, CLSCTX_INPROC_SERVER);
   if (FAILED(hr)) return hr;

   CComPtr<IWICBitmap> pBitmap;
   hr = pWICFactory->CreateBitmapFromHBITMAP(
      bmp, NULL, WICBitmapIgnoreAlpha, &pBitmap);
   if (FAILED(hr)) return hr;

   CComPtr<IWICBitmapEncoder> encoder;
   hr = pWICFactory->CreateEncoder(
      GUID_ContainerFormatPng, NULL, &encoder);
   if (FAILED(hr)) return hr;

   CComPtr<IWICStream> pFileStream;
   hr = pWICFactory->CreateStream(&pFileStream);
   if (FAILED(hr)) return hr;

   hr = pFileStream->InitializeFromFilename(filename, GENERIC_WRITE);
   if (FAILED(hr)) return hr;

   hr = encoder->Initialize(pFileStream, WICBitmapEncoderNoCache);
   if (FAILED(hr)) return hr;

   CComPtr<IWICBitmapFrameEncode> pBitmapFrameEncode;
   CComPtr<IPropertyBag2> pPropertyBag;
   hr = encoder->CreateNewFrame(&pBitmapFrameEncode, &pPropertyBag);
   if (FAILED(hr)) return hr;

   hr = pBitmapFrameEncode->Initialize(pPropertyBag);
   if (FAILED(hr)) return hr;

   hr = pBitmapFrameEncode->WriteSource(pBitmap, NULL);
   if (FAILED(hr)) return hr;

   hr = pBitmapFrameEncode->Commit();
   if (FAILED(hr)) return hr;

   hr = encoder->Commit();
   if (FAILED(hr)) return hr;

   return S_OK;
}

/*
 * subclasswnd.cpp
 *
 * Copyright (C) 1998 Greg Hewgill
 *
 * C++ class to subclass a Windows window
 */

#include <windows.h>
#pragma hdrstop

#include "subclasswnd.h"

SubclassedWindow::SubclassedWindow(HWND hwnd)
 : Hwnd(hwnd)
{
  OldWndProc = (WNDPROC)SetWindowLongPtr(Hwnd, GWL_WNDPROC);
  SetWindowLongPtr(Hwnd, GWL_WNDPROC, (LONG_PTR)StaticWndProc);
  SetWindowLongPtr(Hwnd, GWL_USERDATA, (LONG_PTR)this);
}

SubclassedWindow::~SubclassedWindow()
{
    SetWindowLongPtr(Hwnd, GWL_WNDPROC, (LONG_PTR)OldWndProc);
}

LRESULT CALLBACK SubclassedWindow::StaticWndProc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam)
{
  SubclassedWindow *This = reinterpret_cast<SubclassedWindow *>(GetWindowLongPtr(hwnd, GWL_USERDATA));
  LRESULT r = This->WndProc(hwnd, msg, wparam, lparam);
  if (msg == WM_NCDESTROY) { // last message this window will get
      SetWindowLongPtr(This->Hwnd, GWL_WNDPROC, (LONG_PTR)This->OldWndProc);
    delete This;
  }
  return r;
}

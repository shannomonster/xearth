/*
 * xearthwin.cpp
 *
 * Copyright (C) 1998 Greg Hewgill
 *
 * Main xearth for Windows module.
 */

#include <windows.h>
#include <windowsx.h>
#include <commctrl.h>
#include <stdio.h>
#include <setjmp.h>
#pragma hdrstop

#include "properties.h"
#include "quake.h"
#include "settings.h"
#include "subclasswnd.h"

#include "resource.h"

const char *DefaultRegistryKey = "Software\\Software Gems\\xearth for Windows";

const int ID_TASKBARICON = 3033;
const int WM_TASKBARICON = WM_USER+1021;

const int CM_POPUPMENU  = 1000;
const int CM_REFRESH    = 1001;
const int CM_CLOSE      = 1002;
const int CM_ABOUT      = 1003;
const int CM_PROPERTIES = 1004;

HANDLE TerminateEvent;
HANDLE RefreshEvent;
DWORD LastRefresh;

HINSTANCE g_hInstance;
HWND MainWindow;

bool InProperties = false;

extern int main(int argc, char **argv);
jmp_buf fataljmp;

bool UpdateOk()
{
  HWND fg = GetForegroundWindow();
  char buf[80];
  GetWindowText(fg, buf, sizeof(buf));
  if (stricmp(buf, "Program Manager") == 0) {
    return true;
  }
  RECT wr;
  GetWindowRect(fg, &wr);
  RECT wa;
  SystemParametersInfo(SPI_GETWORKAREA, 0, &wa, 0);
  return (wr.right-wr.left)*(wr.bottom-wr.top) < (wa.right-wa.left)*(wa.bottom-wa.top);
}

char **tokenize(char *s, int *argc_ret)
{
  *argc_ret = 0;
  char **r = NULL;
  char *p = strtok(s, " ");
  while (p) {
    int newargc = *argc_ret + 1;
    r = (char **)realloc(r, newargc*sizeof(char *));
    r[*argc_ret] = p;
    *argc_ret = newargc;
    p = strtok(NULL, " ");
  }
  return r;
}

DWORD CALLBACK DoRefresh(void *)
{
  SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_IDLE);
  HANDLE a[2];
  a[0] = TerminateEvent;
  a[1] = RefreshEvent;
  while (true) {
    DWORD r = WaitForMultipleObjects(2, a, FALSE, INFINITE);
    if (r == WAIT_OBJECT_0) {
      break;
    }
    r = WaitForSingleObject(TerminateEvent, 100);
    if (r == WAIT_OBJECT_0) {
      break;
    }
    ResetEvent(RefreshEvent);
    if (!UpdateOk()) {
      continue;
    }
    char cmdline[1024];
    sprintf(cmdline, "xearth");
    strcat(cmdline, Settings.save_png ? " -png " : " -bmp ");
    strcat(cmdline, " -proj ");
    switch (Settings.proj) {
      case 0: strcat(cmdline, "orth"); break;
      case 1: strcat(cmdline, "merc"); break;
      case 2: strcat(cmdline, "cyl");  break;
      default: strcat(cmdline, "orth"); break;
    }
    if (Settings.pos.type != Position::_default) {
      strcat(cmdline, " -pos");
      switch (Settings.pos.type) {
        case Position::fixed:
          sprintf(&cmdline[strlen(cmdline)], " fixed,%f,%f", Settings.pos.latitude, Settings.pos.longitude);
          break;
        case Position::sunrel:
          sprintf(&cmdline[strlen(cmdline)], " sunrel,%f,%f", Settings.pos.rlatitude, Settings.pos.rlongitude);
          break;
        case Position::orbit:
          sprintf(&cmdline[strlen(cmdline)], " orbit,%f,%f", Settings.pos.period, Settings.pos.inclination);
          break;
        case Position::moon:
          strcat(cmdline, " moon");
          break;
        case Position::random:
          strcat(cmdline, " random");
          break;
      }
    }
    if (Settings.rot_galactic && Settings.proj == 0) {
      strcat(cmdline, " -rot galactic");
    } else if (Settings.rot != 0) {
      sprintf(&cmdline[strlen(cmdline)], " -rot %f", Settings.rot);
    }
    if (Settings.sunpos.type != Position::_default) {
      strcat(cmdline, " -sunpos");
      switch (Settings.sunpos.type) {
        case Position::fixed:
          sprintf(&cmdline[strlen(cmdline)], " %f,%f", Settings.sunpos.latitude, Settings.sunpos.longitude);
          break;
      }
    }
    if (Settings.mag != 1) {
      sprintf(&cmdline[strlen(cmdline)], " -mag %f", Settings.mag);
    }
    strcat(cmdline, " -size");
    if (Settings.size.cx == 0 || Settings.size.cy == 0) {
      sprintf(&cmdline[strlen(cmdline)], " %d,%d", GetSystemMetrics(SM_CXSCREEN), GetSystemMetrics(SM_CYSCREEN));
    } else {
      sprintf(&cmdline[strlen(cmdline)], " %d,%d", Settings.size.cx, Settings.size.cy);
    }
    if (Settings.shift.x != 0 || Settings.shift.y != 0) {
      sprintf(&cmdline[strlen(cmdline)], " -shift %d,%d", Settings.shift.x, Settings.shift.y);
    }
    strcat(cmdline, Settings.shade ? " -shade" : " -noshade");
    strcat(cmdline, Settings.label ? " -label" : " -nolabel");
    if (Settings.label) {
      sprintf(&cmdline[strlen(cmdline)], " -labelpos %d", Settings.labelpos);
    }
    strcat(cmdline, Settings.markers ? " -markers" : " -nomarkers");
    // markerfile
    strcat(cmdline, Settings.stars ? " -stars" : " -nostars");
    if (Settings.stars) {
      sprintf(&cmdline[strlen(cmdline)], " -starfreq %f", Settings.starfreq);
      if (Settings.bigstars > 0) {
        sprintf(&cmdline[strlen(cmdline)], " -bigstars %d", Settings.bigstars);
      }
    }
    strcat(cmdline, Settings.grid ? " -grid" : " -nogrid");
    if (Settings.grid) {
      sprintf(&cmdline[strlen(cmdline)], " -grid1 %d", Settings.grid1);
      sprintf(&cmdline[strlen(cmdline)], " -grid2 %d", Settings.grid2);
    }
    if (Settings.shade) {
      sprintf(&cmdline[strlen(cmdline)], " -day %d", Settings.day);
      sprintf(&cmdline[strlen(cmdline)], " -night %d", Settings.night);
      sprintf(&cmdline[strlen(cmdline)], " -term %d", Settings.term);
    }
    sprintf(&cmdline[strlen(cmdline)], " -gamma %f", Settings.gamma);
    if (Settings.timewarp != 1) {
      sprintf(&cmdline[strlen(cmdline)], " -timewarp %f", Settings.timewarp);
    }
    if (Settings.time) {
      sprintf(&cmdline[strlen(cmdline)], " -time %d", Settings.time);
    }
    int argc;
    char **argv = tokenize(cmdline, &argc);
    int err = setjmp(fataljmp);
    if (err == 0) {
      main(argc, argv);
    } else {
      const char *msg = reinterpret_cast<const char *>(err);
    }
    free(argv);
  }
  return 0;
}

void Refresh()
{
  SetEvent(RefreshEvent);
  if (Settings.wait > 0) {
    LastRefresh = GetTickCount() / (Settings.wait*60*1000);
  }
}

class StaticHyperlink: public SubclassedWindow {
public:
  StaticHyperlink(HWND hwnd);
  virtual ~StaticHyperlink();
  HBRUSH OnCtlColorStatic(HWND parent, HDC dc, HWND control, int type);
  void OnLButtonDown(HWND hwnd, BOOL doubleclick, int x, int y, UINT flags);
  UINT OnNcHitTest(HWND hwnd, int x, int y);
  BOOL OnSetCursor(HWND hwnd, HWND hwndCursor, UINT codeHitTest, UINT msg);
private:
  HFONT font;
  HCURSOR finger;
  COLORREF color;
  virtual LRESULT WndProc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam);
};

StaticHyperlink::StaticHyperlink(HWND hwnd)
 : SubclassedWindow(hwnd)
{
  font = NULL;
  finger = LoadCursor(g_hInstance, MAKEINTRESOURCE(IDC_FINGER));
  color = RGB(0, 0, 255);
}

StaticHyperlink::~StaticHyperlink()
{
  if (font) {
    DeleteObject(font);
  }
}

HBRUSH StaticHyperlink::OnCtlColorStatic(HWND parent, HDC dc, HWND control, int type)
{
  HBRUSH hbr = NULL;
  if ((GetWindowLong(Hwnd, GWL_STYLE) & 0xFF) <= SS_RIGHT) {
    if (font == NULL) {
      LOGFONT lf;
      GetObject((HFONT)SendMessage(Hwnd, WM_GETFONT, 0, 0), sizeof(lf), &lf);
      lf.lfUnderline = TRUE;
      font = CreateFontIndirect(&lf);
    }
    SelectObject(dc, font);
    SetTextColor(dc, color);
    SetBkMode(dc, TRANSPARENT);
    hbr = (HBRUSH)GetStockObject(HOLLOW_BRUSH);
  }
  return hbr;
}

void StaticHyperlink::OnLButtonDown(HWND hwnd, BOOL doubleclick, int x, int y, UINT flags)
{
  char buf[256];
  GetWindowText(Hwnd, buf, sizeof(buf));
  HCURSOR cur = GetCursor();
  SetCursor(LoadCursor(NULL, IDC_WAIT));
  HINSTANCE r = ShellExecute(NULL, "open", buf, NULL, NULL, SW_SHOWNORMAL);
  SetCursor(cur);
  if ((UINT)r > 32) {
    color = RGB(128, 0, 128);
    InvalidateRect(Hwnd, NULL, FALSE);
  }
}

UINT StaticHyperlink::OnNcHitTest(HWND hwnd, int x, int y)
{
  return HTCLIENT;
}

BOOL StaticHyperlink::OnSetCursor(HWND hwnd, HWND hwndCursor, UINT codeHitTest, UINT msg)
{
  SetCursor(finger);
  return TRUE;
}

LRESULT StaticHyperlink::WndProc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam)
{
  switch (msg) {
    case WM_CTLCOLORSTATIC:
      return HANDLE_WM_CTLCOLORSTATIC(hwnd, wparam, lparam, OnCtlColorStatic);
    case WM_LBUTTONDOWN:
      return HANDLE_WM_LBUTTONDOWN(hwnd, wparam, lparam, OnLButtonDown);
    case WM_NCHITTEST:
      return HANDLE_WM_NCHITTEST(hwnd, wparam, lparam, OnNcHitTest);
    case WM_SETCURSOR:
      return HANDLE_WM_SETCURSOR(hwnd, wparam, lparam, OnSetCursor);
    default:
      return CallWindowProc(OldWndProc, hwnd, msg, wparam, lparam);
  }
  return 0;
}

BOOL CALLBACK AboutDlgProc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam)
{
  switch (msg) {
    case WM_COMMAND:
      switch (LOWORD(wparam)) {
        case IDOK:
        case IDCANCEL:
          EndDialog(hwnd, LOWORD(wparam));
          break;
      }
      break;
    case WM_CTLCOLORSTATIC:
      return SendMessage((HWND)lparam, msg, wparam, lparam);
    case WM_INITDIALOG:
      new StaticHyperlink(GetDlgItem(hwnd, IDC_HOMEPAGE));
	  SetDlgItemText(hwnd, IDC_ABOUT_DATE, "(" __DATE__ ")");
      break;
    default:
      return FALSE;
  }
  return TRUE;
}

LRESULT CALLBACK XearthProc(HWND w, UINT msg, WPARAM wparam, LPARAM lparam)
{
  switch (msg) {
    case WM_COMMAND:
      switch (wparam) {
        case CM_POPUPMENU: {
          SetForegroundWindow(w);
          SetCapture(w);
          HMENU m = CreatePopupMenu();
          AppendMenu(m, MF_STRING, CM_REFRESH, "&Refresh");
          AppendMenu(m, MF_STRING, CM_CLOSE, "&Close");
          AppendMenu(m, MF_STRING, CM_ABOUT, "&About");
          AppendMenu(m, MF_SEPARATOR, 0, NULL);
          AppendMenu(m, MF_STRING, CM_PROPERTIES, "&Properties");
          MENUITEMINFO mii;
          ZeroMemory(&mii, sizeof(mii));
          mii.cbSize = sizeof(mii);
          mii.fMask = MIIM_STATE;
          mii.fState = MFS_DEFAULT;
          SetMenuItemInfo(m, CM_PROPERTIES, FALSE, &mii);
          POINT p;
          GetCursorPos(&p);
          TrackPopupMenu(m, TPM_LEFTALIGN|TPM_RIGHTBUTTON, p.x, p.y, 0, w, NULL);
          ReleaseCapture();
          break;
        }
        case CM_REFRESH:
          Refresh();
          break;
        case CM_CLOSE:
          DestroyWindow(w);
          break;
        case CM_ABOUT:
          DialogBox(GetModuleHandle(NULL), MAKEINTRESOURCE(IDD_ABOUT), w, AboutDlgProc);
          break;
        case CM_PROPERTIES:
          SetForegroundWindow(w);
          if (!InProperties) {
            InProperties = true;
            Properties(w);
            InProperties = false;
          }
          break;
      }
      break;
    case WM_CREATE:
      NOTIFYICONDATA nid;
      nid.cbSize = sizeof(nid);
      nid.hWnd = w;
      nid.uID = ID_TASKBARICON;
      nid.uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP;
      nid.uCallbackMessage = WM_TASKBARICON;
      nid.hIcon = (HICON)LoadImage(g_hInstance, MAKEINTRESOURCE(IDI_EARTH), IMAGE_ICON, 16, 16, 0);
      strcpy(nid.szTip, "xearth");
      Shell_NotifyIcon(NIM_ADD, &nid);
      SetTimer(w, 1, 1000, NULL);
      break;
    case WM_DESTROY:
      nid.cbSize = sizeof(nid);
      nid.hWnd = w;
      nid.uID = ID_TASKBARICON;
      Shell_NotifyIcon(NIM_DELETE, &nid);
      KillTimer(w, 1);
      PostQuitMessage(0);
      break;
    case WM_HOTKEY:
      if (wparam == ID_TASKBARICON) {
        PostMessage(w, WM_COMMAND, CM_PROPERTIES, 0);
      }
      break;
    case WM_TASKBARICON:
      if (lparam == WM_LBUTTONDBLCLK) {
        PostMessage(w, WM_COMMAND, CM_PROPERTIES, 0);
      } else if (lparam == WM_RBUTTONDOWN) {
        PostMessage(w, WM_COMMAND, CM_POPUPMENU, 0);
      }
      break;
    case WM_TIMER:
      if ((!Settings.disable_rdc || !GetSystemMetrics(SM_REMOTESESSION)) &&
          Settings.wait > 0 && GetTickCount() / (Settings.wait*60*1000) != LastRefresh) {
        Refresh();
      }
      break;
    default:
      return DefWindowProc(w, msg, wparam, lparam);
  }
  return 0;
}

int CALLBACK WinMain(HINSTANCE hInstance, HINSTANCE, LPTSTR lpszCmdLine, int nCmdShow)
{
  g_hInstance = hInstance;
  WNDCLASS wc;
  ZeroMemory(&wc, sizeof(wc));
  wc.lpfnWndProc = XearthProc;
  wc.hInstance = hInstance;
  wc.hIcon = LoadIcon(g_hInstance, MAKEINTRESOURCE(IDI_EARTH));
  wc.lpszClassName = "XearthClass";
  RegisterClass(&wc);
  MainWindow = CreateWindow(wc.lpszClassName, "Xearth", WS_POPUP, 0, 0, 0, 0, NULL, NULL, hInstance, NULL);
  //RegisterHotKey(MainWindow, ID_TASKBARICON, MOD_ALT|MOD_CONTROL, 'X');
  TerminateEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
  RefreshEvent = CreateEvent(NULL, FALSE, FALSE, NULL);
  StartQuakeThread();
  DWORD tid;
  HANDLE RefreshThread = CreateThread(NULL, 0, DoRefresh, NULL, 0, &tid);
  if (!Settings.disable_rdc || !GetSystemMetrics(SM_REMOTESESSION)) Refresh();
  MSG msg;
  while (GetMessage(&msg, 0, 0, 0)) {
    TranslateMessage(&msg);
    DispatchMessage(&msg);
  }
  SetEvent(TerminateEvent);
  WaitForSingleObject(RefreshThread, INFINITE);
  CloseHandle(RefreshThread);
  //UnregisterHotKey(MainWindow, ID_TASKBARICON);
  HKEY k;
  if (RegOpenKeyEx(HKEY_CURRENT_USER, "Control Panel\\Desktop", 0, KEY_READ, &k) == ERROR_SUCCESS) {
    DWORD type;
    char fn[MAX_PATH];
    DWORD n = sizeof(fn);
    if (RegQueryValueEx(k, "Wallpaper", NULL, &type, (BYTE *)fn, &n) == ERROR_SUCCESS) {
      SystemParametersInfo(SPI_SETDESKWALLPAPER, 0, fn, 0);
    }
    RegCloseKey(k);
  }
  return 0;
}

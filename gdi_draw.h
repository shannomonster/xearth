#pragma once

extern void draw_outlined_string(HDC dc, COLORREF fg, COLORREF bg, int x, int y, const char *text, int len);
extern void mark_location(HDC dc, const MarkerInfo *info, COLORREF color, int dia);
extern void draw_label(HDC dc);
extern void draw_quakes(HDC dc);

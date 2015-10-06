' little X11-Basic tool to create the mapdata.dat file out of the 
' original mapdata.c from the original sources.
' by Markus Hoffmann 
' 
open "I",#1,"mapdata.c"
open "O",#2,"assets/mapdata.dat"
while not eof(#1)
  lineinput #1,t$
  t$=trim$(t$)
  if len(t$)
    if t$="short map_data[] = {"
      st=1
    else if t$="};"
      st=0
    else if st=1
      if left$(t$,2)="/*"
      else
        @process(t$)
        ' print t$
      endif
    else
      ' print t$
    endif
  endif
wend
quit

procedure process(t$)
  while len(t$)
    split t$,",",0,a$,t$
    a$=trim$(a$)
    a=val(a$)
    print #2,mki$(a);
  wend
return


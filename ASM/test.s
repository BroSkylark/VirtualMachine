.equ A r12

%%macro sqr:1
{
jmp ::0
ld A,%0
::0
mul A,%0
}

jmp $+6+3*%sqr:1
ld r0,4
sqr r0
ld r1,8
sqr r1
sqr A
hlt


#ifndef __INCLUDE_H
#define __INCLUDE_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "evaluate.h"

typedef unsigned char  BYTE;
typedef unsigned short WORD;
typedef unsigned short ADDR;

char *readFile(const char *, int *);
const char *findLast(const char *, char);
void skipW(char **);
void rmvW(char *);
char *strdupv(const char *, char);
int replaceC(char *, char, char);
void toLowerCase(char *);
void toUpperCase(char *);
void strcpyv(char *, char *, char, int);
WORD evaluateString(const char *);
void appendC(char **, char);
char **split(const char *, char, int *);

#endif


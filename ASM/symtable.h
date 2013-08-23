#ifndef __SYMTABLE_H
#define __SYMTABLE_H

#include "include.h"
#include "WORD_ARRAY.h"

typedef struct __symbol_table
{
	char **ids;
	char **vals;
	int c, lblc, localFlag;
} SYM_TBL;

void  SYM_TBL_init(SYM_TBL*);
void  SYM_TBL_addSymbol(SYM_TBL*, const char *, const char *);
const char *SYM_TBL_dereference(SYM_TBL*, const char *);
char *SYM_TBL_solveAllSymbols(SYM_TBL*, const char *);
int SYM_TBL_getPos(SYM_TBL*, const char *);
void SYM_TBL_deleteLocalLabels(SYM_TBL*);
void SYM_TBL_copy(SYM_TBL*, SYM_TBL *);
void  SYM_TBL_dispose(SYM_TBL*);

#endif


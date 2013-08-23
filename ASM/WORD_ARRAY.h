#ifndef __WORD_ARRAY
#define __WORD_ARRAY

#include "include.h"

typedef struct __word_array
{
	WORD *data;
	int c, i;
} WORD_ARRAY;

void WORD_ARRAY_init(WORD_ARRAY*);
void WORD_ARRAY_set(WORD_ARRAY*, int, WORD);
void WORD_ARRAY_push(WORD_ARRAY*, WORD);
WORD WORD_ARRAY_peek(WORD_ARRAY*);
WORD WORD_ARRAY_poll(WORD_ARRAY*);
int  WORD_ARRAY_getPos(WORD_ARRAY*);
void WORD_ARRAY_dispose(WORD_ARRAY*);
void WORD_ARRAY_merge(WORD_ARRAY*, WORD_ARRAY *);
void WORD_ARRAY_write(WORD_ARRAY*, char *);
WORD_ARRAY WORD_ARRAY_restore(char *, int);

#endif


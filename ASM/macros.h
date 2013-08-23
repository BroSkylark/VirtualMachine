#ifndef __MACROS_H
#define __MACROS_H

#include "include.h"

typedef struct __macro
{
	char *name;
	int argc, id, size;
	char *body;
} MAC;

typedef struct __macro_array
{
	struct __macro *macros;
	int c;
} M_A;

void M_A_init(M_A*);
void M_A_push(M_A*, MAC);
void M_A_copy(M_A*, M_A *);
void M_A_dispose(M_A*);

void MAC_init(MAC*);
void MAC_set(MAC*, const char *, int, const char *, int);
void MAC_copy(MAC*, MAC *);
void MAC_dispose(MAC*);

#endif


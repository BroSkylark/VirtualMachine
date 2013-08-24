#ifndef __TOKEN_H
#define __TOKEN_H

#include "include.h"

#define T_SYM 0x00
#define T_STR 0x01
#define T_CHR 0x02

typedef struct __token
{
	int type;
	void *data;
} TOK;

typedef struct __token_list
{
	struct __token *token;
	int c;
} T_L;

void T_L_init(T_L*);
void T_L_add(T_L*, TOK);
void T_L_dispose(T_L*);

#endif


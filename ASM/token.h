#ifndef __TOKEN_H
#define __TOKEN_H

#include "include.h"
#include "WORD_ARRAY.h"

#define EOS '\0'
#define ID 0x00
#define ID_META 0x01
#define ID_LBL 0x02
#define ID_INS 0x03

#define SYM 0x04

#define META_ORG 0x05
#define META_DW 0x06

#define NIL 0x07

typedef struct __token_symbol
{
	char *data;
} T_SYM;

typedef struct __token_id
{
	void *token;
	WORD id;
} T_ID;

typedef struct __token_id_meta
{
	void *data;
	WORD meta;
} T_ID_META;

typedef struct __token_id_label
{
	WORD id;
} T_ID_LBL;

typedef struct __token_id_instruction
{
	T_SYM name;
	T_SYM *arguments;
	int argc;
} T_ID_INS;

void T_writeToken(T_ID*, WORD_ARRAY *);
void T_readToken(T_ID*, WORD_ARRAY *);
void T_disposeToken(T_ID*);

void T_ID_copy(T_ID*, T_ID *);

#endif


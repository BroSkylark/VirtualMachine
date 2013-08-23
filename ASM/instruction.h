#ifndef __INSTRUCTION_H
#define __INSTRUCTION_H

#include "include.h"

#define ARG_NONE     0
#define ARG_REGISTER 1
#define ARG_MEMORY   2
#define ARG_CONST    3

typedef struct __instruction
{
	char *name;
	int *arg;
	WORD id;
} INSTR;

typedef struct __instruction_array
{
	struct __instruction *instructions;
	int c;
} INS_ARR;

INS_ARR readInstructions(const char*);
void disposeInstructions(INS_ARR*);
INSTR INSTR_read(char *);
void INSTR_dispose(INSTR*);

#endif


#ifndef __EVALUATE_H
#define __EVALUATE_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int evaluate(const char *);

#ifdef __EVALUATE_H_C

#define E_PUSH  0
#define E_ADD   1
#define E_SUB   2
#define E_MUL   3
#define E_DIV   4
#define E_MOD   5
#define E_AND   6
#define E_OR    7
#define E_XOR   8
#define E_NEG   9
#define E_NOT  10

typedef struct __int_stack
{
	int *v;
	int c, i;
} I_S;

void I_S_init(I_S*);
void I_S_push(I_S*, int);
int I_S_pop(I_S*);
int I_S_poll(I_S*);
void I_S_resize(I_S*);
void I_S_dispose(I_S*);

void evalPM(I_S *, char **);
void evalMDM(I_S *, char **);
void evalAOX(I_S *, char **);
void evalU(I_S *, char **);
void eval(I_S *, char **);

void clean(char *);

#endif

#endif


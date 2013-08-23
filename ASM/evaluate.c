#define __EVALUATE_H_C
#include "evaluate.h"

int evaluate(const char *input)
{
	char *e = strdup(input);
	char *ptr = e;
	clean(e);
	
	I_S a;
	I_S is;
	
	I_S_init(&a);
	I_S_init(&is);
	
	evalPM(&a, &e);
	
	int t;
	while(a.i > 0)
	{
		switch(I_S_poll(&a))
		{
			case E_PUSH:
				t = I_S_poll(&a);
				I_S_push(&is, t);
//				fprintf(stderr, "> Constant %d\n", t);
				break;
			case E_ADD:
				t = I_S_pop(&is);
				t += I_S_pop(&is);
				I_S_push(&is, t);
//				fprintf(stderr, "> Add\n");
				break;
			case E_SUB:
				t = I_S_pop(&is);
				t = I_S_pop(&is) - t;
				I_S_push(&is, t);
//				fprintf(stderr, "> Sub\n");
				break;
			case E_MUL:
				t = I_S_pop(&is);
				t *= I_S_pop(&is);
				I_S_push(&is, t);
//				fprintf(stderr, "> Mul\n");
				break;
			case E_DIV:
				t = I_S_pop(&is);
				t = I_S_pop(&is) / t;
				I_S_push(&is, t);
//				fprintf(stderr, "> Div\n");
				break;
			case E_MOD:
				t = I_S_pop(&is);
				t = I_S_pop(&is) % t;
				I_S_push(&is, t);
//				fprintf(stderr, "> Mod\n");
				break;
			case E_AND:
				t = I_S_pop(&is);
				t &= I_S_pop(&is);
				I_S_push(&is, t);
//				fprintf(stderr, "> And\n");
				break;
			case E_OR:
				t = I_S_pop(&is);
				t |= I_S_pop(&is);
				I_S_push(&is, t);
//				fprintf(stderr, "> Or\n");
				break;
			case E_XOR:
				t = I_S_pop(&is);
				t ^= I_S_pop(&is);
				I_S_push(&is, t);
//				fprintf(stderr, "> Xor\n");
				break;
			case E_NEG:
				t = I_S_pop(&is);
				I_S_push(&is, -t);
//				fprintf(stderr, "> Neg\n");
				break;
			case E_NOT:
				t = I_S_pop(&is);
				I_S_push(&is, ~t);
//				fprintf(stderr, "> Not\n");
				break;
		}
	}
	
	int r = I_S_pop(&is);
	
	if(is.i != 0)
	{
		fprintf(stderr, "ERR: Still %d number remaining in stack after evaluation!\nAbort.\n", is.i);
		exit(1);
	}
	
	I_S_dispose(&is);
	I_S_dispose(&a);
	free(ptr);
	
//	fprintf(stderr, ">>> Evaluating: '%s' == %d\n", input, r);
	
	return r;
}

// # ==========================================================================

int inline isBadC(char c) { return (c == ' ' || c == '\t' || c == '\n') ? 1 : 0; }
int inline isDigit(char c) { return ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || c == 'x' || c == 'X') ? 1 : 0; }

void evalPM(I_S *is, char **in)
{
	evalMDM(is, in);
	
	while(**in == '+' || **in == '-')
	{
		switch(**in)
		{
			case '+':
				(*in)++;
				evalMDM(is, in);
				I_S_push(is, E_ADD);
				break;
			case '-':
				(*in)++;
				evalMDM(is, in);
				I_S_push(is, E_SUB);
				break;
		}
	}
}

void evalMDM(I_S *is, char **in)
{
	evalAOX(is, in);
	
	while(**in == '*' || **in == '/' || **in == '%')
	{
		switch(**in)
		{
			case '*':
				(*in)++;
				evalAOX(is, in);
				I_S_push(is, E_MUL);
				break;
			case '/':
				(*in)++;
				evalAOX(is, in);
				I_S_push(is, E_DIV);
				break;
			case '%':
				(*in)++;
				evalAOX(is, in);
				I_S_push(is, E_MOD);
				break;
		}
	}
}

void evalAOX(I_S *is, char **in)
{
	evalU(is, in);
	
	while(**in == '&' || **in == '|' || **in == '^')
	{
		switch(**in)
		{
			case '&':
				(*in)++;
				evalU(is, in);
				I_S_push(is, E_AND);
				break;
			case '|':
				(*in)++;
				evalU(is, in);
				I_S_push(is, E_OR);
				break;
			case '^':
				(*in)++;
				evalU(is, in);
				I_S_push(is, E_XOR);
				break;
		}
	}
}

void evalU(I_S *is, char **in)
{
	switch(**in)
	{
		case '-':
			(*in)++;
			eval(is, in);
			I_S_push(is, E_NEG);
			break;
		case '~':
		case '!':
			(*in)++;
			eval(is, in);
			I_S_push(is, E_NOT);
			break;
		case '+':
			(*in)++;
			eval(is, in);
			break;
		default:
			eval(is, in);
			break;
	}
}

void eval(I_S *is, char **in)
{
	if(**in == '(')
	{
		(*in)++;
		evalPM(is, in);
	}
	else
	{
		int v;
		sscanf(*in, "%i", &v);
		while(isDigit(**in)) (*in)++;
		I_S_push(is, E_PUSH);
		I_S_push(is, v);
	}
}

void clean(char *src)
{
	int i, l = strlen(src);
	for(i = 0 ; i < l ; i++)
	{
		while(isBadC(src[i]))
		{
			memmove(src + i, src + i + 1, l-- - i);
		}
	}
}

// # --------------------------------------------------------------------------

void I_S_init(I_S *this)
{
	this->c = 0;
	this->i = 0;
	this->v = NULL;
}

void I_S_push(I_S *this, int v)
{
	while(this->i >= this->c)
	{
		I_S_resize(this);
	}
	
	this->v[this->i++] = v;
}

int I_S_pop(I_S *this)
{
	if(this->i == 0)
	{
		fprintf(stderr, "ERR: Tried to pop from empty int stack.\nAbort.\n");
		exit(1);
	}
	
	return this->v[--this->i];
}

int I_S_poll(I_S *this)
{
	if(this->i == 0)
	{
		fprintf(stderr, "ERR: Tried to poll from empty int stack.\nAbort.\n");
		exit(1);
	}
	
	int r = this->v[0];
	if(--this->i > 0)
	{
		memmove(this->v, this->v + 1, this->i * sizeof(int));
	}
	
	return r;
}

void I_S_resize(I_S *this)
{
	this->c = ((this->c == 0) ? 0x10 : (this->c << 1));
	this->v = realloc(this->v, this->c * sizeof(int));
}

void I_S_dispose(I_S *this)
{
	free(this->v);
	
	I_S_init(this);
}


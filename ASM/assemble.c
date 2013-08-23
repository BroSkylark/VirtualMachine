#include "assemble.h"

typedef struct __assembler
{
	T_ID *token;
	INS_ARR ins;
	WORD_ARRAY result;
	SYM_TBL symbolTable;
	int tc;
	WORD org;
} ASM;

void assembleMeta(ASM*, T_ID_META *);
void assembleLabel(ASM*, T_ID_LBL *);
void assembleInstr(ASM*, T_ID_INS *);

void ASM_init(ASM*, WORD_ARRAY *);
WORD_ARRAY ASM_dispose(ASM*);
WORD_ARRAY *tryToFollowInstruction(INSTR *, T_ID_INS *, SYM_TBL *);

WORD_ARRAY assemble(WORD_ARRAY src)
{
	ASM assm;
	ASM_init(&assm, &src);
	
	int i, j;
	for(j = 0 ; j < 2 ; j++)
	{
		for(i = 0 ; i < assm.tc ; i++)
		{
			if(j == 0)
			{
				T_ID_META *tok = (T_ID_META *) assm.token[i].token;
				switch(assm.token[i].id)
				{
					case ID_META:
						switch(tok->meta)
						{
							case META_ORG: assm.org = *((WORD *) tok->data); break;
							case META_DW:  assm.org += ((WORD_ARRAY *) tok->data)->i; break;
						}
						break;
					case ID_LBL:
						assembleLabel(&assm, (T_ID_LBL *) assm.token[i].token);
						break;
					case ID_INS:
						assm.org += 1 + ((T_ID_INS *) assm.token[i].token)->argc;
						break;
				}
			}
			else
			{
				switch(assm.token[i].id)
				{
					case ID_META: assembleMeta(&assm, (T_ID_META *) assm.token[i].token); break;
					case ID_INS:  assembleInstr(&assm, (T_ID_INS *) assm.token[i].token); break;
				}
			}
		}
	}
	
	return ASM_dispose(&assm);
}

void assembleMeta(ASM *this, T_ID_META *tok)
{
	switch(tok->meta)
	{
		case META_ORG:
			break;
		case META_DW:
			WORD_ARRAY_merge(&this->result, (WORD_ARRAY *) tok->data);
			break;
	}
}

void assembleLabel(ASM *this, T_ID_LBL *tok)
{
	char lbl[8], pos[8];
	sprintf(lbl, "#%d", tok->id);
	sprintf(pos, "%d", this->org);
	SYM_TBL_addSymbol(&this->symbolTable, lbl, pos);
}

void assembleInstr(ASM *this, T_ID_INS *tok)
{
	int i;
	for(i = 0 ; i < this->ins.c ; i++)
	{
		if(strcmp(tok->name.data, this->ins.instructions[i].name) == 0)
		{
			WORD_ARRAY *arr = tryToFollowInstruction(&this->ins.instructions[i], tok, &this->symbolTable);
			
			if(arr != NULL)
			{
				WORD_ARRAY_merge(&this->result, arr);
				WORD_ARRAY_dispose(arr);
				free(arr);
				
				return;
			}
		}
	}
	
	fprintf(stderr, "ERR: Couldn't assemble instruction '%s'!\nAbort.\n", tok->name.data);
	exit(1);
}

// # --------------------------------------------------------------------------

WORD_ARRAY *tryToFollowInstruction(INSTR *ins, T_ID_INS *tok, SYM_TBL *symTbl)
{
	WORD_ARRAY *buf = malloc(sizeof(WORD_ARRAY));
	WORD_ARRAY_init(buf);
	
	WORD_ARRAY_push(buf, ins->id);
	
	int i, r;
	for(i = 0 ; i < tok->argc ; i++)
	{
		char *arg = tok->arguments[i].data;
		char *deref;
		
		switch(ins->arg[i])
		{
			case ARG_NONE:
				goto fail;
				break;
			case ARG_REGISTER:
				if(arg[0] != 'r') goto fail;
				sscanf(arg, "r%i", &r);
				WORD_ARRAY_push(buf, (WORD) (r & 0xffff));
				break;
			case ARG_MEMORY:
				if(arg[0] != '(' || arg[1] != 'r' || arg[strlen(arg) - 1] != ')') goto fail;
				sscanf(arg, "(r%i)", &r);
				WORD_ARRAY_push(buf, (WORD) (r & 0xffff));
				break;
			case ARG_CONST:
				deref = SYM_TBL_solveAllSymbols(symTbl, arg);
				r = evaluateString(deref);
				free(deref);
				WORD_ARRAY_push(buf, (WORD) (r & 0xffff));
				break;
		}
	}
	
	if(ins->arg[i] != ARG_NONE) goto fail;
	
	return buf;
	
	fail:
	
	WORD_ARRAY_dispose(buf);
	free(buf);
	
	return NULL;
}

// # ==========================================================================

void ASM_init(ASM *this, WORD_ARRAY *src)
{
	this->tc = WORD_ARRAY_poll(src);
	this->token = malloc(this->tc * sizeof(T_ID));
	this->org = (WORD) 0;
	
	SYM_TBL_init(&this->symbolTable);
	
	int i;
	for(i = 0 ; i < this->tc ; i++)
	{
		T_readToken(&this->token[i], src);
	}
	
	WORD_ARRAY_dispose(src);
	
	this->ins = readInstructions("instructions.txt");
	WORD_ARRAY_init(&this->result);
}

WORD_ARRAY ASM_dispose(ASM *this)
{
	disposeInstructions(&this->ins);
	
	SYM_TBL_dispose(&this->symbolTable);
	
	int i;
	for(i = 0 ; i < this->tc ; i++)
	{
		T_disposeToken(&this->token[i]);
	}
	
	free(this->token);
	
	return this->result;
}


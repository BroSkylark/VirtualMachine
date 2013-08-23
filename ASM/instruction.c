#include "instruction.h"

INS_ARR readInstructions(const char *fn)
{
	int l;
	char *ptr = readFile(fn, &l);
	char *raw = realloc(ptr, l + 1);
	raw[l] = '\0';
	ptr = raw;
	
	if(raw == NULL)
	{
		fprintf(stderr, "ERR: Couldn't open file '%s'.\nAbort.\n", fn);
		exit(1);
	}
	
	INS_ARR arr;
	arr.instructions = NULL;
	arr.c = 0;
	
	while(*raw != '\0')
	{
		char *line = strdupv(raw, '\n');
		
		raw += strlen(line) + 1;
		
		arr.instructions = (INSTR *) realloc(arr.instructions, ++arr.c * sizeof(INSTR));
		arr.instructions[arr.c - 1] = INSTR_read(line);
		arr.instructions[arr.c - 1].id = (WORD) ((arr.c - 1) & 0xffff);
		
		free(line);
	}
	
	free(ptr);
	
	return arr;
}

void disposeInstructions(INS_ARR *this)
{
	int i;
	for(i = 0 ; i < this->c ; i++)
	{
		INSTR_dispose(&this->instructions[i]);
	}
	
	free(this->instructions);
	
	this->instructions = NULL;
	this->c = 0;
}

INSTR INSTR_read(char *line)
{
	line += 8;
	
	INSTR ins;
	ins.name = strdupv(line, ' ');
	ins.arg = NULL;
	
	toLowerCase(ins.name);
	
	line += 6;
	
	rmvW(line);
	
	int i = 0;
	while(1)
	{
		ins.arg = (int *) realloc(ins.arg, ++i * sizeof(int));
		
		if(*line == '\0')
		{
			ins.arg[i - 1] = ARG_NONE;
			break;
		}
		
		switch(*line)
		{
			case 'r':
				ins.arg[i - 1] = ARG_REGISTER;
				line += 2;
				break;
			case '(':
				ins.arg[i - 1] = ARG_MEMORY;
				line += 4;
				break;
			case 'C':
				ins.arg[i - 1] = ARG_CONST;
				line += 1;
				break;
			default:
				fprintf(stderr, "ERR: Unrecognized argument @'%.16s' ...\nAbort.\n", line);
		}
		
		if(*line == ',') line++;
	}
	
	return ins;
}

void INSTR_dispose(INSTR *this)
{
	free(this->name);
	free(this->arg);
	
	this->name = NULL;
	this->arg = NULL;
}


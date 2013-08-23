#include "tokenize.h"

typedef struct __tokenizer
{
	T_ID *token;
	SYM_TBL symTable;
	M_A macros;
	int tc, lblc, mc;
} TOK;

void TOK_init(TOK*);
void TOK_pushToken(TOK*, T_ID);
WORD_ARRAY TOK_evaluate(TOK*);
void TOK_copy(TOK*, TOK *);
void TOK_dispose(TOK*);

void tokenizeMeta(TOK *,  char *);
void tokenizeMacro(TOK *, char *);
void tokenizeLabel(TOK *, char *);
void tokenizeInstr(TOK *, char *);

T_SYM makeSymbol(char *);
void processChar(WORD_ARRAY *, char []);
int processLocal(char **, TOK *);
void tokenizeImpl(TOK *, char *);

WORD_ARRAY tokenize(char *src, int l)
{
	TOK tok;
	TOK_init(&tok);
	
	src = realloc(src, l + 1);
	src[l] = '\0';
	
	tokenizeImpl(&tok, src);
	
	WORD_ARRAY res = TOK_evaluate(&tok);
	
	TOK_dispose(&tok);
	
	free(src);
	
	return res;
}

void tokenizeImpl(TOK *tok, char *src)
{
	replaceC(src, '\t', ' ');
	skipW(&src);
	
	while(*src != '\0')
	{
		if(src[0] == '%' && src[1] == '%')
		{
			char *macro = strdupv(src, '}');
			src += strlen(macro) + 1;
			skipW(&src);
			
			tokenizeMacro(tok, macro);
			
			free(macro);
			
			continue;
		}
		
		char *line = strdupv(src, '\n');
		src += strlen(line) + 1;
		
		fprintf(stderr, "\n$$$ Tokenizing line '%s'\n", line);
		
		switch(*line)
		{
			case '.':
			case '#': tokenizeMeta(tok, line);  break;
//			case '%': tokenizeMacro(tok, line); break;
			case ':': tokenizeLabel(tok, line); break;
			case '\0': break;
			default:  tokenizeInstr(tok, line); break;
		}
		
		free(line);

		skipW(&src);
	}
}

void tokenizeMeta(TOK *tok, char *line)
{
	char buf[256];
	line++;
	strcpyv(buf, line, ' ', 256);
	
	toLowerCase(buf);
	
	if(strcmp(buf, "org") == 0)
	{
		line += 3;
		WORD r = evaluateString(line);
		
		T_ID_META *m = (T_ID_META *) malloc(sizeof(T_ID_META));
		m->meta = META_ORG;
		m->data = (void *) malloc(sizeof(WORD));
		*((WORD *) m->data) = r;
		
		T_ID t;
		t.id = ID_META;
		t.token = (void *) m;
		
		TOK_pushToken(tok, t);
		
		fprintf(stderr, "$ Set offset to @%#04X.\n", r);
	}
	else if(strcmp(buf, "equ") == 0 || strcmp(buf, "def") == 0 || strcmp(buf, "define") == 0)
	{
		line += strlen(buf);
		skipW(&line);
		
		strcpyv(buf, line, ' ', 128);
		line += strlen(buf);
		skipW(&line);
		
		strcpyv(buf + 128, line, ' ', 128);
		line += strlen(buf + 128);
		skipW(&line);
		
		if(strlen(buf) == 0 || strlen(buf + 128) == 0 || *line != '\0')
		{
			fprintf(stderr, "ERR: Invalid META:EQU '.equ %s %s'\nAbort.\n", buf, buf + 128);
		}
		
		SYM_TBL_addSymbol(&tok->symTable, buf, buf + 128);
		
		fprintf(stderr, "$ Added a new symbol: '%s' == '%s'\n", buf, buf + 128);
	}
	else if(strcmp(buf, "inc") == 0 || strcmp(buf, "include") == 0)
	{
		line += strlen(buf);
		replaceC(line, '"', ' ');
		rmvW(line);
		
		int l;
		char *src = readFile(line, &l);
		
		if(src == NULL)
		{
			fprintf(stderr, "ERR: Couldn't include file '%s'\nAbort.\n", line);
			return;
		}
		
		src = realloc(src, l + 1);
		src[l] = '\0';
		
		fprintf(stderr, "$ Including file '%s'.\n", line);
		
		tokenizeImpl(tok, src);
		
		free(src);
	}
	else if(strcmp(buf, "dw") == 0)
	{
		WORD_ARRAY *t = (WORD_ARRAY *) malloc(sizeof(WORD_ARRAY));
		WORD_ARRAY_init(t);
		line += 2;
		
		skipW(&line);
		
		int o = t->i;
		int i;
		char *eval;
		
		while(*line != '\0')
		{
			strcpyv(buf, line, ',', 256);
			
			switch(buf[0])
			{
				case '"':
					for(i = 1 ; buf[i] != '\0' && buf[i] != '"' ; i++)
					{
						processChar(t, buf + i);
						if(buf[i] == '\\') i++;
					}
					break;
				case '\'':
					processChar(t, buf + 1);
					break;
				default:
					eval = SYM_TBL_solveAllSymbols(&tok->symTable, buf);
					WORD_ARRAY_push(t, evaluateString(eval));
					free(eval);
					break;
			}
			
			line += strlen(buf);
			if(*line == ',') line++;
			skipW(&line);
		}
		
		if(t->i == 0)
		{
			fprintf(stderr, "ERR: Empty META.DW array!\nAbort.\n");
			return;
		}
		
		T_ID_META *m = (T_ID_META *) malloc(sizeof(T_ID_META));
		m->meta = META_DW;
		m->data = (void *) t;
		
		T_ID tid;
		tid.id = ID_META;
		tid.token = (void *) m;
		
		TOK_pushToken(tok, tid);
		
		fprintf(stderr, "$ Wrote a few (");
		
		for(; o < t->i ; o++)
		{
			fprintf(stderr, "0x%04X", t->data[o]);
			if(o < t->i - 1) fprintf(stderr, ", ");
		}
		
		fprintf(stderr, ") words.\n");
	}
}

void tokenizeMacro(TOK *tok, char *line)
{
	char *tmp = strdupv(line, ' ');
	toLowerCase(tmp);
	
	if(strcmp(tmp, "%%macro") != 0)
	{
		replaceC(line, '\n', '\\');
		fprintf(stderr, "ERR: This appears to be no macro: '%.16s'.\nAbort.\n", line);
		free(tmp);
		return;
	}
	
	line += strlen(tmp);
	skipW(&line);
	free(tmp);
	
	char *name = strdupv(line, '{');
	replaceC(name, '\n', ' ');
	line += strlen(name) + 1;
	rmvW(name);
	char buf[128];
	int argc = 0;
	replaceC(name, ':', ' ');
	sscanf(name, "%s %i", buf, &argc);
	
	if(argc < 0) argc = 0;
	
	free(name);
	name = strdup(buf);
	
	char *body = strdup(line);
	
	int l;
	TOK t_tmp;
	TOK_copy(tok, &t_tmp);
	
	int i;
	for(i = 0 ; i < tok->tc ; i++)
	{
		T_disposeToken(&tok->token[i]);
	}
	free(tok->token);
	
	tok->token = NULL;
	tok->tc = 0;
	
	tokenizeImpl(tok, body);
	
	l = 0;
	for(i = 0 ; i < tok->tc ; i++)
	{
		if(tok->token[i].id == ID_INS)
		{
			l += 1 + ((T_ID_INS *) tok->token[i].token)->argc;
		}
		else if(tok->token[i].id == ID_META && ((T_ID_META *) tok->token[i].token)->meta == META_DW)
		{
			l += ((WORD_ARRAY *) ((T_ID_META *) tok->token[i].token)->data)->i;
		}
	}
	
	TOK_dispose(tok);
	*tok = t_tmp;
	
	fprintf(stderr, "$ Recognized macro(%d) '%s' with %i argument(s).\n", l, name, argc);
	
	MAC m;
	MAC_init(&m);
	
	MAC_set(&m, name, argc, body, l);
	
	M_A_push(&tok->macros, m);
	
	sprintf(buf, "%%%s:%d", name, argc);
	sprintf(buf + 96, "%d", l);
	SYM_TBL_addSymbol(&tok->symTable, buf, buf + 96);
	
	free(name);
	free(body);
}

void tokenizeLabel(TOK *tok, char *line)
{
	char lbl[128];
	strcpyv(lbl, line, ' ', 128);
	
	const char *sid = SYM_TBL_dereference(&tok->symTable, lbl);
	int id = 0;
	sscanf(sid, "#%i", &id);
	
	T_ID_LBL *tlbl = (T_ID_LBL *) malloc(sizeof(T_ID_LBL));
	tlbl->id = id;
	
	T_ID tid;
	tid.id = ID_LBL;
	tid.token = (void *) tlbl;
	
	TOK_pushToken(tok, tid);
	
	fprintf(stderr, "$ Label '%s' (#%d) detected.\n", lbl, id);
}

void tokenizeInstr(TOK *tok, char *line)
{
	replaceC(line, '\t', ' ');
	skipW(&line);
	
	char buf[256];
	strcpyv(buf, line, ' ', 256);
	toLowerCase(buf);
	
	char *ins = strdup(buf);
	
	int i;
	for(i = 0 ; i < tok->macros.c ; i++)
	{
		if(strcmp(buf, tok->macros.macros[i].name) == 0)
		{
			char *tmp = strdup(line);
			char *ptr = tmp;
			tmp += strlen(ins);
			rmvW(tmp);
			int l;
			char **args = split(tmp, ',', &l);
			MAC *m = &tok->macros.macros[i];
			
			SYM_TBL st;
			SYM_TBL_init(&st);
			st.localFlag = 1;
			
			int j;
			for(j = 0 ; j < l ; j++)
			{
				sprintf(buf, "%%%d", j);
				SYM_TBL_addSymbol(&st, buf, args[j]);
				fprintf(stderr, ">>> sym: '%s' == '%s'\n", buf, args[j]);
				free(args[j]);
			}
			
			free(args);
			free(ptr);
			
			if(l != m->argc)
			{
				SYM_TBL_dispose(&st);
				continue;
			}
			
			char *trns = SYM_TBL_solveAllSymbols(&st, m->body);
			tokenizeImpl(tok, trns);
			free(trns);
			
			SYM_TBL_deleteLocalLabels(&tok->symTable);
			
			SYM_TBL_dispose(&st);
			
			free(ins);
			
			return;
		}
	}
	
	strcpy(buf, ins);
	free(ins);
	
	T_ID_INS *t_ins = (T_ID_INS *) malloc(sizeof(T_ID_INS));
	t_ins->name = makeSymbol(buf);
	t_ins->arguments = NULL;
	t_ins->argc = 0;
	
	char *args = line + strlen(buf);
	rmvW(args);
	
	fprintf(stderr, "$ Wrote instruction '%s'.\n", buf);
	
	int hasLocal = 0;
	
	while(*args != '\0')
	{
		strcpyv(buf, args, ',', 256);
		char *a = SYM_TBL_solveAllSymbols(&tok->symTable, buf);
		
		args += strlen(buf) + 1;
		
		hasLocal |= processLocal(&a, tok);
		
		t_ins->arguments = (T_SYM *) realloc(t_ins->arguments, ++t_ins->argc * sizeof(T_SYM));
		t_ins->arguments[t_ins->argc - 1] = makeSymbol(a);
		
		fprintf(stderr, "\t+ Argument '%s'\n", a);
		
		free(a);
	}
	
	T_ID tid;
	tid.id = ID_INS;
	tid.token = (void *) t_ins;
	
	TOK_pushToken(tok, tid);
	
	if(hasLocal)
	{
		char lbl[16];
		sprintf(lbl, "::loc%d", tok->lblc++);
		tokenizeLabel(tok, lbl);
	}
}

// # ==================================================================================================

void TOK_init(TOK *this)
{
	this->token = NULL;
	this->tc    = 0;
	this->lblc  = 0;
	
	SYM_TBL_init(&this->symTable);
	M_A_init(&this->macros);
}

void TOK_copy(TOK *this, TOK *tok)
{
	tok->tc = this->tc;
	tok->lblc = this->lblc;
	tok->token = malloc(this->tc * sizeof(T_ID));
	
	int i;
	for(i = 0 ; i < this->tc ; i++)
	{
		T_ID_copy(&this->token[i], &tok->token[i]);
	}
	
	SYM_TBL_copy(&this->symTable, &tok->symTable);
	M_A_copy(&this->macros, &tok->macros);
}

void TOK_pushToken(TOK *this, T_ID token)
{
	this->token = (T_ID *) realloc(this->token, ++this->tc * sizeof(T_ID));
	this->token[this->tc - 1] = token;
}

WORD_ARRAY TOK_evaluate(TOK *this)
{
	WORD_ARRAY arr;
	WORD_ARRAY_init(&arr);
	
	WORD_ARRAY_push(&arr, (WORD) (this->tc & 0xffff));
	
	int i;
	for(i = 0 ; i < this->tc ; i++)
	{
		T_writeToken(&this->token[i], &arr);
	}
	
	return arr;
}

void TOK_dispose(TOK *this)
{
	SYM_TBL_dispose(&this->symTable);
	M_A_dispose(&this->macros);
	
	int i;
	for(i = 0 ; i < this->tc ; i++)
	{
		T_disposeToken(&this->token[i]);
	}
	
	free(this->token);
}

// # --------------------------------------------------------------------------

T_SYM makeSymbol(char *id)
{
	T_SYM ts;
	ts.data = strdup(id);
	
	return ts;
}

int processLocal(char **src, TOK *tok)
{
	char *line = *src;
	const char *tmp;
	char lbl[16], buf[1024];
	sprintf(lbl, "::loc%d", tok->lblc);
	
	memset(buf, '\0', 1024);
	
	int i;
	for(i = 0 ; i < strlen(line) ; i++)
	{
		if(line[i] == '$')
		{
			tmp = SYM_TBL_dereference(&tok->symTable, lbl);
			
			if(i > 0) memcpy(buf, line, i);
			memcpy(buf + i, tmp, strlen(tmp));
			memcpy(buf + i + strlen(tmp), line + i + 1, strlen(line) - i);
			fprintf(stderr, "LBL: '%s'\n", buf);
			
			line = strdup(buf);
			free(*src);
			*src = line;
			
			return 1;
		}
	}
	
	return 0;
}

void processChar(WORD_ARRAY *arr, char src[])
{
	if(src[0] == '\\')
	{
		switch(src[1])
		{
			case '\'': WORD_ARRAY_push(arr, (WORD) ('\'' & 0xff)); break;
			case 't':  WORD_ARRAY_push(arr, (WORD) ('\t' & 0xff)); break;
			case 'n':  WORD_ARRAY_push(arr, (WORD) ('\n' & 0xff)); break;
			case '0':  WORD_ARRAY_push(arr, (WORD) ('\0' & 0xff)); break;
		}
	}
	else
	{
		WORD_ARRAY_push(arr, (WORD) (src[0] & 0xff));
	}
}


#include "token.h"

void T_disposeSymToken(T_SYM*);
void T_disposeMetaToken(T_ID_META*);
void T_disposeLabelToken(T_ID_LBL*);
void T_disposeInstrToken(T_ID_INS*);
void T_writeSymbolToken(T_SYM*, WORD_ARRAY *);
void T_writeMetaToken(T_ID_META*, WORD_ARRAY *);
void T_writeLabelToken(T_ID_LBL*, WORD_ARRAY *);
void T_writeInstrToken(T_ID_INS*, WORD_ARRAY *);
void T_readSymbolToken(T_SYM*, WORD_ARRAY *);
void T_readMetaToken(T_ID_META*, WORD_ARRAY *);
void T_readLabelToken(T_ID_LBL*, WORD_ARRAY *);
void T_readInstrToken(T_ID_INS*, WORD_ARRAY *);
void T_copyMetaToken(T_ID_META *this, T_ID_META *sym);
void T_copyLabelToken(T_ID_LBL *this, T_ID_LBL *sym);
void T_copyInstrToken(T_ID_INS *this, T_ID_INS *sym);

void T_writeToken(T_ID *this, WORD_ARRAY *arr)
{
	WORD_ARRAY_push(arr, ID);
	
	switch(this->id)
	{
		case ID_META: T_writeMetaToken((T_ID_META *) this->token, arr); break;
		case ID_LBL:  T_writeLabelToken((T_ID_LBL *) this->token, arr); break;
		case ID_INS:  T_writeInstrToken((T_ID_INS *) this->token, arr); break;
	}
}

void T_readToken(T_ID *this, WORD_ARRAY *arr)
{
	if(WORD_ARRAY_poll(arr) != ID)
	{
		fprintf(stderr, "ERR: Tried to read invalid Token.\nAbort.\n");
	}
	
	this->id = WORD_ARRAY_poll(arr);
	this->token = NULL;
	
	switch(this->id)
	{
		case ID_META: this->token = malloc(sizeof(T_ID_META)); T_readMetaToken((T_ID_META *) this->token, arr); break;
		case ID_LBL:  this->token = malloc(sizeof(T_ID_LBL));  T_readLabelToken((T_ID_LBL *) this->token, arr); break;
		case ID_INS:  this->token = malloc(sizeof(T_ID_INS));  T_readInstrToken((T_ID_INS *) this->token, arr); break;
	}
}

void T_disposeToken(T_ID *this)
{
	switch(this->id)
	{
		case ID_META: T_disposeMetaToken((T_ID_META *) this->token); break;
		case ID_LBL:  T_disposeLabelToken((T_ID_LBL *) this->token); break;
		case ID_INS:  T_disposeInstrToken((T_ID_INS *) this->token); break;
	}
	
	free(this->token);
}

void T_ID_copy(T_ID *this, T_ID *tid)
{
	tid->id = this->id;
	
	switch(this->id)
	{
		case ID_META: tid->token = malloc(sizeof(T_ID_META)); T_copyMetaToken((T_ID_META *) this->token, (T_ID_META *) tid->token); break;
		case ID_LBL:  tid->token = malloc(sizeof(T_ID_LBL));  T_copyLabelToken((T_ID_LBL *) this->token, (T_ID_LBL *) tid->token); break;
		case ID_INS:  tid->token = malloc(sizeof(T_ID_INS));  T_copyInstrToken((T_ID_INS *) this->token, (T_ID_INS *) tid->token); break;
	}
}

// # =======================================================================================

void T_copySymToken(T_SYM *this, T_SYM *sym)
{
	sym->data = strdup(this->data);
}

void T_copyMetaToken(T_ID_META *this, T_ID_META *tid)
{
	tid->meta = this->meta;
	
	switch(this->meta)
	{
		case META_ORG:
			tid->data = malloc(sizeof(WORD));
			*((WORD *) tid->data) = *((WORD *) this->data);
			break;
		case META_DW:
			tid->data = malloc(sizeof(WORD_ARRAY));
			WORD_ARRAY_init((WORD_ARRAY *) tid->data);
			WORD_ARRAY_merge((WORD_ARRAY *) tid->data, (WORD_ARRAY *) this->data);
			break;
	}
}

void T_copyLabelToken(T_ID_LBL *this, T_ID_LBL *tid)
{
	tid->id = this->id;
}

void T_copyInstrToken(T_ID_INS *this, T_ID_INS *tid)
{
	T_copySymToken(&this->name, &tid->name);
	
	tid->argc = this->argc;
	tid->arguments = malloc(this->argc * sizeof(T_SYM));
	
	int i;
	for(i = 0 ; i < this->argc ; i++)
	{
		T_copySymToken(&this->arguments[i], &tid->arguments[i]);
	}
}

// # ---------------------------------------------------------------------------------------

void T_disposeSymToken(T_SYM *this)
{
	free(this->data);
}

void T_disposeMetaToken(T_ID_META *this)
{
	free(this->data);
}

void T_disposeLabelToken(T_ID_LBL *this)
{
}

void T_disposeInstrToken(T_ID_INS *this)
{
	T_disposeSymToken(&this->name);
	
	int i;
	for(i = 0 ; i < this->argc ; i++)
	{
		T_disposeSymToken(&this->arguments[i]);
	}
	
	free(this->arguments);
}

// # --------------------------------------------------------------------------

void T_writeSymbolToken(T_SYM *this, WORD_ARRAY *arr)
{
	WORD_ARRAY_push(arr, SYM);
	WORD_ARRAY_write(arr, this->data);
	WORD_ARRAY_push(arr, EOS);
}

void T_writeMetaToken(T_ID_META *this, WORD_ARRAY *arr)
{
	WORD_ARRAY_push(arr, ID_META);
	WORD_ARRAY_push(arr, this->meta);
	
	switch(this->meta)
	{
		case META_ORG:
			WORD_ARRAY_push(arr, *((WORD *) this->data));
			break;
		case META_DW:
			WORD_ARRAY_push(arr, ((WORD_ARRAY *) this->data)->i);
			WORD_ARRAY_merge(arr, (WORD_ARRAY *) this->data);
			break;
	}
}

void T_writeLabelToken(T_ID_LBL *this, WORD_ARRAY *arr)
{
	WORD_ARRAY_push(arr, ID_LBL);
	WORD_ARRAY_push(arr, this->id);
}

void T_writeInstrToken(T_ID_INS *this, WORD_ARRAY *arr)
{
	WORD_ARRAY_push(arr, ID_INS);
	T_writeSymbolToken(&this->name, arr);
	WORD_ARRAY_push(arr, (WORD) (this->argc & 0xffff));
	
	int i;
	for(i = 0 ; i < this->argc ; i++)
	{
		T_writeSymbolToken(&this->arguments[i], arr);
	}
}

// # --------------------------------------------------------------------------

void T_readSymbolToken(T_SYM *this, WORD_ARRAY *arr)
{
	if(WORD_ARRAY_poll(arr) != SYM)
	{
		fprintf(stderr, "ERR: Tried to read symlink.\nAbort.\n");
		exit(1);
	}
	
	char buf[512];
	int i = -1;
	do
	{
		buf[++i] = (char) WORD_ARRAY_poll(arr);
	} while(buf[i] != '\0');
	
	this->data = strdup(buf);
}

void T_readMetaToken(T_ID_META *this, WORD_ARRAY *arr)
{
	this->meta = WORD_ARRAY_poll(arr);
	int i;
	
	switch(this->meta)
	{
		case META_ORG:
			this->data = malloc(sizeof(WORD));
			*((WORD *) this->data) = WORD_ARRAY_poll(arr);
			break;
		case META_DW:
			this->data = malloc(sizeof(WORD_ARRAY));
			WORD_ARRAY_init((WORD_ARRAY *) this->data);
			i = (int) WORD_ARRAY_poll(arr);
			while(i-- > 0)
			{
				WORD_ARRAY_push((WORD_ARRAY *) this->data, WORD_ARRAY_poll(arr));
			}
	}
}

void T_readLabelToken(T_ID_LBL *this, WORD_ARRAY *arr)
{
	this->id = WORD_ARRAY_poll(arr);
}

void T_readInstrToken(T_ID_INS *this, WORD_ARRAY *arr)
{
	T_readSymbolToken(&this->name, arr);
	this->argc = (int) WORD_ARRAY_poll(arr);
	this->arguments = ((this->argc == 0) ? NULL : malloc(this->argc * sizeof(T_SYM)));
	
	int i;
	for(i = 0 ; i < this->argc ; i++)
	{
		T_readSymbolToken(&this->arguments[i], arr);
	}
}


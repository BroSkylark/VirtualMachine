#include "symtable.h"

void  SYM_TBL_init(SYM_TBL *this)
{
	this->ids       = NULL;
	this->vals      = NULL;
	this->c         = 0;
	this->lblc      = 0;
	this->localFlag = 0;
}

void  SYM_TBL_addSymbol(SYM_TBL *this, const char *id, const char *v)
{
	int i = SYM_TBL_getPos(this, id);
	if(i >= 0)
	{
		free(this->vals[i]);
		this->vals[i] = strdup(v);
	}
	else
	{
		this->ids  = (char **) realloc(this->ids,  ++this->c * sizeof(char *));
		this->vals = (char **) realloc(this->vals,   this->c * sizeof(char *));
	
		this->ids[this->c - 1]  = strdup(id);
		this->vals[this->c - 1] = strdup(v);
	}
}

const char *SYM_TBL_dereference(SYM_TBL *this, const char *id)
{
	int i = SYM_TBL_getPos(this, id);
	
	if(i >= 0)
	{
		return this->vals[i];
	}
	else if(this->localFlag == 0 && id[0] == ':' && id[1] == ':')
	{
		char buf[16];
		sprintf(buf, "#%d", this->lblc++);
		SYM_TBL_addSymbol(this, id, buf);
		
		return SYM_TBL_dereference(this, id);
	}
	
	return id;
}

int inline isAlphaUDigit(char c) { return (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
											 c == '_' || c == ':' || c == '#' || c == '%' || (c >= '0' && c <= '9')) ? 1 : 0); }

char *SYM_TBL_solveAllSymbols(SYM_TBL *this, const char *line)
{
	char res[1024], buf[256];
	int bi = 0, i = 0, ri = 0;
	
	while(ri < 1023 - bi && bi < 255)
	{
		char c = line[i++];
		
		if(isAlphaUDigit(c))
		{
			buf[bi++] = c;
		}
		else
		{
			if(bi > 0)
			{
				buf[bi] = '\0';
				const char *deref = SYM_TBL_dereference(this, buf);
				if(deref != buf)
				{
					deref = SYM_TBL_solveAllSymbols(this, deref);
					strcpy(buf, deref);
					free((void *) deref);
					deref = buf;
				}
	
				memcpy(res + ri, buf, strlen(buf));
				ri += strlen(buf);
				bi = 0;
			}
			
			res[ri++] = c;
			
			if(c == '\0') break;
		}
	}
	
	return strdup(res);
}

int SYM_TBL_getPos(SYM_TBL *this, const char *id)
{
	int i;
	for(i = 0 ; i < this->c ; i++)
	{
		if(strcmp(this->ids[i], id) == 0)
		{
			return i;
		}
	}
	
	return -1;
}

void SYM_TBL_deleteLocalLabels(SYM_TBL *this)
{
	SYM_TBL st;
	SYM_TBL_init(&st);
	
	int i;
	for(i = 0 ; i < this->c ; i++)
	{
		char *t = this->ids[i];
		while(*t != '\0')
		{
			if(*t != ':' && (*t < '0' || *t > '9'))
			{
				SYM_TBL_addSymbol(&st, this->ids[i], this->vals[i]);
				break;
			}
			
			t++;
		}
		
		free(this->ids[i]);
		free(this->vals[i]);
	}
	
	st.lblc = this->lblc;
	
	free(this->ids);
	free(this->vals);
	
	*this = st;
}

void SYM_TBL_copy(SYM_TBL *this, SYM_TBL *tbl)
{
	tbl->lblc      = this->lblc;
	tbl->localFlag = this->localFlag;
	tbl->c         = this->c;
	tbl->ids       = this->c > 0 ? malloc(this->c * sizeof(char *)) : NULL;
	tbl->vals      = this->c > 0 ? malloc(this->c * sizeof(char *)) : NULL;
	
	int i;
	for(i = 0 ; i < this->c ; i++)
	{
		tbl->ids[i]  = strdup(this->ids[i]);
		tbl->vals[i] = strdup(this->vals[i]);
	}
}

void  SYM_TBL_dispose(SYM_TBL *this)
{
	int i;
	for(i = 0 ; i < this->c ; i++)
	{
		free(this->ids[i]);
		free(this->vals[i]);
	}
	
	free(this->ids);
	free(this->vals);
	
	SYM_TBL_init(this);
}


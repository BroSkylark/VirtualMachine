#include "WORD_ARRAY.h"

void WORD_ARRAY_resize(WORD_ARRAY *);

void WORD_ARRAY_init(WORD_ARRAY *this)
{
	this->c = this->i = 0;
	this->data = NULL;
}

void WORD_ARRAY_set(WORD_ARRAY *this, int i, WORD v)
{
	while(i >= this->c)
	{
		WORD_ARRAY_resize(this);
	}
	
	this->data[i] = v;
	
	if(i > this->i) this->i = i;
}

void WORD_ARRAY_push(WORD_ARRAY *this, WORD v)
{
	while(this->c <= this->i)
	{
		WORD_ARRAY_resize(this);
	}
	
	this->data[this->i++] = v;
}

WORD WORD_ARRAY_peek(WORD_ARRAY *this)
{
	if(this->i == 0)
	{
		fprintf(stderr, "ERR: Tried to peek in empty WORD_ARRAY!\nAbort.\n");
		return 0;
	}
	
	return this->data[0];
}

WORD WORD_ARRAY_poll(WORD_ARRAY *this)
{
	if(this->i == 0)
	{
		fprintf(stderr, "ERR: Tried to poll from empty WORD_ARRAY!\nAbort.\n");
		return 0;
	}
	
	WORD r = this->data[0];
	if(--this->i > 0)
	{
		memmove(this->data, this->data + 1, this->i * sizeof(WORD));
	}
	
	return r;
}

int  WORD_ARRAY_getPos(WORD_ARRAY *this)
{
	return this->i;
}

void WORD_ARRAY_merge(WORD_ARRAY *this, WORD_ARRAY *insertee)
{
	if(insertee->c == 0) return;
	
	while(this->c <= this->i + insertee->i)
	{
		WORD_ARRAY_resize(this);
	}
	
	memcpy(this->data + this->i, insertee->data, insertee->i * sizeof(WORD));
	this->i += insertee->i;
}

void WORD_ARRAY_write(WORD_ARRAY *this, char *src)
{
	int l = strlen(src);
	
	while(this->i + l >= this->c)
	{
		WORD_ARRAY_resize(this);
	}
	
	int i;
	for(i = 0 ; i < l ; i++)
	{
		this->data[this->i + i] = (WORD) (src[i] & 0xff);
	}
	
	this->i += l;
}

void WORD_ARRAY_dispose(WORD_ARRAY *this)
{
	free(this->data);
	
	WORD_ARRAY_init(this);
}

void WORD_ARRAY_resize(WORD_ARRAY *this)
{
	this->c = ((this->c == 0) ? 0x10 : (this->c << 2));
	this->data = realloc(this->data, this->c * sizeof(WORD));
}

WORD_ARRAY WORD_ARRAY_restore(char *src, int l)
{
	WORD_ARRAY w;
	w.data = (WORD *) src;
	w.i = w.c = l / sizeof(WORD);
	
	return w;
}


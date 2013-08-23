#include "macros.h"

void M_A_init(M_A *this)
{
	this->macros = NULL;
	this->c = 0;
}

void M_A_push(M_A *this, MAC macro)
{
	macro.id = this->c;
	this->macros = realloc(this->macros, ++this->c * sizeof(MAC));
	this->macros[this->c - 1] = macro;
}

void M_A_copy(M_A *this, M_A *ma)
{
	ma->c = this->c;
	ma->macros = this->c > 0 ? malloc(this->c * sizeof(MAC)) : NULL;
	
	int i;
	for(i = 0 ; i < this->c ; i++)
	{
		MAC_copy(&this->macros[i], &ma->macros[i]);
	}
}

void M_A_dispose(M_A *this)
{
	int i;
	for(i = 0 ; i < this->c ; i++)
	{
		MAC_dispose(&this->macros[i]);
	}
	
	free(this->macros);
	
	M_A_init(this);
}

void MAC_init(MAC *this)
{
	this->name = NULL;
	this->body = NULL;
	this->id   = 0;
	this->argc = 0;
}

void MAC_set(MAC *this, const char *name, int argc, const char *body, int size)
{
	this->name = strdup(name);
	this->body = strdup(body);
	this->argc = argc;
	this->size = size;
}

void MAC_copy(MAC *this, MAC *mac)
{
	mac->name = strdup(this->name);
	mac->body = strdup(this->body);
	mac->argc = this->argc;
	mac->size = this->size;
	mac->id   = this->id;
}

void MAC_dispose(MAC *this)
{
	free(this->name);
	free(this->body);
	
	MAC_init(this);
}


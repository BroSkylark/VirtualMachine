#include "include.h"

char **split(const char *src, char c, int *size)
{
	char **r = NULL;
	int i = 0;
	
	while(*src != '\0')
	{
		char *tmp = strdupv(src, c);
		src += strlen(tmp) + 1;

		if(*tmp == '\0')
		{
			free(tmp);
			continue;
		}
		
		r = realloc(r, ++i * sizeof(char *));
		r[i - 1] = tmp;
	}
	
	*size = i;
	
	return r;
}

void appendC(char **src, char c)
{
	char *line = *src;
	int l = strlen(line);
	line = realloc(line, l + 2);
	line[l] = c;
	line[l + 1] = '\0';
	
	*src = line;
}

WORD evaluateString(const char *line)
{
	return (WORD) (evaluate(line) & 0xffff);
}

void strcpyv(char *target, char *src, char v, int l)
{
	while(*src != v && *src != '\0')
	{
		if(--l == 0) break;
		
		*target = *src;
		target++;
		src++;
	}
	
	*target = '\0';
}

void inline toLowerCase(char *line)
{
	for(; *line ; line++) if(*line >= 'A' && *line <= 'Z') *line += 'a' - 'A';
}

void inline toUpperCase(char *line)
{
	for(; *line ; line++) if(*line >= 'a' && *line <= 'z') *line += 'A' - 'a';
}

int replaceC(char *line, char v, char r)
{
	int c = 0;
	
	while(*line != '\0')
	{
		if(*line == v)
		{
			*line = r;
			c++;
		}
		
		line++;
	}
	
	return c;
}

char *strdupv(const char *line, char v)
{
	int l = 0;
	while(line[l] != v && line[l] != '\0') l++;
	
	char *r = (char *) malloc(l + 1);
	memcpy(r, line, l);
	r[l] = '\0';
	
	return r;
}

void inline skipW(char **ptr)
{
	while(**ptr == ' ' || **ptr == '\t' || **ptr == '\n') (*ptr)++;
}

void rmvW(char *line)
{
	int i;
	for(i = 0 ; line[i] != '\0' ; i++)
	{
		if(line[i] == ' ' || line[i] == '\t')
		{
			int j;
			for(j = i + 1 ; line[j - 1] != '\0' ; j++)
			{
				line[j - 1] = line[j];
			}
			
			i--;
		}
	}
}

char *readFile(const char *fn, int *size)
{
	int l;
	if(size == NULL) size = &l;
	
	FILE *src_f = fopen(fn, "rb");
	
	if(src_f == NULL)
	{
		return NULL;
	}
	
	fseek(src_f, 0, SEEK_END);
	l = *size = ftell(src_f);
	fseek(src_f, 0, SEEK_SET);
	
	char *src = (char *) malloc(l);
	
	fread(src, 1, l, src_f);
	fclose(src_f);
	
	return src;
}

const char *findLast(const char *src, char v)
{
	const char *ptr = src;
	
	while(*src != '\0')
	{
		if(*src == v) ptr = src;
		src++;
	}
	
	return ptr;
}



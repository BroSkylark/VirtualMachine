#include "include.h"
#include "WORD_ARRAY.h"
#include "instruction.h"
#include "tokenize.h"
#include "assemble.h"

char errMsg[256];

int main(int argc, char *argv[])
{
	if(argc < 3)
	{
		fprintf(stderr, "Usage: %s [-a] file.s file.o\n", findLast(argv[0], '/') + 1);
		return EXIT_FAILURE;
	}
	
	char *inFile = argc == 3 ? argv[1] : argv[2];
	char *outFile = argc == 3 ? argv[2] : argv[3];
	
	int l;
	char *src = readFile(inFile, &l);
	
	if(src == NULL)
	{
		fprintf(stderr, "ERR: Couldn't read file '%s'.\nAbort.\n", inFile);
		return EXIT_FAILURE;
	}
	
	WORD_ARRAY result;
	
	if(strcmp(argv[1], "-a") == 0)
	{
		result = assemble(WORD_ARRAY_restore(src, l));
	}
	else
	{
		result = tokenize(src, l);
	}
	
	if(result.data == NULL)
	{
		fprintf(stderr, "ERR: %s\nAbort.\n", errMsg);
		return EXIT_FAILURE;
	}
	
	FILE *out = fopen(outFile, "wb");
	
	if(out == NULL)
	{
		fprintf(stderr, "ERR: Couldn't open file '%s'.\nAbort.\n", outFile);
		return EXIT_FAILURE;
	}
	
	fwrite(result.data, 1, result.i * sizeof(WORD), out);
	fclose(out);
	
	WORD_ARRAY_dispose(&result);
	
	return EXIT_SUCCESS;
}


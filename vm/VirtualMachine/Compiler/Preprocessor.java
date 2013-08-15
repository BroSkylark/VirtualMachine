package vm.VirtualMachine.Compiler;

import java.util.ArrayList;
import java.util.List;

import vm.VirtualMachine.StringHelper;
import vm.VirtualMachine.Assembler.Assembler;

public class Preprocessor
{
	public static String process(String path)
	{
		return StringHelper.concatList(process_recursion(StringHelper.makePresentable(
				Assembler.readFile(path).replaceAll("[\\t ]+", " ").trim()), 0, new ArrayList<String>()));
	}
	
	private static String[] process_recursion(String l[], int rl, List<String> ifs)
	{
		if(rl > 256) throw new RuntimeException("TOO MANY RECURSIONS!");
		
		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] == null || !l[i].startsWith("#")) continue;
			
			if(l[i].startsWith("#include "))
			{
				String fn = l[i].substring(8).trim();
				String lines[] = StringHelper.makePresentable(
						Assembler.readFile(fn).replaceAll("[\\t ]+", " ").trim());
				l[i] = null;
				
				if(ifs.contains(fn)) continue;
				
				ifs.add(fn);
				l[i] = StringHelper.concatList(process_recursion(lines, rl + 1, ifs));
			}
		}
		
		return l;
	}
}

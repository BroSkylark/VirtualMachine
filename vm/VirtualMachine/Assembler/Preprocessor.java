package vm.VirtualMachine.Assembler;

import java.util.ArrayList;
import java.util.List;

import vm.VirtualMachine.Start;
import vm.VirtualMachine.StringHelper;

public class Preprocessor
{
	private int recursionLevel;
	private List<String> includes;
	
	public Preprocessor(String filename)
	{
		recursionLevel = 0;
		includes = new ArrayList<String>();
		includes.add(filename);
	}
	
	public String process(String l[])
	{
		if(++recursionLevel > 256) throw new IllegalArgumentException("TOO MANY RECURSIONS!!!");
		
		List<String> tmp = new ArrayList<String>(l.length);
		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] == null) continue;
			
			if(!l[i].startsWith("#"))
			{
				tmp.add(l[i]);
				continue;
			}
			
			if(l[i].startsWith("#include"))
			{
				String p = l[i].substring(8).trim();
				String fn = p.split("/")[p.split("/").length - 1];
				
				if(includes.contains(fn)) continue;

				includes.add(fn);
				tmp.add(process(StringHelper.makePresentable(Assembler.readFile(p))));
			}
		}
		
		return StringHelper.concatList(tmp);
	}
}

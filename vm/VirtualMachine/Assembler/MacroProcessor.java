package vm.VirtualMachine.Assembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vm.VirtualMachine.StringHelper;

public class MacroProcessor
{
	private static Map<String, Macro> macroTable;
	
	public static String resolveMacros(String s)
	{
		macroTable = new HashMap<String, Macro>();
		Pattern mp = Pattern.compile("(?s)%%macro\\s+?(\\w+:?\\d*?)\\s+?\\{(.*?)\\}(?-s)");
		
		Matcher m = mp.matcher(s);
		while(m.find())
		{
			String mn = m.group(1).toLowerCase();
			String md = m.group(2);
			
			macroTable.put(mn, new Macro(mn, md));
			
			m = mp.matcher(s = m.replaceFirst(""));
		}

		for(Macro macro : macroTable.values())
		{
			macro.solveLabels();
		}

		for(Macro macro : macroTable.values())
		{
			macro.evaluate();
		}
		
		return evaluateMacros(s);
	}
	
	private static String evaluateMacros(String s)
	{
		String l[] = StringHelper.makePresentable(s);
		List<String> tmp = new ArrayList<String>();
		
		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] == null) continue;

			if(ResolveDot.checkIfCanBeCompiled(l[i]) < 0)
			{
				for(Macro macro : macroTable.values())
				{
					if(l[i].toLowerCase().startsWith(macro.getName()))
					{
						String args[] = l[i].substring(macro.getName().length()).trim().split(",");
						
						if(args.length != macro.getArgCount()) continue;
						
						String ls[] = StringHelper.makePresentable(macro.getBody(args));
						for(int j = 0 ; j < ls.length ; j++)
						{
							if(ls[j] != null) tmp.add(ls[j]);
						}

						l[i] = null;
						break;
					}
				}
			}
			
			if(l[i] != null) tmp.add(l[i]);
		}
		
		return StringHelper.concatList(tmp);
	}
	
	private static Pattern LBL = Pattern.compile("::(\\d+)"), LBL_F = Pattern.compile("(::__\\w+?_\\d+?_)#");
	
	private static class Macro
	{
		private String name, body;
		private int argc, instanciation;
		
		public Macro(String name, String body)
		{
			String s[] = name.split(":");
			this.name = s[0];
			this.body = body;
			this.instanciation = 0;
			this.argc = s.length > 1 ? Integer.parseInt(s[1]) : 1;
		}
		
		public String getName() { return name; }
		public int getArgCount() { return argc; }
		
		public void evaluate()
		{
			body = evaluateMacros(body);
		}
		
		public void solveLabels()
		{
			Matcher m = LBL.matcher(body);
			
			while(m.find()) m = LBL.matcher(body = m.replaceFirst("::__" + name + "_$1_#"));;
		}
		
		public String getBody(String params[])
		{
			String r = body;
			
			if(params.length != argc) throw new IllegalArgumentException("MACRO(" + name + ":" + argc + 
					"): Wrong number of arguments!");
			
			for(int i = 0 ; i < params.length ; i++)
			{
				if(params[i] == null) continue;
				
				Matcher m = Pattern.compile("%" + i).matcher(r);
				r = m.replaceAll(params[i]);
			}
			
			Matcher m = LBL_F.matcher(r);

			while(m.find()) m = LBL_F.matcher(r = m.replaceFirst("$1" + instanciation));

			instanciation++;
			
			return r;
		}
	}
}

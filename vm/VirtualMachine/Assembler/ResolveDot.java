package vm.VirtualMachine.Assembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vm.VirtualMachine.MathHelper;
import vm.VirtualMachine.Start;
import vm.VirtualMachine.StringHelper;

public abstract class ResolveDot
{
	public static String resolveDBW(String s)
	{
		String l[] = StringHelper.makePresentable(s);
		
		Pattern p = Pattern.compile("[\"'](.*?)[\"']"), q = Pattern.compile("(\".*?)'(.*?\")");
		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] == null) continue;
			boolean w = l[i].startsWith(".dw");
			
			if(w || l[i].startsWith(".db"))
			{
				String t = l[i].substring(3).trim().replaceAll("\\\\\"", "\"," + ((int)'"') + ",\"");
				Matcher m = q.matcher(t);
				while(m.find()) m = q.matcher(t = m.replaceFirst("$1\"," + ((int) '\'') + ",\"$2"));
				
				m = p.matcher(t);
				while(m.find()) m = p.matcher(t = m.replaceFirst(expand(m.group(1))));
				l[i] = (w ? ".dw " : ".db ") + t;
			}
		}

		return StringHelper.concatList(l);
	}
	
	public static String resolveEqu(String s)
	{
		Map<String, String> equs = new HashMap<String, String>();
		
		String l[] = StringHelper.makePresentable(s);

		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] == null) continue;

			if(l[i].startsWith(".equ"))
			{
				Matcher m = Assembler.PTRN_EQU.matcher(l[i]);
				if(m.find())
				{
					equs.put(m.group(1), m.group(2));
					l[i] = null;
				}
				else
				{
					throw new IllegalArgumentException("EQU_ERR: " + l[i]);
				}
			}
		}

		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] == null) continue;
			
			for(String key : equs.keySet())
			{
				Matcher m = Pattern.compile("(\\s*\\w+\\s+([\\(\\)\\w%]+,)*\\(?)" + key + "(?!\\w+)(\\)?(,[\\w%]+)*)").matcher(l[i]);
				if(m.find())
				{
					l[i] = m.replaceAll("$1" + equs.get(key).toString() + "$" + (m.groupCount() - 1));
				}
			}
		}

		return StringHelper.concatList(l);
	}

	public static String resolveSymbols(String txt)
	{
		Map<String, Integer> symbols = new HashMap<String, Integer>();
		String l[] = StringHelper.makePresentable(txt);
		int org = 0, $c = 0;

		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] != null) 
			{
				if(l[i].startsWith("::"))
				{
					symbols.put(l[i], org);
					Start.out(String.format("### New label: '" + l[i] + "' @0x%04X", org));
					l[i] = null;
				}
				else if(l[i].startsWith(".org"))
				{
					org = MathHelper.evaluate(l[i].substring(5));
					l[i] = null;
				}
				else if(l[i].startsWith(".equ"))
				{
					throw new IllegalArgumentException("ERR: unresolved EQU in symbols: " + l[i]);
				}
				else
				{
					Matcher m = Assembler.PTRN_$.matcher(l[i]);
					String tmp = l[i];
					boolean found = m.find();
					String group1 = "";
					
					if(found)
					{
						group1 = m.group(1);
						tmp = m.replaceAll(String.valueOf(0));
						l[i] = m.replaceAll("::__\\$" + $c);
					}
					
					int d = checkIfCanBeCompiled(tmp);
					
					if(d >= 0)
					{
						org += d;
						
						if(found)
						{
							int v = MathHelper.evaluate(org + group1);
							symbols.put("::__$" + $c, v);
							$c++;
						}
					}
					else
					{
						throw new IllegalArgumentException("ERR: Uncompilable: \"" + l[i] + "\"");
					}
				}
			}
		}

		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] == null) continue;
			
			for(String key : symbols.keySet())
			{
				Matcher m = Pattern.compile("(\\s*\\w+\\s+(\\w+,)*)?\\Q" + key + "\\E(?!\\w+)((,\\w+)*)").matcher(l[i]);
				if(m.find())
				{
					l[i] = m.replaceAll("$1" + symbols.get(key).toString() + "$" + (m.groupCount() - 1));
				}
			}
		}

		return StringHelper.concatList(l);
	}
	
	public static String resolveNumbers(String s)
	{
		String l[] = StringHelper.makePresentable(s);
		String no = "(0b[01]+)|(0o[0-8]+)|(0d\\d+)|(0x[a-fA-F\\d]+)";
		String n = "(" + no + "|(\\d+))";
		Pattern p = Pattern.compile("[\\(!~]*" + n + "?([!~><\\(\\)\\+\\-\\*\\/\\^\\&\\|]+[" + n + "]+)+");
		
		for(int i = 0 ; i < l.length ; i++)
		{
			Matcher m = p.matcher(l[i]);
			while(m.find())
			{
				m = p.matcher(l[i] = m.replaceFirst(Integer.toString(MathHelper.evaluate(m.group(0)) & 0xffff)));
			}
		}
		
		p = Pattern.compile(no);
		for(int i = 0 ; i < l.length ; i++)
		{
			Matcher m = p.matcher(l[i]);
			while(m.find())
			{
				m = p.matcher(l[i] = m.replaceFirst(Integer.toString(MathHelper.evaluate(m.group(0)) & 0xffff)));
			}
		}
		
		return StringHelper.concatList(l);
	}
	
	public static int checkIfCanBeCompiled(String s)
	{
		Matcher m = Assembler.PTRN_$.matcher(s);

		if(m.find())
		{
			s = m.replaceAll("0");
		}

		m = Assembler.PTRN_LBL.matcher(s);

		if(m.find())
		{
			s = m.replaceAll("0");
		}

		m = Assembler.PTRN_VAR.matcher(s);

		if(m.find())
		{
			s = m.replaceAll("$1\\0");
		}

		int c[] = Assembler.compileLine(s);

		return c == null ? -1 : c.length;
	}
	
	private static String expand(String s)
	{
		String r = "";
		char c[] = s.toCharArray();
		
		for(int i = 0 ; i < c.length ; i++)
		{
			if(i > 0) r += ",";
			r += String.valueOf((int) c[i]);
		}
		
		return r;
	}
}

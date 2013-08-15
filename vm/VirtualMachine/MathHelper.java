package vm.VirtualMachine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MathHelper
{
	public static int evaluate(String exp)
	{
		Matcher m = SPACE.matcher(exp);
		if(m.find()) exp = m.replaceAll("");
		
		int r = 0;
		try
		{
			r = Integer.parseInt(exp);
		}
		catch(Exception e)
		{
			r = eval(exp);
		}
		
		return r;
	}
	
	private static int eval(String exp)
	{
		Matcher m = NUMBER.matcher(exp);
		while(m.find()) m = NUMBER.matcher(exp = m.replaceFirst(String.valueOf(readInt(m.group(1)))));
		
		while(exp.contains("("))
		{
			int p = 0, op = -1;
			for(int i = 0 ; i < exp.length() ; i++)
			{
				if(exp.charAt(i) == '(')
				{
					if(p == 0) op = i + 1;
					p++;
				}
				else if(exp.charAt(i) == ')')
				{
					if(--p == 0)
					{
						exp = exp.substring(0, op - 1) + 
								evaluate(exp.substring(op, i)) + 
									exp.substring(i + 1, exp.length());
						break;
					}
				}
			}
			
			if(p != 0)
			{
				throw new RuntimeException("ERR: Faulty mathematical expression: '" + exp + "'");
			}
		}

		return Integer.parseInt(evalPM(exp));
	}
	
	private static final String evalPM(String e)
	{
		Matcher m = PLUS.matcher(e);
		
		if(m.find())
		{
			int i1 = Integer.parseInt(evalPM(m.group(1)));
			int i2 = Integer.parseInt(evalPM(m.group(3)));
			int i = m.group(2).equalsIgnoreCase("+") ? i1 + i2 : i1 - i2;
			e = String.valueOf(i);
		}
		
		return evalTD(e);
	}
	
	private static final String evalTD(String e)
	{
		Matcher m = TIMES.matcher(e);
		
		if(m.find())
		{
			int i1 = Integer.parseInt(evalTD(m.group(1)));
			int i2 = Integer.parseInt(evalTD(m.group(3)));
			int i = m.group(2).equalsIgnoreCase("*") ? i1 * i2 : i1 / i2;
			e = String.valueOf(i);
		}
		
		return evalAOX(e);
	}
	
	private static final String evalAOX(String e)
	{
		Matcher m = AOXN.matcher(e);
		
		if(m.find())
		{
			int i1 = Integer.parseInt(evalAOX(m.group(1)));
			int i2 = Integer.parseInt(evalAOX(m.group(3)));
			int i = m.group(2).equalsIgnoreCase("&") ? (i1 & i2) : m.group(2).equalsIgnoreCase("|") ? 
					(i1 | i2) : m.group(2).equalsIgnoreCase("^") ? (i1 ^ i2) : m.group(2).equalsIgnoreCase("<<")
							? (i1 << i2) : (i1 >> i2);
			e = String.valueOf(i);
		}
		
		if(e.startsWith("!") || e.startsWith("~"))
		{
			e = String.valueOf(~Integer.parseInt(e.substring(1)));
		}
		
		return e;
	}

	private static int readInt(String s)
	{
		if(s.toLowerCase().startsWith("0d"))
		{
			return Integer.parseInt(s.substring(2));
		}
		else if(s.toLowerCase().startsWith("0b"))
		{
			return readBin(s.substring(2));
		}
		else if(s.toLowerCase().startsWith("0x"))
		{
			return readHex(s.substring(2));
		}
		else if(s.toLowerCase().startsWith("0o"))
		{
			return readOct(s.substring(2));
		}
		else if(s.toLowerCase().startsWith("0"))
		{
			return readQuart(s.substring(1));
		}
		else
		{
			return Integer.parseInt(s);
		}
	}

	public static int readBin(String s)
	{
		String bin = "01";
		char c[] = s.toUpperCase().toCharArray();
		int r = 0;
		
		for(int i = 0 ; i < c.length ; i++)
		{
			r = (r << 1) | bin.indexOf(c[i]);
		}
		
		return r;
	}

	public static int readQuart(String s)
	{
		String oct = "0123";
		char c[] = s.toUpperCase().toCharArray();
		int r = 0;
		
		for(int i = 0 ; i < c.length ; i++)
		{
			r = (r << 2) | oct.indexOf(c[i]);
		}
		
		return r;
	}
	
	public static int readOct(String s)
	{
		String oct = "01234567";
		char c[] = s.toUpperCase().toCharArray();
		int r = 0;
		
		for(int i = 0 ; i < c.length ; i++)
		{
			r = (r << 3) | oct.indexOf(c[i]);
		}
		
		return r;
	}

	public static int readHex(String s)
	{
		String hex = "0123456789ABCDEF";
		char c[] = s.toUpperCase().toCharArray();
		int r = 0;
		
		for(int i = 0 ; i < c.length ; i++)
		{
			r = (r << 4) | hex.indexOf(c[i]);
		}
		
		return r;
	}
	
	private static final Pattern 	SPACE = Pattern.compile("[\\s\\n#]+"),
									PLUS = Pattern.compile("([^!~]+)([\\+\\-]{1})(.+)"),
									TIMES = Pattern.compile("([^!~]+)([\\*\\/]{1})(.+)"),
									AOXN = Pattern.compile("([^!~]+?)([\\&\\|\\^<>]{1})(.+)"),
									PAR = Pattern.compile("\\((.*)\\)"),
									NUMBER = Pattern.compile("((?<![\\da-fA-F])(0[bodx]?[\\da-fA-F]+))");
}

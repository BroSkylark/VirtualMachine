package vm.VirtualMachine;

import java.util.ArrayList;
import java.util.List;

import vm.VirtualMachine.Assembler.Assembler;

public class StringHelper
{
	public static String[] makePresentable(String s) { return makePresentable(s, "\n"); }
	public static String[] makePresentable(String s, String c)
	{
		String l[] = s.split(c);

		for(int i = 0 ; i < l.length ; i++)
		{
			l[i] = l[i].trim();
			if(l[i].length() < 1) l[i] = null;
		}

		return l;
	}

	public static String[] removeNull(String s[])
	{
		int l = 0;
		for(int i = 0 ; i < s.length ; i++)
		{
			if(s[i] != null) l++;
		}

		String r[] = new String[l];
		for(int i = 0, j = 0 ; i < s.length ; i++)
		{
			if(s[i] != null) r[j++] = s[i];
		}

		return r;
	}

	public static String concatList(List<? extends Object> list) { return concatList(list, "\n"); }
	public static String concatList(List<? extends Object> list, String c)
	{
		String r = "";

		for(Object o : list)
		{
			r += o.toString() + c;
		}

		return r;
	}

	public static String concatList(String l[]) { return concatList(l, "\n"); }
	public static String concatList(String l[], String c)
	{
		List<String> tmp = new ArrayList<String>();
		for(int i = 0 ; i < l.length ; i++)
		{
			if(l[i] != null) tmp.add(l[i]);
		}

		return concatList(tmp, c);
	}

	public static int[] convertList(List<Integer> list)
	{
		int r[] = new int[list.size()];

		for(int i = 0 ; i < r.length ; i++)
		{
			r[i] = list.get(i).intValue();
		}

		return r;
	}
}

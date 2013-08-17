package vm.VirtualMachine.Compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vm.VirtualMachine.Start;
import vm.VirtualMachine.StringHelper;

public class ClassTree
{
	private Map<String, Class> classes;
	private Class objectClass;
	
	public ClassTree()
	{
		objectClass = new Class("Object");
		classes = new HashMap<String, Class>();
		classes.put(objectClass.name, objectClass);
	}
	
	public String formInheritanceTree(String s)
	{
		s = s.replaceAll("\\s+", " ").trim();
		String l[] = splitFile(s);
		
		for(int i = 0 ; i < l.length ; i++)
		{
			Matcher m = PTRN_CLASS.matcher(l[i]);
			while(m.find()) m = PTRN_CLASS.matcher(l[i] = instantiateClass(m));
		}
		
		return StringHelper.concatList(l, " ").replaceAll("\\s+", " ").trim();
	}
	
	public void printClass(String name)
	{
		if(classes.containsKey(name))
		{
			Start.out(classes.get(name).toString());
		}
	}
	
	// # ======================================================================================================
	
	private static Pattern PTRN_CLASS = Pattern.compile("class\\s+(\\w+)(?:\\s*\\:\\s*(\\w+))?\\s*\\{(.*)\\}"),
			PTRN_PUBLIC = Pattern.compile("public:(.*?)(?:(?=protected:|private:)|(?!.))"),
			PTRN_PROTECTED = Pattern.compile("protected:(.*?)(?:(?=private)|(?!.))"),
			PTRN_FN_DECL = Pattern.compile("(?:static\\s+)?([\\w\\*]+)\\s+(\\w+)\\(([\\w,\\s\\*]*)\\)"),
			PTRN_FD_DECL = Pattern.compile("([\\w\\*]+)\\s+(\\w+\\s*(?:,\\s*\\w+)*)");
	
	private String instantiateClass(Matcher m)
	{
		Class c = new Class(m.group(1));
		
		c.parent = m.group(2);
		if(c.parent == null) c.parent = "Object";

		if(classes.containsKey(c.name)) 
			throw new RuntimeException("ERR: Tried to declare class '" + c.name + "' which does already exist!");

		c.processBody(m.group(3));
		
		classes.put(c.name, c);
		
		return m.replaceFirst("");
	}
	
	private static String[] splitFile(String s)
	{
		int br[] = getCoords(s);
		
		String l[] = new String[br.length - 1];
		for(int i = 1 ; i < br.length ; i++)
		{
			l[i - 1] = s.substring(br[i - 1], br[i]).trim();
		}
		
		return l;
	}

	private static int[] getCoords(String s)
	{
		List<Integer> tmp = new ArrayList<Integer>();
		char t[] = s.toCharArray();
		int l = 0;
		
		tmp.add(0);
		for(int i = 0 ; i < t.length ; i++)
		{
			if(t[i] == '{')
			{
				l++;
			}
			else if(t[i] == '}')
			{
				if(--l == 0 && i + 1 < t.length) tmp.add(i + 1);
			}
		}
		tmp.add(t.length);
		
		if(l != 0) throw new RuntimeException("ERR: No matching braches! (" + l + " are missing.)");
		
		return StringHelper.convertList(tmp);
	}

	private static String[] splitCB(String cb)
	{
		String r[] = new String[] {"", "", ""};
		
		Matcher m = PTRN_PUBLIC.matcher(cb);
		while(m.find())
		{
			r[0] += m.group(1);
			m = PTRN_PUBLIC.matcher(cb = m.replaceAll(" "));
		}
		
		m = PTRN_PROTECTED.matcher(cb);
		while(m.find())
		{
			r[1] += m.group(1);
			m = PTRN_PROTECTED.matcher(cb = m.replaceAll(" "));
		}
		
		r[2] = cb.replaceFirst("private:", "");
		
		return r;
	}
	
	// # ======================================================================================================
	// # ------------------------------------------------------------------------------------------------------
	// # ======================================================================================================

	private static class Class
	{
		private String name, parent;
		private List<Method> methods;
		private List<Field> fields;
		@SuppressWarnings("unused")
		private List<Class> children;
		
		public Class(String name)
		{
			this.name = name;
			this.methods = new ArrayList<Method>();
			this.fields = new ArrayList<Field>();
			this.children = new ArrayList<Class>();
		}
		
		public void processBody(String s)
		{
			String flds[] = splitCB(s);
			
			for(int i = 0 ; i < 3 ; i++)
			{
				if(flds[i] == null || flds[i].length() < 2) continue;
				String ids[] = StringHelper.makePresentable(flds[i], ";");
				
				for(int j = 0 ; j < ids.length ; j++)
				{
					if(ids[j] == null) continue;
				
					Matcher m = PTRN_FN_DECL.matcher(ids[j]);
					if(m.find())
					{
						String p = m.group(3);
						methods.add(new Method(m.group(2), p == null ? null : 
							StringHelper.makePresentable(p, ","), AccessModifier.modifier[i], m.group(1)));
					}
					else if((m = PTRN_FD_DECL.matcher(ids[j])).find())
					{
						String t = m.group(1).trim();
						String v[] = StringHelper.makePresentable(m.group(2), ",");
						for(int k = 0 ; k < v.length ; k++)
						{
							if(v[k] == null) continue;
							fields.add(new Field(t, v[k], AccessModifier.modifier[i]));
						}
					}
				}
			}
		}
		
		@Override
		public String toString()
		{
			return "$" + parent + ":" + name + "{\n\t" + StringHelper.concatList(methods, "\n\t") + 
					"\n\t" + StringHelper.concatList(fields, "\n\t") + "};";
		}
	}
	
	private static class Method
	{
		private String name, returnType;
		private String params[];
		private AccessModifier access;
		
		public Method(String name, String params[], AccessModifier access, String returnType)
		{
			this.name = name;
			this.access = access;
			this.returnType = returnType;
			this.params = params;
		}
		
		@Override
		public String toString()
		{
			String par = "";
			
			if(params != null)
			{
				par = StringHelper.concatList(params, ", ");
				
				if(par.contains(","))
					par = par.substring(0, par.lastIndexOf(','));
			}

			return "#[" + access.toString() + "]" + returnType + ":" + name + "(" + par + ")";
		}
	}
	
	private static class Field
	{
		private String name, type;
		private AccessModifier access;
		@SuppressWarnings("unused")
		private boolean isPointer;
		
		public Field(String type, String name, AccessModifier access)
		{
			this.name = name;
			this.type = type;
			this.access = access;
			isPointer = false;
			
			if(type.contains("*"))
			{
				type = type.replaceFirst("\\*", "");
				isPointer = true;
			}
		}
		
		@Override
		public String toString()
		{
			return "@[" + access.toString() + "]" + type + ":" + name;
		}
	}
	
	private static enum AccessModifier
	{
		PUBLIC,
		PROTECTED,
		PRIVATE;
		
		public static AccessModifier modifier[] = new AccessModifier[] {PUBLIC, PROTECTED, PRIVATE};
		
		@Override
		public String toString()
		{
			return accessStrings[ordinal()];
		}
		
		private static final String accessStrings[] = new String[] {"PUBLIC", "PROTECTED", "PRIVATE"};
	}
}

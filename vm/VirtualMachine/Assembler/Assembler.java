package vm.VirtualMachine.Assembler;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vm.Debug.Logger;
import vm.VirtualMachine.MathHelper;
import vm.VirtualMachine.Start;
import vm.VirtualMachine.StringHelper;

public class Assembler
{
	private int data[];
	private String filename;

	public Assembler(String fn)
	{
		filename = fn;

		data = compile(readFile(filename));
		if(data == null) data = new int[] {0xff};
	}

	public int[] getCode() { return data; }
	public boolean wasCompileSuccessfull() { return data.length > 1; }

	public int[] compile(String s)
	{
		Logger.initializeLogger("resource/logger/00_raw.log");
		Start.out("# RAW #====================================================");
		Start.out(s);
		Start.out("# ====================================================\n\n");

		s = (new Preprocessor(filename)).process(StringHelper.makePresentable(s));
		Logger.initializeLogger("resource/logger/01_pre.log");
		Start.out("# PRE #====================================================");
		Start.out(s);
		Start.out("# ====================================================\n\n");

		s = ResolveDot.resolveDBW(s);
		Logger.initializeLogger("resource/logger/02_bd_bw.log");
		Start.out("# DB/W #====================================================");
		Start.out(s);
		Start.out("# ====================================================\n\n");

//		s = ResolveDot.resolveNumbers(s);
//		Logger.initializeLogger("resource/logger/06_numbers1.log");
//		Start.out("# NO#1 #===================================================");
//		Start.out(s);
//		Start.out("# ====================================================\n\n");

		s = ResolveDot.resolveEqu(s);
		Logger.initializeLogger("resource/logger/03_equ.log");
		Start.out("# EQU #====================================================");
		Start.out(s);
		Start.out("# ====================================================\n\n");
		
		s = MacroProcessor.resolveMacros(s);
		Logger.initializeLogger("resource/logger/04_macros.log");
		Start.out("# MAC #====================================================");
		Start.out(s);
		Start.out("# ====================================================\n\n");
		
		s = ResolveDot.resolveSymbols(s);
		Logger.initializeLogger("resource/logger/05_symbols.log");
		Start.out("# SYM #====================================================");
		Start.out(s);
		Start.out("# ====================================================\n\n");
		
		s = ResolveDot.resolveNumbers(s);
		Logger.initializeLogger("resource/logger/06_numbers.log");
		Start.out("# NO #==================================================");
		Start.out(s);
		Start.out("# ====================================================\n\n");

		String lines[] = s.split("\n");

		Logger.initializeLogger("resource/logger/07_dump.log");
		Start.out("# DUMP #====================================================");
		List<Integer> code = new ArrayList<Integer>();
		for(int i = 0 ; i < lines.length ; i++)
		{
			if(lines[i] != null)
			{
				int c[] = compileLine(lines[i]);
				
				if(c == null)
				{
					Start.out("ERR: Difficulty compiling line #" + i + ": '" + lines[i] + "'");
					continue;
				}
				
				String l = "";
				for(int j = 0 ; j < c.length ; j++)
				{
					code.add(c[j]);
					l += String.format("%02X ", c[j]);
				}
				Start.out(l);
			}
		}
		Start.out("# ====================================================\n\n");

		Start.out("");

		return StringHelper.convertList(code);
	}

	public static String readFile(String path)
	{
		String s = "";
		if(!path.startsWith("/")) path = "resource/" + path;

		try
		{
			BufferedReader br = new BufferedReader(new FileReader(path));

			while(br.ready())
			{
				s += br.readLine() + "\n";
			}

			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return s.trim();
	}
	
	public static void dumpBitecode(String path, int code[])
	{
		if(!path.startsWith("/")) path = "resource/" + path;
		
		try
		{
			FileOutputStream bw = new FileOutputStream(path);
			byte t[] = new byte[code.length];
			
			for(int i = 0 ; i < t.length ; i++) t[i] = (byte) (code[i] & 0xff);
			
			bw.write(t);
			
			bw.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static int[] compileLine(String s)
	{
		s = s.trim();

		if(s.startsWith(".db") || s.startsWith(".dw"))
		{
			boolean w = s.startsWith(".dw");
			String l[] = s.substring(3).trim().split(",");
			int r[] = new int[l.length * (w ? 2 : 1)];

			for(int i = 0 ; i < r.length / (w ? 2 : 1) ; i++)
			{
				int v = MathHelper.evaluate(l[i]);
				if(w)
				{
					r[i * 2] = (v >> 8) & 0xff;
					r[i * 2 + 1] = v & 0xff;
				}
				else
				{
					r[i] = v & 0xff;
				}
			}

			return r;
		}

		for(int i = 0 ; i < INSTRUCTIONS.length ; i++)
		{
			if(PTRN_LD_REP.matcher(s.toLowerCase()).find())
			{
				return null;
			}
			
			Matcher m = INSTRUCTIONS[i].pattern.matcher(s.toLowerCase());
			if(m.find())
			{
				List<Integer> r = new ArrayList<Integer>(m.groupCount() + 1);
				r.add(i);

				for(int j = 1 ; j <= m.groupCount() ; j++)
				{
					String g = m.group(j);
					
					if(g == null) break;

					if(g.toLowerCase().startsWith("r"))
					{
						r.add(Integer.parseInt(g.substring(1)));
					}
					else if(g.startsWith("(r"))
					{
						r.add(Integer.parseInt(g.substring(2, g.lastIndexOf(')'))));
					}
					else
					{
						int d = MathHelper.evaluate(g);

						if(d >= 0)
						{
							if(INSTRUCTIONS[i].isWord())
							{
								r.add((d >> 8) & 0xff);
								r.add(d & 0xff);
							}
							else
							{
								r.add(d);
							}
						}
					}
				}

				return StringHelper.convertList(r);
			}
		}
		
		return null;
	}

	private static final String 	RX_REGISTER = "(r\\d{1,2})", 
			RX_NUMBER = "((?:0b[01]+)|(?:0o[0-8]+)|(?:0d\\d+)|(?:0x[a-fA-F\\d]+)|(?:\\d+))",
			RX_ADDRESS = "(\\(r\\d{1,2}\\))";
	public static final Pattern 	PTRN_EQU = Pattern.compile("\\s*\\.equ\\s+([a-zA-Z_]\\w*)\\s+(\\w*)"),
			PTRN_LBL = Pattern.compile("::\\w+"),
			PTRN_VAR = Pattern.compile("(\\w+\\s+(?:\\w+,)*r?)\\w+"),
			PTRN_$ = Pattern.compile("\\$((?:\\s*[\\+\\-\\*\\/\\&\\|\\^\\!\\~]?\\s*\\(?\\s*[\\da-fA-F]+[bodh]?\\s*\\)?)*)"),
			PTRN_LD_REP = Pattern.compile("(?<!\\w)(?:ld|LD)\\s+([\\w%]+)\\s*,\\s*\\1(?!\\w)");

	public static final Instruction INSTRUCTIONS[] = new Instruction[] {
		new Instruction("NOP", null),
		(new Instruction("LD", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		new Instruction("LD", new String[] {RX_REGISTER, RX_REGISTER}),
		new Instruction("LD", new String[] {RX_REGISTER, RX_ADDRESS}),
		new Instruction("LD", new String[] {RX_ADDRESS, RX_REGISTER}),
		new Instruction("ADD", new String[] {RX_REGISTER, RX_REGISTER}),
		new Instruction("SUB", new String[] {RX_REGISTER, RX_REGISTER}),
		new Instruction("JMP", new String[] {RX_REGISTER}),
		new Instruction("JZ", new String[] {RX_REGISTER, RX_REGISTER}),
		new Instruction("AND", new String[] {RX_REGISTER, RX_REGISTER}),
		new Instruction("OR", new String[] {RX_REGISTER, RX_REGISTER}),
		new Instruction("NOT", new String[] {RX_REGISTER}),
		new Instruction("SHL", new String[] {RX_REGISTER}),
		new Instruction("SHR", new String[] {RX_REGISTER}),
		new Instruction("ROL", new String[] {RX_REGISTER}),
		new Instruction("ROR", new String[] {RX_REGISTER}),
		new Instruction("BTST", new String[] {RX_REGISTER, RX_NUMBER}),
		new Instruction("BSET", new String[] {RX_REGISTER, RX_NUMBER}),
		new Instruction("BRST", new String[] {RX_REGISTER, RX_NUMBER}),
		new Instruction("PUSH", new String[] {RX_REGISTER}),
		new Instruction("POP", new String[] {RX_REGISTER}),
		new Instruction("EI", null),
		new Instruction("DI", null),
		new Instruction("INT", new String[] {RX_NUMBER}),
		new Instruction("LDI", new String[] {RX_REGISTER}),
		new Instruction("STI", new String[] {RX_REGISTER}),
		new Instruction("INC", new String[] {RX_REGISTER}),
		new Instruction("DEC", new String[] {RX_REGISTER}),
		new Instruction("CALL", new String[] {RX_REGISTER}),
		new Instruction("RET", null),
		new Instruction("HLT", null),
		new Instruction("JP", new String[] {RX_REGISTER}),
		new Instruction("IN", new String[] {RX_REGISTER, RX_NUMBER}),
		new Instruction("OUT", new String[] {RX_NUMBER, RX_REGISTER}),
		new Instruction("SHR", new String[] {RX_REGISTER, RX_NUMBER}),
		new Instruction("SHL", new String[] {RX_REGISTER, RX_NUMBER}),
		new Instruction("MUL", new String[] {RX_REGISTER, RX_REGISTER}),
		new Instruction("DIV", new String[] {RX_REGISTER, RX_REGISTER}),
		(new Instruction("ADD", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		(new Instruction("SUB", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		(new Instruction("MUL", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		(new Instruction("DIV", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		(new Instruction("JMP", new String[] {RX_NUMBER})).makeWord(),
		(new Instruction("JZ", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		(new Instruction("CALL", new String[] {RX_NUMBER})).makeWord(),
		(new Instruction("AND", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		(new Instruction("OR", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		new Instruction("NOP", new String[] {RX_REGISTER}),
		new Instruction("LDB", new String[] {RX_REGISTER, RX_ADDRESS}),
		new Instruction("LDB", new String[] {RX_ADDRESS, RX_REGISTER}),
		new Instruction("FLSH", null),
		(new Instruction("OUT", new String[] {RX_NUMBER, RX_NUMBER})).makeWord(),
		new Instruction("XOR", new String[] {RX_REGISTER, RX_REGISTER}),
		(new Instruction("XOR", new String[] {RX_REGISTER, RX_NUMBER})).makeWord(),
		new Instruction("JFLG", new String[] {RX_NUMBER, RX_REGISTER}),
		(new Instruction("JFLG", new String[] {RX_NUMBER, RX_NUMBER})).makeWord(),
		new Instruction("ELOG", null),
		new Instruction("DLOG", null),
		new Instruction("PG", new String[] {RX_REGISTER}),
		new Instruction("PG", new String[] {RX_NUMBER}),
		new Instruction("PEEK", new String[] {RX_REGISTER})
	};

	public static class Instruction
	{
		public final Pattern pattern;
		private boolean isWord;

		public Instruction(String ID, String params[])
		{
			String s = "(?<!\\w)" + ID.toLowerCase();

			if(params != null && params.length > 0)
			{
				s += " ";
				for(int i = 0 ; i < params.length ; i++)
				{
					if(i > 0) s += "\\s*,\\s*";
					s += params[i];
				}
			}
			s += "(?![\\w,])";

			pattern = Pattern.compile(s.trim());
			isWord = false;
		}

		public Instruction makeWord() { isWord = true; return this; }
		public boolean isWord() { return isWord; }
	}
}

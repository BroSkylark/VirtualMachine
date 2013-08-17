package vm.VirtualMachine.CPU;

@SuppressWarnings("unused")
public abstract class CPU implements ICPU
{
	private static final Instruction instructions[];
	public static final int RAM_SIZE = 0x10000;
	public static final int INT_TABLE = 0x0010,
			F_HLT = 1,
			F_SIGNED = 2,
			F_OVERFLOW = 3,
			F_EI = 4,
			F_INT = 5,
			F_FLSH = 6;
	private final short memory[], registers[];
	private final Periphery periphery[];
	private long counter;

	public CPU()
	{
		periphery = new Periphery[16];
		memory = new short[RAM_SIZE];
		registers = new short[16];

		for(int i = 0 ; i < RAM_SIZE ; i++)
		{
			memory[i] = (short) 0xffff;
		}

		reset();
	}

	public void execute(IPrinter ip)
	{
		resetFlag(F_FLSH);
		if(checkFlag(F_HLT)) return;
		
		step();
		
		if((++counter & 0x000f) == 0) interrupt(0);
		
		resetFlag(F_INT);
		
		int instruction = next() & 0xffff;
		
		if(instruction < instructions.length)
		{
			instructions[instruction].print(this, ip);
			instructions[instruction].execute(this);
		}
		else
		{
			ip.print(String.format("ERR: Unrecognized instruction @0x%04X: 0x%04X", PC() - 1, instruction));
			setFlag(F_HLT);
		}
	}
	
	public boolean isHalted() { return checkFlag(F_HLT); }
	public boolean isFlushing() { return checkFlag(F_FLSH); }
	
	public abstract void loadROM();
	public void step() { }

	public void reset()
	{
		counter = 0l;

		for(int i = 0 ; i < registers.length ; i++)
		{
			registers[i] = 0x0000;
		}
		
		SP(0xffff);
	}

	private final void interrupt(int IM)
	{
		if(checkFlag(F_EI) && !checkFlag(F_INT))
		{
			setFlag(F_INT);
			push(PC());
			PC(INT_TABLE + IM);
		}
	}
	
	public void printMemory(IPrinter ip, int a1, int a2)
	{
		ip.print("# ============================================================================================");
		ip.print(String.format("# ----  Memory: 0x%04X - 0x%04X  ---------------------------" +
				"----------------------------------", a1, a2));
		ip.print("# ============================================================================================");
		ip.print("           | 0000 0001 0002 0003 0004 0005 0006 0007 0008 0009 000A 000B 000C 000D 000E 000F");
		ip.print("  ---------+----------------------------------------------------------------------------------");
		
		for(int i = a1 >> 4 ; i < (a2 + 0xf) >> 4 ; i++)
		{
			String s = String.format("  @0x%04X: | ", i << 4);
			
			for(int j = i << 4 ; j < (i + 1) << 4 ; j++)
			{
				s += j < a1 || j >= a2 ? "    " : String.format("%04X ", memory[j]);
			}
			
			ip.print(s);
		}
		
		ip.print("\n\n");
	}
	
	public void printState(IPrinter ip)
	{
		ip.print("# ====================================================");
		ip.print("# ----  Register State  ------------------------------");
		ip.print("# ====================================================");
		ip.print(String.format("     BC: 0x%04X    A: 0x%04X   SP: 0x%04X", BC(), A(), SP()));
		ip.print(String.format("     HL: 0x%04X    F: 0x%04X   PC: 0x%04X", HL(), F(), PC()));
		ip.print("  ----------------------------------------------------");
		
		for(int i = 0 ; i < 4 ; i++)
		{
			String s = "    ";
			for(int j = 0 ; j < 4 ; j++)
			{
				s += i * 4 + j < 10 ? " r" : "r";
				s += String.format("%d: 0x%04X  ", i * 4 + j, registers[i * 4 + j]);
			}
			ip.print(s);
		}
		
		ip.print("\n\n");
	}

	@Override
	public final void writeMemory(int addr, int src[])
	{
		for(int i = 0 ; i < src.length ; i++)
		{
			if(addr + i >= RAM_SIZE)
			{
				(new RuntimeException("SEVERE: tried to write over RAM boundaries.")).printStackTrace();
				break;
			}
			
			poke(addr + i, src[i]);
		}
	}
	
	@Override
	public final int[] readMemory(int addr, int length)
	{
		int data[] = new int[length];
		
		for(int i = 0 ; i < length ; i++)
		{
			if(addr + i >= RAM_SIZE)
			{
				(new RuntimeException("SEVERE: tried to read over RAM boundaries.")).printStackTrace();
				break;
			}
			
			data[i] = peek(addr + i);
		}
		
		return data;
	}
	
	@Override
	public void attachPeriphery(Periphery p)
	{
		if(hasPeriphery(p)) return;
		
		for(int i = 0 ; i < periphery.length ; i++)
		{
			if(periphery[i] == null)
			{
				periphery[i] = p;
				p.attachToCPU(this, i);
				return;
			}
		}
	}
	
	@Override
	public void detachPeriphery(Periphery p)
	{
		if(!hasPeriphery(p)) return;
		
		for(int i = 0 ; i < periphery.length ; i++)
		{
			if(p.equals(periphery[i])) periphery[i] = null;
		}
	}
	
	public final boolean hasPeriphery(Periphery p)
	{
		for(int i = 0 ; i < periphery.length ; i++)
		{
			if(p.equals(periphery[i])) return true;
		}
		
		return false;
	}
	
	private final boolean checkFlag(int f) { return checkFlag(F(), f); }
	private final boolean checkFlag(int v, int f)
	{
		return (v & F_OR[f]) != 0;
	}
	
	private final void setFlag(boolean s, int f) { if(s) setFlag(f); else resetFlag(f); }
	private final int setFlag(boolean s, int v, int f) { return s ? setFlag(v, f) : resetFlag(v, f); }
	private final void setFlag(int f) { F(setFlag(F(), f)); }
	private final int setFlag(int v, int f) { return v | F_OR[f]; }
	private final void resetFlag(int f) { F(resetFlag(F(), f)); }
	private final int resetFlag(int v, int f) { return v & F_AND[f]; }
	
	private final void poke(int a, int v)
	{
		memory[a] = (short) (v & 0xffff);
	}
	
	private final int peek(int a)
	{
		return checkFlag(F_SIGNED) ? (int)memory[a] : ((int)memory[a] & 0xffff);
	}
	
	private final int next()
	{
		int v = peek(PC());
		PC(PC() + 1);
		return v;
	}
	
	private final void pushR(int r) { push(getR(r)); }
	private final void push(int v)
	{
		SP(SP() - 1);
		poke(SP(), v);
	}
	
	private final int pop()
	{
		int v = peek(SP());
		SP(SP() + 1);
		return v;
	}

	private final int getR(int r) 
	{ 
		return checkFlag(registers[13], F_SIGNED) ? (int)registers[r] : (int)registers[r] & 0xffff;
	}
	private final void setR(int r, int v)
	{
		registers[r] = (short) (v & 0xffff);
	}

	private final int BC() { return getR(10); }
	private final void BC(int v) { setR(10, v); }
	private final int HL() { return getR(11); }
	private final void HL(int v) { setR(11, v); }
	private final int A() { return getR(12); }
	private final void A(int v) { setR(12, v); }
	private final int F() { return getR(13); }
	private final void F(int v) { setR(13, v); }
	private final int SP() { return getR(14); }
	private final void SP(int v) { setR(14, v); }
	private final int PC() { return getR(15); }
	private final void PC(int v) { setR(15, v); }
	
	public static void printInstructionList(IPrinter ip)
	{
		for(Instruction i : instructions)
		{
			String s = String.format("0x%04X: %s      ", i.ID, i.name.toUpperCase()).substring(0, 14);
			
			for(int j = 0 ; j < 2 ; j++)
			{
				switch(i.params[j])
				{
					case NONE:
						break;
					case REGISTER:
						s += " rX ";
						break;
					case MEMORY:
						s += "(rX)";
						break;
					case CONST:
						s += "  C ";
						break;
				}
				
				if(j == 0 && i.params[1] != ParamType.NONE) s += ", ";
			}
			
			ip.print(s);
		}
	}
	
	// TODO # ==================================================================================================
	
	private static int c = 0;
	private static final int 
			ID_HLT		= c++,
			ID_NOP		= c++,
			ID_LD_XY	= c++,
			ID_LD_XC	= c++,
			ID_LD_XI	= c++,
			ID_LD_IX	= c++,
			ID_JMP_C	= c++,
			ID_JZ_XC	= c++,
			ID_CALL_C	= c++,
			ID_CLLZ_XC	= c++,
			ID_RET		= c++,
			ID_CMP_XY	= c++,
			ID_CMP_XC	= c++,
			ID_BTST_XC	= c++,
			ID_BSET_XC	= c++,
			ID_BRST_XC	= c++,
			ID_INC_X	= c++,
			ID_DEC_X	= c++,
			ID_ADD_XY	= c++,
			ID_SUB_XY	= c++,
			ID_MUL_XY	= c++,
			ID_DIV_XY	= c++,
			ID_NOT_X	= c++,
			ID_AND_XY	= c++,
			ID_OR_XY	= c++,
			ID_XOR_XY	= c++,
			ID_SHL_XC	= c++,
			ID_SHR_XC	= c++;
	
	static
	{
		instructions = new Instruction [c];
		new Instruction(ID_HLT, "hlt", ParamType.NONE, ParamType.NONE)
		{
			@Override
			public void execute(CPU cpu)
			{
				cpu.setFlag(F_HLT);
			}
		};
		new Instruction(ID_NOP, "nop", ParamType.NONE, ParamType.NONE)
		{
			@Override
			public void execute(CPU cpu)
			{
			}
		};
		new Instruction(ID_LD_XY, "ld", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r1 = cpu.next();
				int r2 = cpu.next();

				cpu.setR(r1, cpu.getR(r2));
			}
		};
		new Instruction(ID_LD_XC, "ld", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v = cpu.next();

				cpu.setR(r, v);
			}
		};
		new Instruction(ID_LD_XI, "ld", ParamType.REGISTER, ParamType.MEMORY)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r1 = cpu.next();
				int r2 = cpu.next();

				cpu.setR(r1, cpu.peek(cpu.getR(r2)));
			}
		};
		new Instruction(ID_LD_IX, "ld", ParamType.MEMORY, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r1 = cpu.next();
				int r2 = cpu.next();

				cpu.poke(cpu.getR(r1), cpu.getR(r2));
			}
		};
		new Instruction(ID_JMP_C, "jmp", ParamType.CONST, ParamType.NONE)
		{
			@Override
			public void execute(CPU cpu)
			{
				int v = cpu.next();

				cpu.PC(v);
			}
		};
		new Instruction(ID_JZ_XC, "jz", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v = cpu.next();

				if(cpu.getR(r) == 0)
				{
					cpu.PC(v);
				}
			}
		};
		new Instruction(ID_CALL_C, "call", ParamType.CONST, ParamType.NONE)
		{
			@Override
			public void execute(CPU cpu)
			{
				int v = cpu.next();

				cpu.push(cpu.PC());
				cpu.PC(v);
			}
		};
		new Instruction(ID_CLLZ_XC, "cllz", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v = cpu.next();

				if(cpu.getR(r) == 0)
				{
					cpu.push(cpu.PC());
					cpu.PC(v);
				}
			}
		};
		new Instruction(ID_RET, "ret", ParamType.NONE, ParamType.NONE)
		{
			@Override
			public void execute(CPU cpu)
			{
				cpu.PC(cpu.pop());
			}
		};
		new Instruction(ID_CMP_XY, "cmp", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int v1 = cpu.getR(cpu.next());
				int v2 = cpu.getR(cpu.next());

				int r = 0;
				if(v1 == v2) r |= 1;
				if(v1 < v2) r |= 2;
				if(v1 > v2) r |= 4;
				if(v1 <= v2) r |= 8;
				if(v2 >= v2) r |= 16;

				cpu.A(r);
			}
		};
		new Instruction(ID_CMP_XC, "cmp", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int v1 = cpu.getR(cpu.next());
				int v2 = cpu.next();

				int r = 0;
				if(v1 == v2) r |= 1;
				if(v1 < v2) r |= 2;
				if(v1 > v2) r |= 4;
				if(v1 <= v2) r |= 8;
				if(v2 >= v2) r |= 16;

				cpu.A(r);
			}
		};
		new Instruction(ID_BTST_XC, "btst", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int v1 = cpu.getR(cpu.next());
				int v2 = cpu.next();

				cpu.A((v1 & F_OR[v2]) != 0 ? 1 : 0);
			}
		};
		new Instruction(ID_BSET_XC, "bset", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v = cpu.next();

				cpu.setR(r, cpu.getR(r) | F_OR[v]);
			}
		};
		new Instruction(ID_BRST_XC, "brst", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v = cpu.next();

				cpu.setR(r, cpu.getR(r) & F_AND[v]);
			}
		};
		new Instruction(ID_INC_X, "inc", ParamType.REGISTER, ParamType.NONE)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v = cpu.getR(r) + 1;

				cpu.setFlag((v == 0x8000  &&  cpu.checkFlag(F_SIGNED)) || 
						(v == 0x10000 && !cpu.checkFlag(F_SIGNED)), F_OVERFLOW);

				cpu.setR(r, v);
			}
		};
		new Instruction(ID_DEC_X, "dec", ParamType.REGISTER, ParamType.NONE)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v = cpu.getR(r) - 1;

				cpu.setFlag((v == 0x7fff &&  cpu.checkFlag(F_SIGNED)) || 
						(v == 0xffff && !cpu.checkFlag(F_SIGNED)), F_OVERFLOW);

				cpu.setR(r, v);
			}
		};
		new Instruction(ID_ADD_XY, "add", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v1 = cpu.getR(r);
				int v2 = cpu.getR(cpu.next());

				v1 += v2;

				cpu.setFlag((v1 < -0x7ffff || v2 > 0x8000) && cpu.checkFlag(F_SIGNED) ||
						v1 > 0x10000 && !cpu.checkFlag(F_SIGNED), F_OVERFLOW);

				cpu.setR(r, v1);
			}
		};
		new Instruction(ID_SUB_XY, "sub", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v1 = cpu.getR(r);
				int v2 = cpu.getR(cpu.next());

				v1 -= v2;

				cpu.setFlag((v1 < -0x7ffff || v2 > 0x8000) && cpu.checkFlag(F_SIGNED) ||
						v1 > 0x10000 && !cpu.checkFlag(F_SIGNED), F_OVERFLOW);

				cpu.setR(r, v1);
			}
		};
		new Instruction(ID_MUL_XY, "mul", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v1 = cpu.getR(r);
				int v2 = cpu.getR(cpu.next());

				v1 *= v2;

				cpu.resetFlag(F_OVERFLOW);

				cpu.setR(r, v1);
				cpu.A(v1 >> 16);
			}
		};
		new Instruction(ID_DIV_XY, "div", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int v1 = cpu.getR(r);
				int v2 = cpu.getR(cpu.next());

				cpu.resetFlag(F_OVERFLOW);

				cpu.setR(r, v1 / v2);
				cpu.A(v1 % v2);
			}
		};
		new Instruction(ID_NOT_X, "not", ParamType.REGISTER, ParamType.NONE)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();

				cpu.setR(r, ~cpu.getR(r));
			}
		};
		new Instruction(ID_AND_XY, "and", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r1 = cpu.next();
				int r2 = cpu.next();

				cpu.setR(r1, cpu.getR(r1) & cpu.getR(r2));
			}
		};
		new Instruction(ID_OR_XY, "or", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r1 = cpu.next();
				int r2 = cpu.next();

				cpu.setR(r1, cpu.getR(r1) | cpu.getR(r2));
			}
		};
		new Instruction(ID_XOR_XY, "xor", ParamType.REGISTER, ParamType.REGISTER)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r1 = cpu.next();
				int r2 = cpu.next();

				cpu.setR(r1, cpu.getR(r1) ^ cpu.getR(r2));
			}
		};
		new Instruction(ID_SHL_XC, "shl", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int c = cpu.next();

				cpu.setR(r, cpu.getR(r) << c);
			}
		};
		new Instruction(ID_SHR_XC, "shr", ParamType.REGISTER, ParamType.CONST)
		{
			@Override
			public void execute(CPU cpu)
			{
				int r = cpu.next();
				int c = cpu.next();

				cpu.setR(r, cpu.getR(r) >> c);
			}
		};
	}
	
	// TODO # ============================================================================================
	
	private static abstract class Instruction
	{
		public final int ID;
		private ParamType params[];
		private String name;
		
		public Instruction(int id, String s, ParamType p1, ParamType p2)
		{
			ID = id;
			name = s.toUpperCase().trim();
			params = new ParamType[2];
			params[0] = p1 == null ? ParamType.NONE : p1;
			params[1] = p2 == null ? ParamType.NONE : p2;
			
			instructions[ID] = this;
		}
		
		public void print(CPU cpu, IPrinter ip)
		{
			String s = String.format("@0x%04X: " + name + " ", cpu.PC() - 1);
			
			for(int i = 0 ; i < 2 ; i++)
			{
				switch(params[i])
				{
					case NONE:
						
						break;
					case REGISTER:
						s += String.format("r%d", cpu.peek(cpu.PC() + i));
						break;
					case MEMORY:
						s += String.format("(r%d)", cpu.peek(cpu.PC() + i));
						break;
					case CONST:
						s += String.format("0x%04X", cpu.peek(cpu.PC() + i));
						break;
				}
			
				if(i == 0 && params[1] != ParamType.NONE) s += ",";
			}
			
			ip.print(s);
		}
		
		public abstract void execute(CPU cpu);
	}
	
	private static enum ParamType
	{
		NONE,
		REGISTER,
		MEMORY,
		CONST;
	}

	private static final int 
		F_OR[] =  {	0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080, 
					0x0100, 0x0200, 0x0400, 0x0800, 0x1000, 0x2000, 0x4000, 0x8000	},
		F_AND[] = {	0xFFFE, 0xFFFD, 0xFFFB, 0xFFF7, 0xFFEF, 0xFFDF, 0xFFBF, 0xFF7F,
					0xFEFF, 0xFDFF, 0xFBFF, 0xF7FF, 0xEFFF, 0xDFFF, 0xBFFF, 0x7FFF	};
}

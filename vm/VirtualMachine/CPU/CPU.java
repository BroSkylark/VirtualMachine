package vm.VirtualMachine.CPU;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vm.Debug.Logger;
import vm.VirtualMachine.Start;

public abstract class CPU implements ICPU
{
	private static final int RAM_SIZE = 0x10000;
	private static final int IA_TABLE = 0x10,
								F_OVERFLOW = 1,
								F_ZERO = 2,
								F_SIGNED = 3,
								F_ENABLE_PAGE = 4,
								F_FORCE_PAGE = 5,
								F_INTERRUPT = 6;
	private final int memory[];
	private final int registers[], sp[];
	private int PC, IM;
	private boolean EI, isHalted, isFlushing, isLogging;
	private long counter;
	private Periphery periphery[];

	public CPU()
	{
		periphery = new Periphery[16];
		memory = new int[RAM_SIZE];
		registers = new int[0x10];
		sp = new int[4];
		for(int i = 0 ; i < RAM_SIZE ; i++)
		{
			memory[i] = 0xffffffff;
		}
		reset();
	}
	
	public final void reset()
	{
		for(int i = 0 ; i < 16 ; i++)
		{
			setR(i, 0);
			periphery[i] = null;
		}
		
		PC = 0;
		IM = 0;
		EI = false;
		isHalted = false;
		isFlushing = false;
		isLogging = false;
		PP(0);
		sp[0] = sp[1] = sp[2] = sp[3] = RAM_SIZE - 1;
	}
	
	private final int getMemP(int i, int p)
	{ 
		return (((memory[i] << (p * 8)) & 0xff) >> (p * 8));
	}
	
	private final void setMemP(int i, int p, int v)
	{ 
		memory[i] &= (0xffffffff ^ (0xff << (p * 8)));
		memory[i] |= (v & 0xff) << (p * 8);
	}
	
	public final boolean isStopped()
	{
		return isHalted;
	}
	
	public final boolean isFlushing()
	{
		return isFlushing;
	}
	
	protected void step() { }
	protected abstract void loadROM();
	
	public final void execute()
	{
		isFlushing = false;
		if(isHalted) return;
		
		step();
		
		counter++;
		
		if((counter & 15) == 0) interrupt(0);
		
		if(checkFlag(F_INTERRUPT)) executeInterrupt();
		
		int instruction = peekb(PC, checkFlag(F_FORCE_PAGE) ? 0 : getPage());
		PC(PC + 1);
		
		String log = String.format("Executing @(0x%02X):", PC - 1) + (instruction < INSTRUCTIONS.length ?
				INSTRUCTIONS[instruction] : String.format("0x%02X: UNKNOWN INSTRUCTION", instruction));
		
		// TODO
		switch(instruction)
		{
		case 0x00: nop();		break;
		case 0x01: ld_xc();		break;
		case 0x02: ld_xy();		break;
		case 0x03: ld_xi();		break;
		case 0x04: ld_ix();		break;
		case 0x05: add();		break;
		case 0x06: sub();		break;
		case 0x07: jmp();		break;
		case 0x08: jz();		break;
		case 0x09: and();		break;
		case 0x0A: or();		break;
		case 0x0B: not();		break;
		case 0x0C: shl();		break;
		case 0x0D: shr();		break;
		case 0x0E: rol();		break;
		case 0x0F: ror();		break;
		case 0x10: bchk();		break;
		case 0x11: bset();		break;
		case 0x12: brst();		break;
		case 0x13: push_x();	break;
		case 0x14: pop_x();		break;
		case 0x15: ei();		break;
		case 0x16: di();		break;
		case 0x17: int_c();		break;
		case 0x18: ldi();		break;
		case 0x19: sti();		break;
		case 0x1A: inc();		break;
		case 0x1B: dec();		break;
		case 0x1C: call();		break;
		case 0x1D: ret();		break;
		case 0x1E: hlt();		break;
		case 0x1F: jp();		break;
		case 0x20: in();		break;
		case 0x21: out();		break;
		case 0x22: shr_n();		break;
		case 0x23: shl_n();		break;
		case 0x24: mul();		break;
		case 0x25: div();		break;
		case 0x26: add_n();		break;
		case 0x27: sub_n();		break;
		case 0x28: mul_n();		break;
		case 0x29: div_n();		break;
		case 0x2A: jmp_a();		break;
		case 0x2B: jz_a();		break;
		case 0x2C: call_a();	break;
		case 0x2D: and_n();		break;
		case 0x2E: or_n();		break;
		case 0x2F: nop_n();		break;
		case 0x30: ldb_xi();	break;
		case 0x31: ldb_ix();	break;
		case 0x32: flsh();		break;
		case 0x33: out_n();		break;
		case 0x34: xor();		break;
		case 0x35: xor_c();		break;
		case 0x36: jflg();		break;
		case 0x37: jflg_n();	break;
		case 0x38: elog();		break;
		case 0x39: dlog();		break;
		case 0x3A: pg_x();		break;
		case 0x3B: pg_c();		break;
		case 0x3C: peek();		break;
		default:
			Start.out(String.format("ERR: Unrecognized instruction: 0x%02X", instruction));
			isHalted = true;
			break;
		}

		if(isLogging)
		{
			Start.out(log);
			printState(Start.printer);
		}
	}
	
	public void printState(IPrinter ip)
	{
		ip.print("# =======================================");
		ip.print(String.format("PC: 0x%04X\t\tSP: 0x%04X", PC, SP()));
		ip.print(String.format(" F: 0x%04X\t\t A: 0x%04X", F(), A()));
		ip.print(String.format("BC: 0x%04X\t\tHL: 0x%04X", BC(), HL()));
		ip.print("# ---------------------------------------");
		for(int i = 0 ; i < 11 ; i += 2)
		{
			ip.print(String.format("\tr%02d: 0x%04X\t\tr%02d: 0x%04X", i, getR(i), i + 1, getR(i + 1)));
		}
		ip.print("# ---------------------------------------");
		
		ip.print("");
	}

	public void printMemory(IPrinter ip, int sO, int eO) { printMemory(ip, sO, eO, getPage()); }
	public void printMemory(IPrinter ip, int sO, int eO, int pg)
	{
		ip.print("# =======================================");
		ip.print(				"# -----------  Memory Dump --------------");
		ip.print(String.format(	"# -----------  0x%04X - 0x%04X ----------", sO, eO));
		ip.print("# =======================================");
		ip.print("        | 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f");
		ip.print("--------+------------------------------------------------");
		
		for(int p = (sO - (sO % 16)) / 16 ; p < (eO - (eO % 16)) / 16 + (eO % 16 == 0 ? 0 : 1) ; p++)
		{
			String s = String.format("@0x%04X | ", p * 16);
			for(int i = p * 16 ; i < (p + 1) * 16 ; i++)
			{
				s += i < sO || i >= eO ? "   " : String.format("%02X ", getMemP(i, pg));
			}
			ip.print(s);
		}
		
		ip.print("# =======================================");
	}
	
	private int getPage() { return checkFlag(F_ENABLE_PAGE) ? PP() : 0; }
	
	@Override
	public final void writeMemory(int addr, int src[]) { writeMemory(addr, src, getPage()); }
	public final void writeMemory(int addr, int src[], int p)
	{
		for(int i = 0 ; i < src.length ; i++)
		{
			if(addr + i >= RAM_SIZE)
			{
				(new RuntimeException("SEVERE: tried to write over page boundaries.")).printStackTrace();
				break;
			}
			setMemP(addr + i, p, src[i]);
		}
	}
	
	@Override
	public final int[] readMemory(int addr, int length) { return readMemory(addr, length, getPage()); }
	public final int[] readMemory(int addr, int length, int p)
	{
		int data[] = new int[length];
		
		for(int i = 0 ; i < length ; i++)
		{
			if(addr + i >= RAM_SIZE)
			{
				(new RuntimeException("SEVERE: tried to read over page boundaries.")).printStackTrace();
				break;
			}
			data[i] = getMemP(addr + i, p);
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
	
	// TODO #=============================================================================================
	
	private final void nop()
	{
	}
	
	private final void ld_xc()
	{
		int r = peekb(PC);
		int v = peekw(PC + 1);
		PC(PC + 3);
		
		setR(r, v);
	}
	
	private final void ld_xy()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r1, getR(r2));
	}
	
	private final void ld_xi()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		int p = getPage();
		int v = peekw(getR(r2), p);
		setR(r1, v);
	}
	
	private final void ld_ix()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		int p = getPage();
		PC(PC + 2);
		
		pokew(getR(r2), getR(r1), p);
	}
	
	private final void add()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);

		setR(r1, add_impl(getR(r1), getR(r2)));
	}
	
	private final void sub()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r1, sub_impl(getR(r1), getR(r2)));
	}
	
	private final void jmp()
	{
		int r = peekb(PC);
		PC(getR(r));
	}
	
	private final void jz()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		if(getR(r1) == 0)
		{
			PC(getR(r2));
		}
	}
	
	private final void and()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r1, getR(r1) & getR(r2));
	}
	
	private final void or()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r1, getR(r1) | getR(r2));
	}
	
	private final void not()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		setR(r, (~getR(r)) & 0xffff);
	}
	
	private final void shl()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		setR(r, getR(r) << 1);
	}
	
	private final void shr()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		setR(r, getR(r) >> 1);
	}
	
	private final void rol()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		int v = getR(r);
		int b = (v >> 15) & 1;
		
		setR(r, (v << 1) | b);
	}
	
	private final void ror()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		int v = getR(r);
		int b = v & 1;
		
		setR(r, (v >> 1) | (b << 15));
	}
	
	private final void bchk()
	{
		int r = peekb(PC);
		int f = peekb(PC + 1);
		PC(PC + 2);
		
		A((getR(r) & F_SET[f]) >> f);
	}
	
	private final void bset()
	{
		int r = peekb(PC);
		int f = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r, getR(r) | F_SET[f]);
	}
	
	private final void brst()
	{
		int r = peekb(PC);
		int f = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r, getR(r) & F_RST[f]);
	}
	
	private final void push_x()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		push(getR(r));
	}
	
	private final void pop_x()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		setR(r, pop());
	}
	
	private final void ei()
	{
		EI = true;
	}
	
	private final void di()
	{
		EI = false;
	}

	private final void int_c()
	{
		int i = peekb(PC);
		PC(PC + 1);
		
		interrupt(i);
	}
	
	private final void ldi()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		setR(r, peekw(IA_TABLE + IM));
	}
	
	private final void sti()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		pokew(getR(r), IA_TABLE + IM);
	}
	
	private final void inc()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		setR(r, add_impl(getR(r), 1));
	}
	
	private final void dec()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		setR(r, sub_impl(getR(r), 1));
	}
	
	private final void call()
	{
		int r = peekb(PC);
		
		push(PC + 1);
		PC(getR(r));
	}
	
	private final void ret()
	{
		PC(pop());
	}
	
	private final void hlt()
	{
		isHalted = true;
	}
	
	private final void jp()
	{
		int r = peekb(PC);
		int v = getR(r);
		if(checkFlag(F_SIGNED)) v = -((~(v - 1)) & 0xffff);
		
		PC(PC - 1 + v);
	}
	
	private final void in()
	{
		int r = peekb(PC);
		int p = peekb(PC + 1);
		PC(PC + 2);
		
		int v = 0xffff;
		if(periphery[p] != null)
		{
			v = periphery[p].retrieveWord();
		}
		
		setR(r, v);
	}
	
	private final void out()
	{
		int p = peekb(PC);
		int r = peekb(PC + 1);
		PC(PC + 2);
		
		if(periphery[p] != null)
		{
			periphery[p].acceptWord(getR(r));
		}
	}
	
	private final void shr_n()
	{
		int r = peekb(PC);
		int n = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r, getR(r) >> n);
	}
	
	private final void shl_n()
	{
		int r = peekb(PC);
		int n = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r, getR(r) << n);
	}
	
	private final void mul()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r1, mul_impl(getR(r1), getR(r2)));
	}
	
	private final void div()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r1, div_impl(getR(r1), getR(r2)));
	}

	private final void add_n()
	{
		int r = peekb(PC);
		int n = peekw(PC + 1);
		PC(PC + 3);
		
		setR(r, add_impl(getR(r), n));
	}
	
	private final void sub_n()
	{
		int r = peekb(PC);
		int n = peekw(PC + 1);
		PC(PC + 3);
		
		setR(r, sub_impl(getR(r), n));
	}

	private final void mul_n()
	{
		int r = peekb(PC);
		int n = peekw(PC + 1);
		PC(PC + 3);
		
		setR(r, mul_impl(getR(r), n));
	}
	
	private final void div_n()
	{
		int r = peekb(PC);
		int n = peekw(PC + 1);
		PC(PC + 3);
		
		setR(r, div_impl(getR(r), n));
	}
	
	private final void jmp_a()
	{
		PC(peekw(PC));
	}
	
	private final void jz_a()
	{
		int r = peekb(PC);
		int a = peekw(PC + 1);
		PC(PC + 3);
		
		if(getR(r) == 0)
		{
			PC(a);
		}
	}
	
	private final void call_a()
	{
		push(PC + 2);
		PC(peekw(PC));
	}
	
	private final void and_n()
	{
		int r = peekb(PC);
		int n = peekw(PC + 1);
		PC(PC + 3);
		
		setR(r, getR(r) & n);
	}
	
	private final void or_n()
	{
		int r = peekb(PC);
		int n = peekw(PC + 1);
		PC(PC + 3);
		
		setR(r, getR(r) | n);
	}
	
	private final void nop_n()
	{
		int v = peekb(PC);
		
		if(v > 0)
		{
			pokeb(v - 1, PC);
			PC(PC - 1);
		}
		else
		{
			PC(PC + 1);
		}
	}

	private final void ldb_xi()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		int p = getPage();
		int v = peekb(getR(r2), p);
		setR(r1, v);
	}
	
	private final void ldb_ix()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		int p = getPage();
		PC(PC + 2);
		
		pokeb(getR(r2), getR(r1), p);
	}
	
	private final void flsh()
	{
		isFlushing = true;
	}
	
	private final void out_n()
	{
		int p = peekw(PC);
		int v = peekw(PC + 2);
		PC(PC + 4);

		if(periphery[p] != null)
		{
			periphery[p].acceptWord(v);
		}
	}
	
	private final void xor()
	{
		int r1 = peekb(PC);
		int r2 = peekb(PC + 1);
		PC(PC + 2);
		
		setR(r1, getR(r1) ^ getR(r2));
	}
	
	private final void xor_c()
	{
		int r = peekb(PC);
		int v = peekw(PC + 1);
		PC(PC + 3);
		
		setR(r, getR(r) ^ v);
	}
	
	private final void jflg()
	{
		int f = peekb(PC);
		int r = peekb(PC + 1);
		PC(PC + 2);
		
		if(checkFlag(f))
		{
			PC(getR(r));
		}
	}
	
	private final void jflg_n()
	{
		int f = peekw(PC);
		int a = peekw(PC + 2);
		PC(PC + 4);
		
		if(checkFlag(f))
		{
			PC(a);
		}
	}
	
	private final void elog()
	{
		isLogging = true;
		Start.out("### ENABLE LOGGING!");
	}
	
	private final void dlog()
	{
		isLogging = false;
		Start.out("### DISABLE LOGGING!");
	}
	
	private final void pg_x()
	{
		int r = peekb(PC);
		PC(PC + 1);
		
		PP(getR(r));
	}
	
	private final void pg_c()
	{
		int v = peekb(PC);
		PC(PC + 1);
		
		PP(v);
	}
	
	private final void retp()
	{
		PC(pop());
		if(checkFlag(F_ENABLE_PAGE)) PP(pop());
	}
	
	private final void peek()
	{
		int r = peekb(PC);
		PC(PC + 1);
		setR(r, peekw(SP()));
	}
	
	// TODO #====================================================================================================
	
	private final void setFlag(boolean b, int f) { if(b) setFlag(f); else resetFlag(f); }
	private final void setFlag(int f)
	{
		F(F() | F_SET[f]);
	}
	
	private final void resetFlag(int f)
	{
		F(F() & F_RST[f]);
	}
	
	private final boolean checkFlag(int f)
	{
		return (F() & F_SET[f]) != 0;
	}
	
	private final void executeInterrupt()
	{
		resetFlag(F_INTERRUPT);
		if(checkFlag(F_ENABLE_PAGE))
		{
			int pp = PP();
			PP(0);
			push(pp);
		}
		push(PC);
		PC(peekw(IA_TABLE + IM * 2));
	}
	
	public final void interrupt(int im)
	{
		if(EI && !checkFlag(F_INTERRUPT))
		{
			IM = im;
			setFlag(F_INTERRUPT);
		}
	}

	private final int add_impl(int x, int y)
	{
		int z = x + y;
		
		if(!checkFlag(F_SIGNED))
		{
			setFlag((z & 0xffff0000) != 0, F_OVERFLOW);
		}
		else
		{
			if((x & (1 << 15)) != 0) x -= 0x10000;
			if((y & (1 << 15)) != 0) y -= 0x10000;
			
			z = x + y;
			
			setFlag(z > 0x7fff || z < -0x8000, F_OVERFLOW);
		}
		
		resetFlag(F_ZERO);
		
		return z & 0xffff;
	}
	
	private final int sub_impl(int x, int y)
	{
		int z = x - y;
		
		if(!checkFlag(F_SIGNED))
		{
			setFlag((z & 0xffff0000) != 0, F_OVERFLOW);
		}
		else
		{
			if((x & (1 << 15)) != 0) x -= 0x10000;
			if((y & (1 << 15)) != 0) y -= 0x10000;
			
			z = x - y;
			
			setFlag(z > 0x7fff || z < -0x8000, F_OVERFLOW);
		}
		
		resetFlag(F_ZERO);
		
		return z & 0xffff;
	}
	
	private final int mul_impl(int x, int y)
	{
		int z = x * y;

		if(!checkFlag(F_SIGNED))
		{
			setFlag((z & 0xffff0000) != 0, F_OVERFLOW);
		}
		else
		{
			if((x & (1 << 15)) != 0) x -= 0x10000;
			if((y & (1 << 15)) != 0) y -= 0x10000;
			
			z = x * y;
			
			setFlag(z > 0x7fff || z < -0x8000, F_OVERFLOW);
		}
		
		resetFlag(F_ZERO);
		A((z >> 16) & 0xffff);
		
		return z & 0xffff;
	}
	
	private final int div_impl(int x, int y)
	{
		if(checkFlag(F_SIGNED))
		{
			if((x & (1 << 15)) != 0) x -= 0x10000;
			if((y & (1 << 15)) != 0) y -= 0x10000;
		}

		int m = x % y;
		int d = x / y;
		
		resetFlag(F_OVERFLOW);
		resetFlag(F_ZERO);
		A(m & 0xffff);
		
		return d & 0xffff;
	}
	
	private final void pushR(int r) { push(getR(r)); }
	private final void push(int v)
	{
		SP(SP() - 2);
		pokew(v, SP());
	}
	
	private final void popR(int r) { setR(r, pop()); }
	private final int pop()
	{
		int v = peekw(SP());
		SP(SP() + 2);
		return v;
	}

	private final int peekb(int addr) { return peekb(addr, 0); }
	private final int peekb(int addr, int page)
	{
		return getMemP(addr, page);//memory[page][addr];
	}

	private final int peekw(int addr) { return peekw(addr, 0); }
	private final int peekw(int addr, int page)
	{
		return (peekb(addr, page) << 8) | peekb(addr + 1, page);
	}

	private final void pokeb(int v, int addr) { pokeb(v, addr, 0); }
	private final void pokeb(int v, int addr, int page)
	{
		setMemP(addr, page, v);
//		memory[page][addr] = v & 0xff;
	}

	private final void pokew(int v, int addr) { pokew(v, addr, 0); }
	private final void pokew(int v, int addr, int page)
	{
		setMemP(addr + 1, page, v);
		setMemP(addr, page, v >> 8);
//		memory[page][addr + 1] = v & 0xff;
//		memory[page][addr] = (v >> 8) & 0xff;
	}

	private final void PC(int pc) { PC = pc < 0 ? 0 : pc & 0xffff; }
	private final int getR(int r) { return r == 12 ? SP() : registers[r]; }
	private final void setR(int r, int v) { if(r == 12) { SP(v); } else { registers[r] = v & 0xffff; } }
	private final int PP() { return registers[12] & 3; }
	private final void PP(int pp) { registers[12] = pp & 3; }
	private final int A() { return getR(11); }
	private final void A(int a) { setR(11, a); }
	private final int SP() { return sp[getPage()]; }
	private final void SP(int sp) { this.sp[getPage()] = sp; }
	private final int BC() { return getR(13); }
	private final void BC(int bc) { setR(13, bc); }
	private final int HL() { return getR(14); }
	private final void HL(int hl) { setR(14, hl); }
	private final int F() { return getR(15); }
	private final void F(int f) { setR(15, f); }
	
	private static final int	F_SET[] = new int[] {1, 2, 4, 8, 16, 32, 64, 128},
								F_RST[] = new int[] {254, 253, 251, 247, 239, 223, 191, 127};
	
	// TODO
	private static final String INSTRUCTIONS[] = new String[] {
		"0x00: NOP           ", "0x01: LD    rX ,  c ", "0x02: LD    rX , rY ", "0x03: LD    rX ,(rY)", 
		"0x04: LD   (rX), rY ", "0x05: ADD   rX , rY ", "0x06: SUB   rX , rY ", "0x07: JMP   rX      ",
		"0x08: JZ    rX , rY ", "0x09: AND   rX , rY ", "0x0A: OR    rX , rY ", "0x0B: NOT   rX      ",
		"0x0C: SHL   rX      ", "0x0D: SHR   rX      ", "0x0E: ROL   rX      ", "0x0F: ROR   rX      ",
		"0x10: BTST  rX ,  f ", "0x11: BSET  rX ,  f ", "0x12: BRST  rX ,  f ", "0x13: PUSH  rX      ",
		"0x14: POP   rX      ", "0x15: EI            ", "0x16: DI            ", "0x17: INT    c      ",
		"0x18: LDI   rX      ", "0x19: STI   rX      ", "0x1A: INC   rX      ", "0x1B: DEC   rX      ",
		"0x1C: CALL  rX      ", "0x1D: RET           ", "0x1E: HLT           ", "0x1F: JP    rX      ",
		"0x20: IN    rX ,  p ", "0x21: OUT    p , rX ", "0x22: SHR   rX ,  n ", "0x23: SHL   rX ,  n ",
		"0x24: MUL   rX , rY ", "0x25: DIV   rX , rY ", "0x26: ADD   rX ,  n ", "0x27: SUB   rX ,  n ",
		"0x28: MUL   rX ,  n ", "0x26: DIV   rX ,  n ", "0x2A: JMP    a      ", "0x2B: JZ    rX ,  a ",
		"0x2C: CALL   a      ", "0x2D: AND   rX ,  n ", "0x2E: OR    rX ,  n ", "0x2F: NOP    n      ",
		"0x30: LDB   rX ,(rY)", "0x31: LDB  (rX), rY ", "0x32: FLSH          ", "0x33: OUT    p ,  n ",
		"0x34: XOR   rX , rY ", "0x35: XOR   rX ,  c ", "0x36: JFLG   f , rX ", "0x37: JFLG   f ,  a ",
		"0x38: ELOG          ", "0x39: DLOG          ", "0x3A: PG    rX      ", "0x3B: PG     c      ",
		"0x3C: PEEK  rX      "
	};
}

package vm.VirtualMachine;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;

import vm.VirtualMachine.Assembler.Assembler;
import vm.VirtualMachine.CPU.CPU;

public class Test extends CPU
{
	@Override
	protected void loadROM()
	{
		int PGRM[] = (new Assembler("pgrm.s")).getCode();
		int ROM[] = (new Assembler("kernel.s")).getCode();
		Assembler.dumpBitecode("kernel.bin", ROM);
		Assembler.dumpBitecode("pgrm.bin", PGRM);
		
		writeMemory(0, ROM, 0);
		writeMemory(0x400, PGRM, 0);
	}
}

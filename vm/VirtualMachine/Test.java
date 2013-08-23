package vm.VirtualMachine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import vm.VirtualMachine.Assembler.Assembler;
import vm.VirtualMachine.CPU.CPU;

public class Test extends CPU
{
	@Override
	public void loadROM()
	{
//		int PGRM[] = (new Assembler("pgrm.s")).getCode();
//		int ROM[] = (new Assembler("kernel.s")).getCode();
//		Assembler.dumpBitecode("kernel.bin", ROM);
//		Assembler.dumpBitecode("pgrm.bin", PGRM);

		int ROM[] = readFile("resource/test.bin");
//		int ROM[] = new int[] {3, 0, 4, 3, 1, 3, 18, 0, 1, 0};
		
		writeMemory(0, ROM);
//		writeMemory(0x400, PGRM);
	}
	
	private int[] readFile(String fn)
	{
		File file = new File(fn);

		byte[] result = null;
		
		try
		{
			InputStream input =  new BufferedInputStream(new FileInputStream(file));
			result = readAndClose(input);
		}
		catch (FileNotFoundException ex)
		{
			ex.printStackTrace();
		}
		
		if(result != null && result.length > 0)
		{
			int r[] = new int[result.length >> 1];
			
			for(int i = 0 ; i < r.length ; i++)
			{
				r[i] = (int)(result[i << 1] & 0xff) | ((int)(result[(i << 1) | 1] & 0xff) << 8);
			}
			
			return r;
		}

		return new int[] {0};
	}
	
	byte[] readAndClose(InputStream aInput)
	{
		byte[] bucket = new byte[32*1024]; 
		ByteArrayOutputStream result = null; 
		
		try  
		{
			try 
			{
				result = new ByteArrayOutputStream(bucket.length);
				
				int bytesRead = 0;
				while(bytesRead != -1)
				{
					bytesRead = aInput.read(bucket);
					if(bytesRead > 0)
					{
						result.write(bucket, 0, bytesRead);
					}
				}
			}
			finally
			{
				aInput.close();
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		
		return result.toByteArray();
	}
		  
}

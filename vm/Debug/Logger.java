package vm.Debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import vm.VirtualMachine.CPU.IPrinter;

public class Logger implements IPrinter
{
	public static Logger instance;
	private File file;
	private List<String> log;

	private Logger(File file)
	{
		this.file = file;
		this.log = new ArrayList<String>();
	}

	@Override
	public void print(String s)
	{
		log(s);
	}
	
	public void log(String s)
	{
		log.add(s + '\n');
	}

	public void flush()
	{
		try
		{
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);

			for(String s : log)
			{
				bw.write(s);
			}

			bw.close();
			
			log.clear();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void initializeLogger(String path)
	{
		if(instance != null)
		{
			instance.flush();
			instance = null;
		}
		
		try
		{
			File f = new File(path);

			if(!f.exists())
			{
				f.createNewFile();
			}

			if(f.exists())
			{
				instance = new Logger(f);
			}
			else
			{
				throw new RuntimeException("Couldn't create file for logger.");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}

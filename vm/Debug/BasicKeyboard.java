package vm.Debug;

import org.lwjgl.input.Keyboard;

import vm.VirtualMachine.Start;
import vm.VirtualMachine.CPU.Periphery;

public class BasicKeyboard extends Periphery
{
	private int mode;
	
	@Override
	public void acceptWord(int word)
	{
		mode = word & 1;
	}

	@Override
	public int retrieveWord()
	{
		if(mode == 0)
		{
			while (Keyboard.next())
			{
				if (Keyboard.getEventKeyState())
				{
					return Keyboard.getEventKey();
				}
			}
		}
		else
		{
			for(int i = 0 ; i < 0x100 ; i++)
			{
				if(Keyboard.isKeyDown(i)) return i;
			}
		}
		
		return 0xffff;
	}
}

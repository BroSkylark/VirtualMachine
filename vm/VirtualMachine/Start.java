package vm.VirtualMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import vm.Debug.BasicKeyboard;
import vm.Debug.BasicScreen;
import vm.Debug.Logger;
import vm.VirtualMachine.Assembler.Assembler;
import vm.VirtualMachine.Compiler.Compiler;
import vm.VirtualMachine.CPU.IPrinter;

public class Start
{
	private static final int WIDTH = 256, HEIGHT = 256;
	public static boolean DEBUG;
	public static final IPrinter printer = new IPrinter() {
		@Override
		public void print(String s)
		{
			out(s);
		}
	};
	
	public static boolean test()
	{
		return false;
	}
	

	public static void main(String argv[])
	{
		Logger.initializeLogger("resource/log.log");
		DEBUG = true;
		
		if(test()) return;

		Test t = new Test();
		BasicScreen screen = new BasicScreen(WIDTH, HEIGHT);
		BasicKeyboard keyboard = new BasicKeyboard();
		t.loadROM();
		t.attachPeriphery(screen);
		t.attachPeriphery(keyboard);


		try
		{
			Display.setDisplayMode(new DisplayMode(WIDTH * 2, HEIGHT * 2));
			Display.create();
		}
		catch(LWJGLException e)
		{
			e.printStackTrace();
		}

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glOrtho(0, WIDTH, HEIGHT, 0, -1, 1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);


		GL11.glEnable(GL11.GL_TEXTURE_2D);

		screen.createTexture();

		try
		{
			Logger.initializeLogger("resource/log.log");
			
			t.printMemory(printer, 0, 0x100);
			
			while(!Display.isCloseRequested())
			{
				if(!t.isStopped())
				{
					for(int i = 0 ; i < 16 ; i++)
					{
						t.execute();
						
						if(t.isFlushing()) break;
					}
				}
				else
				{
					break;
				}

				screen.step();
				keyboard.step();

				if(screen.hasBeenModified())
				{
					screen.refreshTexture();
				}

				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
				GL11.glPushMatrix();

				screen.bind();

				GL11.glBegin(GL11.GL_QUADS);
				{
					GL11.glTexCoord2f(0, 0);
					GL11.glVertex2f(0, 0);

					GL11.glTexCoord2f(1, 0);
					GL11.glVertex2f(WIDTH, 0);

					GL11.glTexCoord2f(1, 1);
					GL11.glVertex2f(WIDTH, HEIGHT);

					GL11.glTexCoord2f(0, 1);
					GL11.glVertex2f(0, HEIGHT);
				}
				GL11.glEnd();

				screen.unBind();

				GL11.glPopMatrix();

				Display.update();

				Display.sync(60);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(Display.isCreated())
				Display.destroy();
		}

		t.printMemory(printer, 0, 0x100);
		
		t.printState(printer);
		
		Logger.instance.flush();
	}

	public static void out(Object o) { out(o, true); }
	public static void out(Object o, boolean nl)
	{
		if(DEBUG)
		{
			Logger.instance.log(o.toString());
			System.out.print(o.toString());
			if(nl) System.out.println();
		}
	}
}

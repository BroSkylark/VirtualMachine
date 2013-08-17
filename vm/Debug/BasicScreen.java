package vm.Debug;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import vm.VirtualMachine.CPU.Periphery;

public class BasicScreen extends Periphery
{
	private static BufferedImage charset;
	private static final int BYTES_PER_PIXEL = 3;
	private BufferedImage frame;
	public final int width, height;
	private boolean isModified;
	private int textureID;
	private Queue<Integer> buffer;

	public BasicScreen(int w, int h)
	{
		width = w;
		height = h;
		frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		isModified = true;
		buffer = new ArrayDeque<Integer>(16);
		textureID = 0;
	}

	@Override
	public void acceptWord(int word)
	{
		buffer.add(word & 0xffff);
//		Start.out(String.format("Received:  0x%04X", word));
	}

	@Override
	public void step()
	{
		while(!buffer.isEmpty())
		{
			switch(buffer.peek() & 0xff)
			{
			case 0:
				buffer.poll();
				break;
			case 1:
				if(((buffer.peek() >> 8) & 1) == 0)
				{
					if(buffer.size() < 6) return;
					buffer.poll();
					int x = buffer.poll();
					int y = buffer.poll();
					int w = buffer.poll();
					int h = buffer.poll();
					int a = buffer.poll();
					drawImage(x, y, w, h, a);
				}
				else
				{
					if(buffer.size() < 2) return;
					buffer.poll();
					drawImage(0, 0, width, height, buffer.poll());
				}
				break;
			case 2:
				generateText(buffer.poll(), buffer.poll());
				break;
			case 3:
				buffer.poll();
				clear();
				break;
			}
		}
	}
	
	private void clear()
	{
		Graphics2D g = frame.createGraphics();
		g.clearRect(0, 0, width, height);
		g.dispose();
		
		isModified = true;
	}
	
	private void generateText(int addr, int length)
	{
//		int word, x, y, c;
//		Graphics2D g = frame.createGraphics();
//		
//		if((drawMode & 2) != 0)
//		{
//			int data[] = this.cpu.readMemory(addr, length * 2);
//			
//			for(int i = 0 ; i < data.length ; i += 2)
//			{
//				x = (data[i] & 0xf0) >> 4;
//				y =  data[i] & 0x0f;
//				c =  data[i + 1];
//				
//				writeChar(g, x, y, c);
//			}
//		}
//		else
//		{
//			int data[] = this.cpu.readMemory(addr, length);
//			
//			for(int i = 0 ; i < data.length ; i++)
//			{
//				writeChar(g, this.x++, this.y, data[i]);
//				if(this.x - oX >= spriteWidth) { this.x = oX; this.y++; }
//			}
//		}
//		
//		isModified = true;
//		
//		g.dispose();
	}
	
	private void drawImage(int dX, int dY, int w, int h, int addr)
	{
		Graphics2D g = frame.createGraphics();
		
		int data[] = cpu.readMemory(addr, w * h * 2);
		for(int i = 0, x = 0, y = 0 ; i < data.length ; i += 2)
		{
			int ct = (data[i] << 8) | data[i + 1];
			float c[] = Color.RGBtoHSB(((ct >> 8) & 0x0f) << 4, ((ct >> 4) & 0x0f) << 4, (ct & 0x0f) << 4, null);

			g.setColor(Color.getHSBColor(c[0], c[1], c[2]));
			g.drawLine(dX + x, dY + y, dX + x, dY + y);
			
			if(++x >= w) { x = 0; y++; }
		}
		
		isModified = true;
		
		g.dispose();
	}

	@Override
	public int retrieveWord()
	{
		return 0xffff;
	}

	@SuppressWarnings("unused")
	private void writeChar(Graphics2D g, int x, int y, int c)
	{
		int s = 8;
		int dx = x * s, dy = y * s, sx = (c & 0x0f) * s, sy = ((c & 0xf0) >> 4) * s;
		g.drawImage(charset, dx, dy, dx + s, dy + s, sx, sy, sx + s, sy + s, null);
	}

	public void refreshTexture()
	{
		if(textureID <= 0) throw new RuntimeException("ERR: Tried to refresh texture without generating one!");
		
		int[] pixels = new int[frame.getWidth() * frame.getHeight()];

		frame.getRGB(0, 0, frame.getWidth(), frame.getHeight(), pixels, 0, frame.getWidth());

		ByteBuffer buffer = BufferUtils.createByteBuffer(frame.getWidth() * frame.getHeight() * BYTES_PER_PIXEL);

		for(int y = 0; y < frame.getHeight(); y++){
			for(int x = 0; x < frame.getWidth(); x++){
				int pixel = pixels[y * frame.getWidth() + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
				buffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
				buffer.put((byte) (pixel & 0xFF));               // Blue component
//				buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
			}
		}

		buffer.flip(); //FOR THE LOVE OF GOD DO NOT FORGET THIS

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID); //Bind texture ID

		//Setup wrap mode
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

		//Setup texture scaling filtering
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		//Send texel data to OpenGL
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, frame.getWidth(), frame.getHeight(), 0,
				GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);

		isModified = false;
	}

	public void createTexture()
	{
		textureID = GL11.glGenTextures(); 

		refreshTexture();
	}
	
	public boolean hasBeenModified() { return isModified; }
	public void bind() { GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID); }
	public void unBind() { GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); }

	static
	{
		try
		{
			charset = ImageIO.read(new File("resource/charset.png"));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}

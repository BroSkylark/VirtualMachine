package vm.VirtualMachine.CPU;

public abstract class Periphery
{
	protected ICPU cpu;
	protected int port;
	
	public void attachToCPU(ICPU icpu, int p)
	{
		cpu = icpu;
		port = p;
	}
	
	public void step()
	{
	}
	
	public abstract void acceptWord(int word);
	public abstract int retrieveWord();
}

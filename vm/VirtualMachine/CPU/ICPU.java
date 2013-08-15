package vm.VirtualMachine.CPU;

public interface ICPU
{
	public abstract void writeMemory(int offset, int data[]);
	public abstract int[] readMemory(int offset, int length);
	public abstract void attachPeriphery(Periphery p);
	public abstract void detachPeriphery(Periphery p);
}

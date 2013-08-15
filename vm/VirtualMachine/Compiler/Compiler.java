package vm.VirtualMachine.Compiler;

import vm.VirtualMachine.Assembler.Assembler;

public class Compiler
{
	private String file, data;
	private ClassTree classTree;
	
	public Compiler(String path)
	{
		file = path;
		data = "";
		classTree = new ClassTree();
	}
	
	public void compile()
	{
		data = Preprocessor.process(file);
		data = classTree.formInheritanceTree(data);
		classTree.printClass("Test");
	}
}

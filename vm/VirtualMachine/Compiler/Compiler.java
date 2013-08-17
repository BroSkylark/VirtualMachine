package vm.VirtualMachine.Compiler;

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

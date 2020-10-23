package mortar.util.text;

import ninja.bytecode.shuriken.execution.J;
import ninja.bytecode.shuriken.reaction.O;

public class VersionCodes
{
	public static int getVersionCode(String version)
	{
		O<Integer> bit = new O<Integer>().set(1);
		O<Integer> code = new O<Integer>().set(0);

		for(char i : version.toCharArray())
		{
			J.attempt(() -> code.set(code.get() + Integer.valueOf(i + "") * bit.get()));
			bit.set(bit.get() + 1);
		}

		return code.get();
	}
}

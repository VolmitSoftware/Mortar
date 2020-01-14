package mortar.api.nms;

import mortar.util.text.D;

public class Catalyst
{
	public static final CatalystHost host = getHost();

	private static CatalystHost getHost()
	{
		NMSVersion v = NMSVersion.current();

		switch(v)
		{
			case R1_10:
				D.as("NMP").v("Selected Catalyst:10");
				return new Catalyst10();
			case R1_11:
				D.as("NMP").v("Selected Catalyst:11");
				return new Catalyst11();
			case R1_12:
				D.as("NMP").v("Selected Catalyst:12");
				return new Catalyst12();
			case R1_13:
				D.as("NMP").v("Selected Catalyst:13");
				return new Catalyst13();
			case R1_14:
				D.as("NMP").v("Selected Catalyst:14");
				return new Catalyst14();
			case R1_8:
				D.as("NMP").v("Selected Catalyst:8");
				return new Catalyst8();
			case R1_9_4:
				D.as("NMP").v("Selected Catalyst:94");
				return new Catalyst94();
			default:
				D.as("NMP").v("Could not find a suitable catalyst for your NMS version!");
				return null;
		}
	}
}

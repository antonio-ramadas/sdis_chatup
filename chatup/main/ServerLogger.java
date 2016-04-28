package chatup.main;

public class ServerLogger
{
	private static ServerLogger instance;

	private ServerLogger()
	{
	}

	public static ServerLogger getInstance()
	{
		if (instance == null)
		{
			instance = new ServerLogger();
		}

		return instance;
	}
}

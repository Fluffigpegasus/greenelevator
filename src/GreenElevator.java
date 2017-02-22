import java.net.Socket;
import java.io.*;

class GreenElevator
{
	private static Elevator[] elevators;
	private static PrintWriter wr;
	
	public static void main(String[] args) throws Exception
	{
		if (args.length != 4)
		{
			System.out.println("usage: java GreenElevator <host> <port> <elevator count> <floor count>");
			System.exit(1);
		}

		String host = args[0];
		int port = -1;
		int countElevators = -1;
		int countFloors = -1;

		try
		{
			port = Integer.parseInt(args[1]);
			countElevators = Integer.parseInt(args[2]);
			countFloors = Integer.parseInt(args[3]);
			
			if (port < 1 || countElevators < 1 || countFloors < 1) { throw new Exception(); }
		}

		catch (Exception exception)
		{
			System.out.println("error: invalid <port>, <elevator count> or <floor count>");
			System.exit(1);
		}
		
		Socket socket = new Socket(host, port);
		wr = new PrintWriter(socket.getOutputStream());

		elevators = new Elevator[countElevators + 1];

		for (int i = 1; i <= countElevators; i++)
		{
			elevators[i] = new Elevator(i);
			elevators[i].start();
		}

		BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String commandWithArguments;

		while ((commandWithArguments = rd.readLine()) != null)
		{
			char command = commandWithArguments.charAt(0);

			switch (command)
			{
				case 'f':
					handleCommandF(commandWithArguments);
					break;
				
				case 'p':
					handleCommandP(commandWithArguments);
					break;
				
				default:
					System.out.println("Received unknown event: " + commandWithArguments);
			}
		}
		
		socket.close();
	}

	static void sendCommand(String command)
	{
		wr.println(command);
		wr.flush();
	}

	// inform the elevator of its position
	private static void handleCommandF(String commandWithArguments)
	{		
		String[] split = commandWithArguments.split(" ");
		elevators[Integer.parseInt(split[1])].setLocation(Double.parseDouble(split[2]));
	}

	private static void handleCommandP(String commandWithArguments)
	{

	}
}

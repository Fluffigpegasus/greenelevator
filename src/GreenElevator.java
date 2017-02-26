import java.net.Socket;
import java.io.*;
import java.util.LinkedList;

class GreenElevator
{
	private static Elevator[] elevators;
	private static PrintWriter wr;
	private static LinkedList<Task> queue = new LinkedList<Task>();

	static boolean debug = false;

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

		socket.setSoLinger(false, 0);

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
			if (debug) { System.out.println("RECV: " + commandWithArguments); }

			char command = commandWithArguments.charAt(0);

			switch (command)
			{
				case 'b':
					handleCommandB(commandWithArguments);
					break;

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
		if (debug) { System.out.println("SENT: " + command); }
		wr.println(command);
		wr.flush();
	}

	private static void handleCommandB(String commandWithArguments)
	{
		String[] split = commandWithArguments.split(" ");
		Task task = new Task(Integer.parseInt(split[1]), split[2].equals("1") ? Direction.UP : Direction.DOWN);

		handleOutsideClick(task);
	}

	// inform the elevator of its position
	private static void handleCommandF(String commandWithArguments)
	{
		String[] split = commandWithArguments.split(" ");
		elevators[Integer.parseInt(split[1])].setLocation(Double.parseDouble(split[2]));
	}

	private static void handleCommandP(String commandWithArguments)
	{
		String[] split = commandWithArguments.split(" ");

		if(split[2].equals("32000"))
		{
			elevators[Integer.parseInt(split[1])].emergencyStop();
			return;
		}

		else
		{
			elevators[Integer.parseInt(split[1])].addDestination(Integer.parseInt(split[2]));
		}
	}

	static void handleOutsideClick(Task task)
	{
		Elevator candidate = null;
		double min = -1;

		for (int i = 1; i < elevators.length; i++)
		{
			if (!elevators[i].isEmergencyStopped() &&
			    elevators[i].getDestinations() == 1 &&
			    elevators[i].getDestination().getFloor() == task.getFloor() &&
			    elevators[i].getDestination().getDirection() != (task.getDirection() == Direction.UP ? Direction.DOWN : Direction.UP) &&
			    (Math.abs(elevators[i].getLocation() - task.getFloor()) < min || min < 0))
			{
				candidate = elevators[i];
			}
		}

		if (candidate != null)
		{
			if (candidate.getDestination().getDirection() == null) { candidate.getDestination().setDirection(task.getDirection()); }
			return;
		}

		if(queue.isEmpty())
		{
			candidate = null;
			min = -1;

			for(int i = 1; i < elevators.length; i++)
			{
				if(!elevators[i].isInUse() && (Math.abs(elevators[i].getLocation() - task.getFloor()) < min || min < 0))
				{
					candidate = elevators[i];
				}
			}
			if (candidate == null) { addTask(task); }
			else { candidate.giveTask(task); }
		}

		else
		{
			addTask(task);
		}
	}

	synchronized static Task getNextTask()
	{
		return queue.poll();
	}

	synchronized static void addTask(Task task)
	{
		queue.add(task);
	}
}

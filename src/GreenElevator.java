import java.net.Socket;
import java.io.*;
import java.util.LinkedList;
import java.util.Iterator;

class GreenElevator
{
	private static Elevator[] elevators;
	private static PrintWriter wr;
	private static LinkedList<Task> queue = new LinkedList<Task>();

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
		wr.println(command);
		wr.flush();
	}

	private static void handleCommandB(String commandWithArguments)
	{
		String[] split = commandWithArguments.split(" ");
		Task task = new Task(Integer.parseInt(split[1]), split[2].equals("1") ? Direction.UP : Direction.DOWN);

		handleOutsideClick(task);
		synchronized(GreenElevator.class) { GreenElevator.class.notifyAll(); }
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

		if (split[2].equals("32000")) { elevators[Integer.parseInt(split[1])].emergencyStop(); }
		else { elevators[Integer.parseInt(split[1])].addDestination(Integer.parseInt(split[2])); }

		synchronized(GreenElevator.class) { GreenElevator.class.notifyAll(); }
	}

	static void handleOutsideClick(Task task)
	{
		LinkedList<Elevator> sortedElevators = new LinkedList<Elevator>();
		sortedElevators.add(elevators[1]);
		
		// check if there already is an elevator heading for this floor -- if so, don't proceed
		for (int j = 1; j < elevators.length; j++)
		{
			if (elevators[j].isInUse() && !elevators[j].getDestinations().isEmpty() && elevators[j].getDestinations().getFirst().getFloor() == task.getFloor() && (elevators[j].getDestinations().getFirst().getDirection() == null || elevators[j].getDestinations().getFirst().getDirection() == task.getDirection()))
			{
				return;
			}
		}

		outer1:
		for (int j = 2; j < elevators.length; j++)
		{
			double distance = Math.abs(elevators[j].getLocation() - task.getFloor());
			int i = 0;

			for (Elevator otherElevator : sortedElevators)
			{
				if (Math.abs(otherElevator.getLocation() - task.getFloor()) > distance)
				{
					sortedElevators.add(i, elevators[j]);
					continue outer1;
				}

				i++;
			}

			sortedElevators.add(elevators[j]);
		}

		// try to assign the task to an elevator
		outer2:
		for (Elevator elevator : sortedElevators)
		{
			if (elevator.isEmergencyStopped()) { continue; }

			if (!elevator.isInUse()) 
			{
				elevator.giveTask(task);
				return;
			}

			// the elevator is on our floor and it's not being used any longer
			else if (elevator.getDestinations().isEmpty() && Math.abs(elevator.getLocation() - task.getFloor()) < 0.05)
			{
				elevator.openDoors();
				return;
			}

			// does it pass by our floor before its next destination?
			else if (!elevator.getDestinations().isEmpty())
			{
				for (Task destination : elevator.getDestinations())
				{
					if (destination.getDirection() != null && destination.getDirection() != task.getDirection()) { continue outer2; }
				}

				LinkedList<Task> destinationsAll = new LinkedList<Task>(elevator.getDestinations());
				destinationsAll.add(0, new Task(elevator.getLocation(), null));

				// is the elevator passing us right now?
				boolean elevatorPassingBy = false;

				// does it pass by our floor at all?
				Iterator<Task> iterator = destinationsAll.iterator();

				Task taskPrev = null;
				Task taskNext = null;

				int j = 0;

				while (iterator.hasNext())
				{
					if (taskPrev == null)
					{
						taskPrev = iterator.next();
						taskNext = iterator.next();

						j++;
					}

					if (
					     (taskPrev.getFloor() > task.getFloor() && task.getFloor() > taskNext.getFloor() && task.getDirection() == Direction.DOWN)
					     ||
					     (taskPrev.getFloor() < task.getFloor() && task.getFloor() < taskNext.getFloor() && task.getDirection() == Direction.UP)
					    )
					{
						elevatorPassingBy = true;
						break;
					}

					if (!iterator.hasNext()) { break; }

					taskPrev = taskNext;
					taskNext = iterator.next();

					j++;
				}

				// is the queue strictly smaller or stricly larger?
				if (elevatorPassingBy)
				{
					double prev = -1;
					int k = 0;
					boolean strictlyIncreasing = true;
					boolean strictlyDecreasing = true;

					for (Task destination : destinationsAll)
					{
						if (k > 0)
						{
							if (strictlyIncreasing && destination.getFloor() <= prev) { strictlyIncreasing = false; }
							if (strictlyDecreasing && destination.getFloor() >= prev) { strictlyDecreasing = false; }
						}

						prev = destination.getFloor();
						k++;
					}

					if (strictlyIncreasing || strictlyDecreasing)
					{
						elevator.addDestination((int) task.getFloor());
						return;
					}
				}
			}
		}
		
		// no elevator was available to handle our task -- stall it
		queue.add(task);
	}

	synchronized static Task getNextTask()
	{
		return queue.poll();
	}
}

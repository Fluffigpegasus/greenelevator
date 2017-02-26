import java.util.LinkedList;
import java.util.Iterator;

class Elevator extends Thread
{
	private int elevatorId;
	private double location;
	private LinkedList<Task> destinations = new LinkedList<Task>();
	private boolean moving = false;
	private boolean emergencyStopped = false;
	private long doorOpen = -1;

	Elevator(int elevatorId)
	{
		this.elevatorId = elevatorId;
	}

	@Override
	public void run()
	{
		GreenElevator.sendCommand("where " + elevatorId);

		while (true)
		{
			if (doorOpen != -1 && System.currentTimeMillis() > doorOpen + 5000)
			{
				GreenElevator.sendCommand("door " + elevatorId + " -1");
				doorOpen = -1;

				Task next = GreenElevator.getNextTask();

				if (next != null) { destinations.add(next); }
				if (!destinations.isEmpty()) { startMoving(); }
			}

			try { Thread.sleep(100); }
			catch (Exception exception) {}
		}
	}

	public boolean isInUse()
	{
		return !destinations.isEmpty() || emergencyStopped || doorOpen != -1;
	}

	public int getElevatorId()
	{
		return elevatorId;
	}

	void setLocation(double location)
	{
		this.location = location;

		if (moving && Math.abs(location - destinations.getFirst().getFloor()) < 0.1)
		{
			GreenElevator.sendCommand("move " + elevatorId + " 0");
			doorOpen = System.currentTimeMillis();
			GreenElevator.sendCommand("door " + elevatorId + " 1");

			destinations.poll();
			moving = false;
		}
	}

	double getLocation()
	{
		return location;
	}

	void giveTask(Task task)
	{
		assert(destinations.isEmpty());

		destinations.add(task);
		startMoving();
	}

	void addDestination(int floor)
	{hat programming environments (software) and computing platforms (a computer and OS) you used for implementation.
Describe algorithms you have
		Task task = new Task(floor, null);
		boolean isAdded = false;

		// add an element in-between if applicable
		if (!destinations.isEmpty())
		{
			Direction currentDirection = location - destinations.getFirst().getFloor() > 0 ? Direction.DOWN : Direction.UP;

			if (destinations.size() == 1)
			{
				if (currentDirection == Direction.DOWN && destinations.getFirst().getFloor() < floor ||
				    currentDirection == Direction.UP && destinations.getFirst().getFloor() > floor)
				{
					destinations.add(0, task);
					isAdded = true;
				}
			}

			else if (destinations.size() > 1)
			{
				if (currentDirection == Direction.DOWN && floor < location && destinations.getFirst().getFloor() < floor ||
				    currentDirection == Direction.UP && floor > location && destinations.getFirst().getFloor() > floor)
				{
					destinations.add(0, task);
					isAdded = true;
				}

				else
				{
					Iterator<Task> iterator = destinations.iterator();
					Task taskPrev = null;
					Task taskNext = null;

					int i = 0;

					while (iterator.hasNext())
					{
						if (taskPrev == null)
						{
							taskPrev = iterator.next();
							taskNext = iterator.next();

							i++;
							continue;
						}

						if (currentDirection == Direction.DOWN && taskPrev.getFloor() > floor && taskNext.getFloor() < floor ||
						    currentDirection == Direction.UP && taskPrev.getFloor() < floor && taskNext.getFloor() > floor)
						{
							destinations.add(i, task);
							isAdded = true;
						}

						if (!iterator.hasNext()) { break; }

						taskPrev = taskNext;
						taskNext = iterator.next();

						i++;
					}
				}
			}
		}

		if (!isAdded) { destinations.add(task); }
		if (destinations.size() == 1) { startMoving(); }
	}

	Task getDestination()
	{
		return destinations.getFirst();
	}

	int getDestinations()
	{
		return destinations.size();
	}

	void emergencyStop()
	{
		emergencyStopped = true;

		GreenElevator.sendCommand("move " + elevatorId + " 0");

		for(Task task : destinations)
		{
			if (task.getDirection() != null) { GreenElevator.handleOutsideClick(task); }
		}

		destinations.clear();
	}

	boolean isEmergencyStopped()
	{
		return emergencyStopped;
	}

	private void startMoving()
	{
		GreenElevator.sendCommand("move " + elevatorId + " " + (destinations.getFirst().getFloor() > location ? 1 : -1));
		moving = true;
	}
}

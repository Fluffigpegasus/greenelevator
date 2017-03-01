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

			if (!isInUse())
			{
				try { synchronized(GreenElevator.class) { wait(); } }
				catch (Exception exception) {}
			}
		}
	}

	public void openDoors()
	{
		doorOpen = System.currentTimeMillis();
		GreenElevator.sendCommand("door " + elevatorId + " 1");
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

		if (moving && Math.abs(location - destinations.getFirst().getFloor()) < 0.05)
		{
			GreenElevator.sendCommand("move " + elevatorId + " 0");
			
			openDoors();

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
	{
		for (Task task : destinations)
		{
			if (task.getFloor() == floor) { return; }
		}

		Task task = new Task(floor, null);
		boolean isAdded = false;

		// add an element in-between if applicable
		if (!destinations.isEmpty())
		{
			Direction currentDirection = location - destinations.getFirst().getFloor() > 0 ? Direction.DOWN : Direction.UP;

			if (location > floor && floor > destinations.getFirst().getFloor() ||
			    location < floor && floor < destinations.getFirst().getFloor())
			{
				destinations.add(0, task);
				isAdded = true;
			}

			else if (destinations.size() > 1)
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
					}

					if (taskPrev.getFloor() > floor && floor > taskNext.getFloor() ||
					    taskPrev.getFloor() < floor && floor < taskNext.getFloor())
					{
						destinations.add(i, task);
						isAdded = true;
						break;
					}

					if (!iterator.hasNext()) { break; }

					taskPrev = taskNext;
					taskNext = iterator.next();

					i++;
				}
			}
		}

		if (!isAdded) { destinations.add(task); }
		
		if (destinations.size() == 1)
		{
			emergencyStopped = false;
			
			if (doorOpen == -1) { startMoving(); }
		}
	}

	Task getDestination()
	{
		return destinations.getFirst();
	}

	LinkedList<Task> getDestinations()
	{
		return destinations;
	}

	void emergencyStop()
	{
		emergencyStopped = true;

		GreenElevator.sendCommand("move " + elevatorId + " 0");

		for(Task task : destinations)
		{
			if (task.getDirection() != null) { GreenElevator.handleOutsideClick(task); }
		}
		
		synchronized(GreenElevator.class) { GreenElevator.class.notifyAll(); }		

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

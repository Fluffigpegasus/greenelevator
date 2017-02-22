class Elevator extends Thread
{
	private int elevatorId;
	private double location;

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
			
		}
	}

	public int getElevatorId()
	{
		return elevatorId;
	}

	void setLocation(double location)
	{
		this.location = location;

		System.out.println("Elevator " + elevatorId + " is now on location " + location);
	}
}

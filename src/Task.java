class Task
{
	private double floor;
	private Direction direction;

	Task(double floor, Direction direction)
	{
		this.floor = floor;
		this.direction = direction;
	}

	Task(int floor, Direction direction)
	{
		this.floor = (double) floor;
		this.direction = direction;
	}

	double getFloor()
	{
		return floor;
	}

	Direction getDirection()
	{
		return direction;
	}

	void setDirection(Direction direction)
	{
		this.direction = direction;
	}
}

class Task
{
	private int floor;
	private Direction direction;

	Task(int floor, Direction direction)
	{
		this.floor = floor;
		this.direction = direction;
	}

	int getFloor()
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

# Commands that we can send
* door <elevator> <-1|1>
* move <elevator> <-1|0|1>
* where <elevator>
* velocity
* scale <elevator> <floor>

# Events that we can react on
* b <floor> <-1|1> -- someone requested an elevator on a certain floor
* p <elevator> 32000 -- emergency stop (?) an elevator from inside
* p <elevator> <floor> -- someone inside an elevator requested moving
* f <elevator> <location> -- location (floor) of an elevator, emitted when moving or on a `where` request
* v <velocity> -- returns the speed of the elevators

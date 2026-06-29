package orsc;

final class MonitorMode {
	final int x;
	final int y;
	final int width;
	final int height;

	MonitorMode(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = Math.max(1, width);
		this.height = Math.max(1, height);
	}
}

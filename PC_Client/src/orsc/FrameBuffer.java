package orsc;

import java.nio.ByteBuffer;

final class FrameBuffer {
	final ByteBuffer buffer;

	FrameBuffer(int byteCount) {
		buffer = ByteBuffer.allocateDirect(byteCount);
	}

	int capacity() {
		return buffer.capacity();
	}
}

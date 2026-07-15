package orsc;

import java.util.ArrayDeque;

final class FrameBufferPool {
	private static final int MAX_RETAINED_BUFFERS = 3;

	private final ArrayDeque<FrameBuffer> available = new ArrayDeque<>();

	synchronized FrameBuffer acquire(int requiredBytes) {
		FrameBuffer selected = null;
		for (FrameBuffer frameBuffer : available) {
			if (frameBuffer.capacity() >= requiredBytes) {
				selected = frameBuffer;
				break;
			}
		}
		if (selected != null) {
			available.remove(selected);
			return selected;
		}
		return new FrameBuffer(requiredBytes);
	}

	synchronized void release(FrameBuffer frameBuffer) {
		if (frameBuffer == null) {
			return;
		}
		frameBuffer.buffer.clear();
		if (available.size() < MAX_RETAINED_BUFFERS) {
			available.addFirst(frameBuffer);
		}
	}
}

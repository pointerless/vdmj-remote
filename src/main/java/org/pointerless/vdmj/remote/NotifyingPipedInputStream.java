package org.pointerless.vdmj.remote;

import java.io.IOException;
import java.io.PipedInputStream;
import java.util.Arrays;

public class NotifyingPipedInputStream extends PipedInputStream {

	//TODO: Properly implement without VDMJ changes
	//Must be able to stop reading when VDMJ starts writing
	public boolean waitingForInput = false;

	public NotifyingPipedInputStream(int pipeSize){
		super(pipeSize);
	}

	@Override
	public synchronized int read() throws IOException {
		Thread thread = Thread.currentThread();
		System.out.println("Set1");
		System.out.println(Arrays.toString(thread.getStackTrace()));
		waitingForInput = true;
		int out = super.read();
		waitingForInput = false;
		System.out.println("Reset1");
		return out;
	}

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		Thread thread = Thread.currentThread();
		System.out.println("Set2");
		System.out.println(Arrays.toString(thread.getStackTrace()));
		waitingForInput = true;
		int out = super.read(b, off, len);
		waitingForInput = false;
		System.out.println("Reset2");
		return out;
	}
}

package org.pointerless.vdmj.remote.engine;

import java.io.*;

/**
 * Represents a pair of connected streams, holding them together
 */
public class PairedPipedIOStream{

	private static final class Input extends PipedInputStream {

		public Input(int pipeSize){ super(pipeSize); }

	}

	private static final class Output extends PipedOutputStream {

		public Output(){ super(); }

		public <T extends PipedInputStream> Output(T inputStream) throws IOException {
			super(inputStream);
		}
	}

	public static int pipeSize = 2 << 6;

	private final Input input;
	private final Output output;

	public PairedPipedIOStream() throws IOException {
		this.input = new Input(pipeSize);
		this.output = new Output(input);
	}

	public InputStream getInputStream(){
		return input;
	}

	public OutputStream getOutputStream(){
		return output;
	}

}

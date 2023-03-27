package org.pointerless.vdmj.remote;

import java.io.*;

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

	public static int pipeSize = 2 << 15;

	private Input input;
	private Output output;

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

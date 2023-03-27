package org.pointerless.vdmj.remote.engine;

import java.util.concurrent.LinkedBlockingQueue;

public class CommandQueue extends LinkedBlockingQueue<Command> {

	@Override
	public void put(Command command) throws InterruptedException {
		command.onQueued();
		super.put(command);
	}

	@Override
	public Command take() throws InterruptedException {
		Command taken = super.take();
		taken.onExecutionStart();
		return taken;
	}

}

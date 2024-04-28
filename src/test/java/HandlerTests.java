import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.values.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.engine.VDMJHandler;
import org.pointerless.vdmj.remote.ipc.IPCIOFactory;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HandlerTests {

	private VDMJHandler handler;
	private Thread handlerThread;

	@BeforeAll()
	public void setup(){
		log.info("Setting up IPCIO server");

		if(IPCIOFactory.available()){
			try {
				IPCIOFactory.getIPCIO().close();
			} catch (Exception e) {
				fail("Failed to close existing IPCIO");
			}
		}
		IPCIOFactory.createIPCIO(new BufferedReader(Reader.nullReader()), new PrintWriter(Writer.nullWriter()));
		log.info("IPCIO server set up");

		String[] vdmjArgs = {
				"-i", "-annotations", "-vdmsl", TestHelper.getTmpConway().toString()
		};

		try {
			handler = new VDMJHandler(vdmjArgs);
			handlerThread = new Thread(handler, "HandlerTests-VDMJ-Handler-Thread");
			handlerThread.start();

			handler.pickupStartupString();
		}catch (IOException e){
			fail("Could not start handler: "+e.getMessage());
		}

		List<String> expectedStartup = List.of(
				"Parsed 1 module in .* secs. No syntax errors",
				"Type checked 1 module in .* secs. No type errors",
				"Initialized 1 module in .* secs. No init errors",
				"Interpreter started"
		);

		List<String> actualStartup = List.of(handler.getStartupString().split("\n"));

		assertLinesMatch(expectedStartup, actualStartup, "Startup did not match: "+actualStartup);

		log.info("Finished starting handler, continuing with tests");
	}

	@Test()
	public void testEnv() throws InterruptedException {
		Command result = handler.runCommand("env");
		assertFalse(result.isError(), "Error in env: "+result.getResponse());
		log.info("No error in env call");
	}

	@Test
	public void testHelp() throws InterruptedException{
		Command result = handler.runCommand("help");
		assertFalse(result.isError(), "Error in help: "+result.getResponse());
		log.info("No error in help call");
	}

	@AfterAll
	public void cleanup(){
		if(handlerThread != null){
			this.handlerThread.interrupt();
		}
		TestHelper.deleteTmpConway();
	}
}

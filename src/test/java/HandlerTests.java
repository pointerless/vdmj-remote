import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.engine.VDMJHandler;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HandlerTests {

	private VDMJHandler handler;
	private Thread handlerThread;

	@BeforeAll()
	public void setup() {
		URL conwayVDMSL = this.getClass().getClassLoader().getResource("Conway.vdmsl");

		assertNotNull(conwayVDMSL, "Couldn't find Conway.vdmsl in resources");

		String[] vdmjArgs = {
				"-i", "-annotations", "-vdmsl", conwayVDMSL.toString()
		};

		try {
			handler = new VDMJHandler(vdmjArgs);
			handlerThread = new Thread(handler);
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

		log.info("Finished starting server, continuing with tests");

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
		this.handlerThread.interrupt();
	}
}

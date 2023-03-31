import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pointerless.vdmj.remote.ExtendedMain;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.engine.VDMJHandler;
import org.pointerless.vdmj.remote.gui.MainOutputSession;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MainTests {

	private VDMJHandler handler;
	private ExtendedMain extendedMain;

	@BeforeAll()
	public void setup() throws IOException{
		URL conwayVDMSL = this.getClass().getClassLoader().getResource("Conway.vdmsl");

		assertNotNull(conwayVDMSL);

		String[] vdmjArgs = {
				"-i", "-annotations", "-vdmsl", conwayVDMSL.toString()
		};

		handler = new VDMJHandler(vdmjArgs);

		int port = MainOutputSession.getRandomPort();

		assertNotEquals(-1, port);

		extendedMain = new ExtendedMain(handler, port);

		List<String> expectedStartup = List.of(
				"Parsed 1 module in .* secs. No syntax errors",
				"Type checked 1 module in .* secs. No type errors",
				"Initialized 1 module in .* secs. No init errors",
				"Interpreter started"
		);

		List<String> actualStartup = List.of(handler.getStartupString().split("\n"));

		assertLinesMatch(expectedStartup, actualStartup);

		//extendedMain.run();

		log.info("Finished starting server, continuing with tests");

	}

	@Test()
	public void testConway() throws InterruptedException {
		Command result = handler.runCommand("env");

		log.info(result.toString());
	}

}

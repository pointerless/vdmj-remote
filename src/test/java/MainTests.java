import org.junit.jupiter.api.Test;
import org.pointerless.vdmj.remote.ExtendedMain;
import org.pointerless.vdmj.remote.SessionException;
import org.pointerless.vdmj.remote.VDMJHandler;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MainTests {

	@Test
	public void testConway() throws IOException {
		URL conwayVDMSL = this.getClass().getClassLoader().getResource("Conway.vdmsl");

		assertNotNull(conwayVDMSL);

		String[] vdmjArgs = {
				"-i", "-annotations", "-vdmsl", conwayVDMSL.toString()
		};

		VDMJHandler handler = new VDMJHandler(vdmjArgs);

		int port = -1;
		try {
			port = ExtendedMain.getRandomPort();
		}catch (SessionException e){
			fail(e);
		}

		assertNotEquals(-1, port);

		ExtendedMain extendedMain = new ExtendedMain(handler, port);

		List<String> expectedStartup = List.of(
				"Parsed 1 module in .* secs. No syntax errors",
				"Type checked 1 module in .* secs. No type errors",
				"Initialized 1 module in .* secs. No init errors",
				"Interpreter started"
		);

		List<String> actualStartup = List.of(handler.getStartupString().split("\n"));

		assertLinesMatch(expectedStartup, actualStartup);

	}

}

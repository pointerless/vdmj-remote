import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pointerless.vdmj.remote.SerializationHelper;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.engine.VDMJHandler;
import org.pointerless.vdmj.remote.rest.MainOutputSession;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MainOutputSessionTests {

	private static final ObjectMapper objectMapper = SerializationHelper.getObjectMapper();

	private VDMJHandler handler;
	private Thread handlerThread;
	private MainOutputSession session;
	private int sessionPort;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private static final List<String> expectedStartup = List.of(
			"Parsed 1 module in .* secs. No syntax errors",
			"Type checked 1 module in .* secs. No type errors",
			"Initialized 1 module in .* secs. No init errors",
			"Interpreter started"
	);

	@BeforeAll
	public void setup(){

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

		List<String> actualStartup = List.of(handler.getStartupString().split("\n"));

		assertLinesMatch(expectedStartup, actualStartup, "Startup did not match: "+actualStartup);

		log.info("Finished starting server, starting OutputSession");

		sessionPort = MainOutputSession.getRandomPort();
		session = MainOutputSession.mainOutputSession(handler, sessionPort);
		session.startSession();
	}


	private <T> HttpResponse<T> sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler){
		HttpResponse<T> response = null;
		try {
			log.info("Sending '"+request.uri()+"' request");
			response = httpClient.send(request, bodyHandler);
			assertEquals(response.statusCode(), 200, "Invalid response code: "+ response.statusCode());

			log.info("'"+request.uri()+"' response OK");
		} catch (IOException e) {
			fail("Couldn't send '"+request.uri()+"' request: "+e.getMessage());
		} catch (InterruptedException e) {
			fail("'"+request.uri()+"' request sending interrupted: "+e.getMessage());
		}

		return response;
	}

	private <T> HttpResponse<T> sendGET(String path, HttpResponse.BodyHandler<T> bodyHandler){
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+sessionPort+path))
				.GET()
				.build();

		return sendRequest(request, bodyHandler);
	}

	private <T> HttpResponse<T> sendPOST(String path, String body, HttpResponse.BodyHandler<T> bodyHandler){
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+sessionPort+path))
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		return sendRequest(request, bodyHandler);
	}

	@Test
	public void testStartup() {
		log.info("Sending /startup request");
		HttpResponse<String> response = sendGET("/startup", HttpResponse.BodyHandlers.ofString());

		List<String> receivedStartup = List.of(response.body().split("\n"));
		assertLinesMatch(expectedStartup, receivedStartup, "Received startup did not match: "+receivedStartup);

		log.info("Received startup string valid");
	}

	@Test
	public void testExecHelp() {
		log.info("Sending /exec (help) request");
		HttpResponse<String> response = sendPOST("/exec", "help", HttpResponse.BodyHandlers.ofString());

		try {
			Command commandResponse = objectMapper.readValue(response.body(), Command.class);
			assertTrue(commandResponse.getResponse().getMessage().length() > 0);
			assertTrue(commandResponse.getResponse().getMessage().contains("init - "));

			log.info("Received help response valid");
		} catch (JsonProcessingException e) {
			fail("Couldn't deserialize Command from response: "+e.getMessage());
			throw new RuntimeException(e);
		}

	}

	@Test
	public void testExecTests() {
		log.info("Sending /exec (p tests()) request");
		HttpResponse<String> response = sendPOST("/exec", "p tests()", HttpResponse.BodyHandlers.ofString());
		Pattern testResultsPattern = Pattern.compile("(\\[(?:true|true,\\s)*])");

		try {
			Command commandResponse = objectMapper.readValue(response.body(), Command.class);
			assertTrue(commandResponse.getResponse().getMessage().length() > 0);

			Matcher matcher = testResultsPattern.matcher(commandResponse.getResponse().getMessage());
			assertTrue(matcher.find(), "Couldn't find valid test results in response");

			List<Boolean> results = objectMapper.readerForListOf(Boolean.class).readValue(matcher.group(1));
			assertTrue(results.stream().allMatch(Boolean::booleanValue), "Not all Conway tests passed");

			log.info("Received p tests() response valid");
		} catch (JsonProcessingException e) {
			fail("Couldn't deserialize Command from response: "+e.getMessage());
			throw new RuntimeException(e);
		}

	}

	@AfterAll
	public void cleanup(){
		session.stopSession();
		handlerThread.interrupt();
	}


}

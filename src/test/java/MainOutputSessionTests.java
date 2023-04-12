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
import org.pointerless.vdmj.remote.ipc.*;
import org.pointerless.vdmj.remote.rest.MainOutputSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

	private ServerSocket serverSocket;
	private Socket ipcioSocket;
	private IPCIO ipcio;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private static final List<String> expectedStartup = List.of(
			"Parsed 1 module in .* secs. No syntax errors",
			"Type checked 1 module in .* secs. No type errors",
			"Initialized 1 module in .* secs. No init errors",
			"Interpreter started"
	);

	@BeforeAll
	public void setup(){
		try {
			log.info("Setting up IPCIO server");
			serverSocket = new ServerSocket(0);
			if(IPCIOFactory.available()){
				fail("IPCIO already built");
			}else{
				ipcio = IPCIOFactory.createSocketIPCIO(new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort()));
			}
			ipcioSocket = serverSocket.accept();
			log.info("IPCIO server set up");
		} catch (IOException e) {
			fail("Couldn't start IPCIO server: "+e.getMessage());
		}

		String[] vdmjArgs = {
				"-i", "-annotations", "-vdmsl", TestHelper.getTmpConway().toString()
		};

		try {
			handler = new VDMJHandler(vdmjArgs);
			handlerThread = new Thread(handler, "MainOutputSessionTests-VDMJ-Handler-Thread");
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

		try {
			BufferedReader socketReader = new BufferedReader(new InputStreamReader(ipcioSocket.getInputStream()));
			IPCLog startLog = objectMapper.readValue(socketReader.readLine(), IPCLog.class);
			assertEquals(IPCLogType.START, startLog.getType());
			log.info("Received valid START IPC log from OutputSession");
		}catch (IOException e){
			fail("Could not read START message: "+e.getMessage());
		}
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

	@Test
	public void testValidHeartbeat(){
		UUID requestId = UUID.randomUUID();
		try {
			PrintWriter writer = new PrintWriter(ipcioSocket.getOutputStream(), true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(ipcioSocket.getInputStream()));
			IPCLog response = null;
			try{
				IPCInstruction instruction = new IPCInstruction();
				instruction.setType(IPCInstructionType.HEARTBEAT);
				instruction.setMessage(requestId.toString());
				try {
					writer.println(objectMapper.writeValueAsString(instruction));
					writer.flush();
				}catch (IOException e){
					fail("Couldn't send instruction");
				}

				String responseLine = reader.readLine();
				response = objectMapper.readValue(responseLine, IPCLog.class);
			}catch (IOException e) {
				fail("Couldn't read message from client socket: "+e.getMessage());
			}
			assertEquals(response.getType(), IPCLogType.EVENT);
			assertEquals(requestId.toString(), response.getMessage());
			log.info("Valid IPCIO Heartbeat success");
		} catch (IOException e) {
			fail("Couldn't send message through client socket: "+e.getMessage());
		}
	}

	@Test
	public void testInvalidHeartbeat(){
		String invalidHeartbeatRequest = "{ }";
		try {
			PrintWriter writer = new PrintWriter(ipcioSocket.getOutputStream(), true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(ipcioSocket.getInputStream()));
			try{
				log.info("Sending HeartbeatRequest: "+invalidHeartbeatRequest);
				writer.println(invalidHeartbeatRequest);
				writer.flush();
				String responseLine = reader.readLine();
				IPCLog invalidMessageLog = objectMapper.readValue(responseLine, IPCLog.class);
				assertEquals("Invalid message", invalidMessageLog.getMessage(),
						"Invalid response for invalid request: "+responseLine);
				log.info("Output valid for invalid request: "+responseLine);
			}catch (IOException e) {
				fail("Couldn't read message from client socket: "+e.getMessage());
			}
		} catch (IOException e) {
			fail("Couldn't send message through client socket: "+e.getMessage());
		}
	}

	@AfterAll
	public void cleanup(){
		try {
			ipcio.close();
			serverSocket.close();
		} catch (Exception e) {
			fail("Couldn't close IPCIO: "+e.getMessage());
		}
		session.stopSession();
		handlerThread.interrupt();
		TestHelper.deleteTmpConway();
	}


}

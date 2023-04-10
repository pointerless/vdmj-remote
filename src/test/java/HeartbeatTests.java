import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pointerless.vdmj.remote.Heartbeat;
import org.pointerless.vdmj.remote.SerializationHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HeartbeatTests {

	private static final ObjectMapper objectMapper = SerializationHelper.getObjectMapper();

	private Heartbeat heartbeat;
	private Thread heartbeatThread;

	private Socket clientSocket;

	@BeforeAll
	public void setup(){
		try {
			log.info("Setting up Heartbeat server");
			heartbeat = new Heartbeat(0);
			heartbeatThread = new Thread(heartbeat);
			heartbeatThread.setDaemon(true);
			heartbeatThread.start();
			log.info("Heartbeat server set up");
		} catch (IOException e) {
			fail("Couldn't start heartbeat server: "+e.getMessage());
		}
		try {
			clientSocket = new Socket("127.0.0.1", +heartbeat.getPort());
		} catch (IOException e){
			fail("Couldn't connect to heartbeat server: "+e.getMessage());
		}
	}

	@Test
	public void testValidHeartbeat(){
		UUID requestId = UUID.randomUUID();
		try {
			PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			Heartbeat.HeartbeatResponse response = null;
			try{
				Heartbeat.HeartbeatRequest request = new Heartbeat.HeartbeatRequest(requestId);
				log.info("Sending HeartbeatRequest: "+objectMapper.writeValueAsString(request));
				writer.println(objectMapper.writeValueAsString(request));
				writer.flush();
				String line = reader.readLine();
				response = objectMapper.readValue(line, Heartbeat.HeartbeatResponse.class);
				log.info("Received HeartbeatResponse: "+objectMapper.writeValueAsString(response));
			}catch (IOException e) {
				fail("Couldn't read message from client socket: "+e.getMessage());
			}
			assertEquals(requestId, response.getRequestId());
			log.info("Valid heartbeat success");
		} catch (IOException e) {
			fail("Couldn't send message through client socket: "+e.getMessage());
		}
	}

	@Test
	public void testInvalidHeartbeat(){
		String invalidHeartbeatRequest = "{'requestId':'"+UUID.randomUUID().toString().substring(0, 18)+"'}";
		try {
			PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			try{
				log.info("Sending HeartbeatRequest: "+invalidHeartbeatRequest);
				writer.println(invalidHeartbeatRequest);
				writer.flush();
				String line = reader.readLine();
				assertEquals(line, "false", "Invalid response for invalid request: "+line);
				log.info("Output valid for invalid request: "+line);
			}catch (IOException e) {
				fail("Couldn't read message from client socket: "+e.getMessage());
			}
		} catch (IOException e) {
			fail("Couldn't send message through client socket: "+e.getMessage());
		}
	}

	@AfterAll
	public void cleanup(){
		heartbeat.stop();
		heartbeatThread.interrupt();
	}

}

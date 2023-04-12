import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class TestHelper {

    private static Path tmpConway = null;

    public static Path getTmpConway(){

        if(tmpConway == null) {
            InputStream conwayVDMSL = TestHelper.class.getClassLoader().getResourceAsStream("Conway.vdmsl");

            assertNotNull(conwayVDMSL, "Couldn't find Conway.vdmsl in resources");

            try {
                tmpConway = Files.createTempFile("handler-test-Conway", ".vdmsl");
            } catch (IOException e) {
                fail("Couldn't create tmp Conway.vdmsl: "+e.getMessage());
            }

            try(OutputStream tmpOutput = new FileOutputStream(tmpConway.toFile())){
                conwayVDMSL.transferTo(tmpOutput);
            } catch (IOException e) {
                fail("Couldn't transfer Conway.vdmsl to tmp file: "+e.getMessage());
            }
        }

        return tmpConway;
    }

    public static void deleteTmpConway(){
        try {
            Files.delete(tmpConway);
            tmpConway = null;
        } catch (IOException e) {
            log.debug("Couldn't delete tmp Conway file: "+e.getMessage());
        }
    }

}

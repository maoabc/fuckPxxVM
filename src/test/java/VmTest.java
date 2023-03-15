import org.junit.jupiter.api.Test;
import pdd.vm.mw.MwBin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class VmTest {
    @Test
    public void testParse() throws IOException {
//        final FieldRefTypeSet fieldSet = new FieldRefTypeSet();
//        fieldSet.loadFromAndroidSdk();
        final InputStream input = getClass().getResourceAsStream("/mw1.bin");
        final File file = new File("/home/mao/mytest/alive.jar");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        MwBin.toClassesJar(input, file);
    }
}

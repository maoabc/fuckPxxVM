package pdd.vm.mw;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class SignData {
    private final LinkedHashMap<String, byte[]> map = new LinkedHashMap<>();

    public void readSignature(InputStream input) throws IOException {
        int readShort = MwDataUtils.readShort(input);
        for (int i = 0; i < readShort; i++) {
            String utf = MwDataUtils.readUTF(input);
            int dataLen = MwDataUtils.readShort(input);
            byte[] data = new byte[dataLen];
            MwDataUtils.readBytes(input, data, 0, dataLen);
            map.put(utf, data);
        }
    }

    public Map<String, byte[]> dataMap() {
        return this.map;
    }
}

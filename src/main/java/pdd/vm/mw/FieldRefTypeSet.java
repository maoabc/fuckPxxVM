package pdd.vm.mw;

import io.ApkUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 外部一些field集合，通过读取android.jar等得到field类型信息
 */
public class FieldRefTypeSet {
    //以类名+field名作为key,缓存从一些已知类的信息
    private final Map<FieldKey, String> fieldTypeMap = new HashMap<>();


    public void loadFromAndroidSdk() throws IOException {
        ApkUtils.getFiles(getClass().getResourceAsStream("/android.jar"), Pattern.compile(".*\\.class"), new ApkUtils.Extract() {
            @Override
            public void one(String entryName, byte[] data) {
                final ClassReader cr = new ClassReader(data);
                cr.accept(new ClassVisitor(Opcodes.ASM9) {
                    private String className;

                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        className = name;
                        super.visit(version, access, name, signature, superName, interfaces);
                    }

                    @Override
                    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                        fieldTypeMap.put(new FieldKey(className, name), descriptor);
                        return super.visitField(access, name, descriptor, signature, value);
                    }
                }, ClassReader.SKIP_CODE);
            }
        });
    }

    public void addFieldRef(String owner, String name, String desc) {
        fieldTypeMap.put(new FieldKey(owner, name), desc);
    }


    public String getFieldDesc(String clazz, String name) {
        final String type = fieldTypeMap.get(new FieldKey(clazz, name));
        if (type == null) {
            return "Lmw/unknownType;";
        }
        return type;
    }


    private static class FieldKey {
        private final String className;
        private final String name;

        FieldKey(String className, String name) {
            this.className = className;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FieldKey fieldKey = (FieldKey) o;

            if (!className.equals(fieldKey.className)) return false;
            return name.equals(fieldKey.name);
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }
    }
}

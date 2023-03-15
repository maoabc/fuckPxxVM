package pdd.vm.mw;

import java.util.Map;

public class ClassData {
    final int accessFlags;
    final String name;
    final String superName;
    final String[] ifacs;
    final Map<String, Map<String, Object>> annotations;
    final String signature;
    final FieldData[] fields;
    final MethodData[] methods;

    ClassData(int accessFlags, String name, String superName, String[] ifacs,
                     Map<String, Map<String, Object>> annotation, String signature,
                     FieldData[] fields, MethodData[] methods) {
        this.accessFlags = accessFlags;
        this.name = name;
        this.superName = superName;
        this.ifacs = ifacs;
        this.annotations = annotation;
        this.signature = signature;
        this.fields = fields;
        this.methods = methods;
    }

    public static class FieldData {
        final int accessFlags;
        final String name;
        final String desc;
        final Map<String, Map<String, Object>> annotations;
        final String signature;

        FieldData(int accessFlags, String name, String desc,
                         Map<String, Map<String, Object>> annotation,
                         String signature) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.desc = desc;
            this.annotations = annotation;
            this.signature = signature;
        }
    }

    public static class MethodData {
        final int accessFlags;
        final String name;
        final String desc;
        final Map<String, Map<String, Object>> annotations;
        final byte[] code;

        MethodData(int accessFlags, String name, String desc,
                          Map<String, Map<String, Object>> annotation,
                          byte[] code) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.desc = desc;
            this.annotations = annotation;
            this.code = code;
        }
    }
}

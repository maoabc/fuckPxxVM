package pdd.vm.mw;

import org.objectweb.asm.*;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 */
public class MwBin {


    private Object[] constPool;

    public List<ClassData> parse(byte[] data) throws IOException {
        final InputStream input = new ByteArrayInputStream(data);

        final byte[] buf = new byte[4];
        MwDataUtils.readBytes(input, buf, 0, 4);
        if (buf[0] != 0x1a || buf[1] != 0x0c || buf[2] != 0x06) {//前三个字节是magic
            throw new IOException("Illegal magic number");
        }
        //版本号
        final int version = buf[3];
        if (version >= 5) {//签名
            final SignData signData = new SignData();
            signData.readSignature(input);
        }
        constPool = Const.initConstPool(input);

        final ArrayList<ClassData> classes = new ArrayList<>();
        final int clazzSize = MwDataUtils.readShort(input);
        for (int i = 0; i < clazzSize; i++) {
            classes.add(parseClazz(input, version, constPool));
        }


        return classes;
    }

    //根据解析的信息，使用asm生成class
    public HashMap<String, byte[]> convert(List<ClassData> classes) throws IOException {
        final FieldRefTypeSet fieldRefTypeSet = new FieldRefTypeSet();

        //先从android sdk加载一些field信息
        fieldRefTypeSet.loadFromAndroidSdk();

        //再从自身加载field信息
        for (ClassData classData : classes) {
            for (ClassData.FieldData field : classData.fields) {
                fieldRefTypeSet.addFieldRef(classData.name, field.name, field.desc);
            }
        }
        //todo 可能还得从pdd apk里的classesX.dex里加载


        //保存所有生成的.class文件字节数组
        final HashMap<String, byte[]> all = new HashMap<>();

        for (ClassData cls : classes) {
            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cw.visit(Opcodes.V1_5, cls.accessFlags, cls.name, cls.signature, cls.superName, cls.ifacs);

            for (Map.Entry<String, Map<String, Object>> entry : cls.annotations.entrySet()) {
                final AnnotationVisitor av = cw.visitAnnotation(entry.getKey(), true);
                for (Map.Entry<String, Object> annEntry : entry.getValue().entrySet()) {
                    visitAnnotation(av, annEntry.getKey(), annEntry.getValue());
                }
                av.visitEnd();
            }

            for (ClassData.FieldData field : cls.fields) {
                final FieldVisitor fv = cw.visitField(field.accessFlags, field.name, field.desc, field.signature, null);
                for (Map.Entry<String, Map<String, Object>> entry : field.annotations.entrySet()) {
                    final AnnotationVisitor av = fv.visitAnnotation(entry.getKey(), true);
                    for (Map.Entry<String, Object> annEntry : entry.getValue().entrySet()) {
                        visitAnnotation(av, annEntry.getKey(), annEntry.getValue());
                    }
                    av.visitEnd();
                }
            }

            for (ClassData.MethodData method : cls.methods) {
                MethodVisitor mv = cw.visitMethod(method.accessFlags, method.name, method.desc, null, null);

                for (Map.Entry<String, Map<String, Object>> entry : method.annotations.entrySet()) {
                    final AnnotationVisitor av = mv.visitAnnotation(entry.getKey(), true);
                    for (Map.Entry<String, Object> annEntry : entry.getValue().entrySet()) {
                        visitAnnotation(av, annEntry.getKey(), annEntry.getValue());
                    }
                    av.visitEnd();
                }

                if (method.code != null) {
                    MwCode.parseCode(method.code, constPool, fieldRefTypeSet, mv);
                }
            }
            all.put(cls.name, cw.toByteArray());
        }
        return all;
    }


    //注解相关信息
    private static void visitAnnotation(AnnotationVisitor av, String name, Object obj) {
        if (obj instanceof List) {
            final AnnotationVisitor newAv = av.visitArray(name);
            for (Object o : ((List<?>) obj)) {
                visitAnnotation(newAv, null, o);
            }
            newAv.visitEnd();
        } else if (obj instanceof Const) {
            if (((Const) obj).tag == Const.TAG_CLASS) {
                av.visitAnnotation(name, (String) ((Const) obj).obj);
            }
        } else if (obj instanceof String[]) {
            //todo 好像有问题，测试后生成annotation没表示出字符串数组
            final Type type = Type.getType(obj.getClass());
            av.visit(name, type);
        } else {
            av.visit(name, obj);
        }
    }

    private static ClassData parseClazz(InputStream input, int version, Object[] pool) throws IOException {
        String clazzName = readStringConst(input, pool);
        String superName = readStringConst(input, pool);

        int interfaceSize = MwDataUtils.readByte(input);
        String[] ifacs = new String[interfaceSize];
        for (int i = 0; i < interfaceSize; i++) {
            ifacs[i] = readStringConst(input, pool);
        }

        final Map<String, Map<String, Object>> annotations = readAnnotation(input, pool);

        //泛型信息
        String signature = null;
        if (version >= 3) {
            final int idx = MwDataUtils.readShort(input);
            if (idx < 0xffff) {
                signature = (String) pool[idx];
            }
        }

        int flags = 0;
        if (version >= 4) {
            flags = MwDataUtils.readShort(input);
        }

        //读取field
        int fieldSize = MwDataUtils.readShort(input);
        final ClassData.FieldData[] fields = new ClassData.FieldData[fieldSize];
        for (int i = 0; i < fieldSize; i++) {
            String name = readStringConst(input, pool);
            String desc = readStringConst(input, pool);
            int fieldFlags = MwDataUtils.readShort(input);
            Map<String, Map<String, Object>> fieldAnnotations = readAnnotation(input, pool);
            String fieldSignature = null;
            int idx;
            if (version >= 3 && (idx = MwDataUtils.readShort(input)) < 65535) {
                fieldSignature = (String) pool[idx];
            }
            fields[i] = new ClassData.FieldData(fieldFlags, name, desc, fieldAnnotations, fieldSignature);
        }

        //读取方法
        int methodSize = MwDataUtils.readShort(input);
        final ClassData.MethodData[] methods = new ClassData.MethodData[methodSize];
        for (int i = 0; i < methodSize; i++) {
            String methodName = readStringConst(input, pool);
            String methodDesc = readStringConst(input, pool);
            int methodFlags = MwDataUtils.readShort(input);
            Map<String, Map<String, Object>> methodAnnotations = readAnnotation(input, pool);

            int codeSize = MwDataUtils.readShort(input);
            final byte[] code;
            if (codeSize > 0) {
                code = new byte[codeSize];
                MwDataUtils.readBytes(input, code, 0, codeSize);
            } else {//抽象方法 code为空
                code = null;
            }
            methods[i] = new ClassData.MethodData(methodFlags, methodName, methodDesc, methodAnnotations, code);

            readSign(input, version);
        }

        readSign(input, version);

        return new ClassData(flags, clazzName, superName, ifacs, annotations, signature, fields, methods);

    }


    private static String readStringConst(InputStream input, Object[] pool) throws IOException {
        return (String) pool[MwDataUtils.readShort(input)];
    }


    private static Map<String, Map<String, Object>> readAnnotation(InputStream inputStream, Object[] pool) throws IOException {
        int size = MwDataUtils.readByte(inputStream);
        HashMap<String, Map<String, Object>> hashMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Const c = (Const) pool[MwDataUtils.readShort(inputStream)];
            String str = (String) c.obj;
            ArrayList<?> arrayList = (ArrayList<?>) c.obj2;
            HashMap<String, Object> hashMap2 = new HashMap<>();
            for (int j = 0; j < arrayList.size(); j += 2) {
                hashMap2.put((String) arrayList.get(j), arrayList.get(j + 1));
            }
            hashMap.put(str, hashMap2);
        }
        return hashMap;
    }


    private static Map<String, byte[]> readSign(InputStream input, int version) throws IOException {
        if (version >= 5) {
            SignData signData = new SignData();
            signData.readSignature(input);
            return signData.dataMap();
        }
        return new LinkedHashMap<>();
    }


    public static void toClassesJar(File inFile, File dest) throws IOException {
        if (!inFile.exists()) {
            throw new FileNotFoundException(inFile.getAbsolutePath());
        }

        try (
                final FileInputStream input = new FileInputStream(inFile);
        ) {
            toClassesJar(input, dest);
        }
    }

    public static void toClassesJar(InputStream input, File dest) throws IOException {
        final byte[] bytes = MwDataUtils.readAllBytes(input);
        final MwBin mwBin = new MwBin();
        final List<ClassData> classData = mwBin.parse(bytes);

        final HashMap<String, byte[]> allClasses = mwBin.convert(classData);

        try (final ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(dest))) {
            for (Map.Entry<String, byte[]> entry : allClasses.entrySet()) {
                final ZipEntry zipEntry = new ZipEntry(entry.getKey() + ".class");
                zout.putNextEntry(zipEntry);
                copyStream(new ByteArrayInputStream(entry.getValue()), zout);
            }
        }
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        final byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }

}

package pdd.vm.mw;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MwCode {

    //对opcode进行分类，以便更好预处理
    public static final byte[] insnFormats;

    static {
        byte[] bytes = new byte[201];
        for (int i = 0; i < 201; i++) {
            bytes[i] = (byte) ("000000000000000011999222222222222222222222222200000000222222222222222222222222200000000000000000000000000000000000000000000000000000:0000000000000000000077777777777777772;<0000004444555563130033000=7777".charAt(i) - '0');
        }
        insnFormats = bytes;
    }


    public static void parseCode(byte[] insnBuf, Object[] pool, final FieldRefTypeSet fieldRefTypeSet, MethodVisitor mv) throws IOException {
        final ByteArrayInputStream input = new ByteArrayInputStream(insnBuf);

        //不需要使用这两个信息
        final int maxStack = MwDataUtils.readShort(input);
        final int maxLocals = MwDataUtils.readShort(input);
        //指令数
        final int insnCount = MwDataUtils.readShort(input);

        //指令
        final Insn[] insns = new Insn[insnCount];

        //第一次遍历指令记录跳转指令相关位置
        Map<Integer, Label> labels = new HashMap<>();

//        System.out.println("code " + maxLocals + "   " + insnCount);
        for (int insnIdx = 0; insnIdx < insnCount; insnIdx++) {
            final int opcode = MwDataUtils.readByte(input);
            final Insn insn = new Insn();
            insn.opcode = opcode;
            switch (insnFormats[opcode]) {
                case 0://无操作数
                    break;
                case 1: {//bipush及newarray相关
                    final int op1 = MwDataUtils.readShort(input);
                    if (opcode == 188) {
                        //这里不转换也没问题
                        switch (op1) {
                            case 4:
                                insn.op1 = Opcodes.T_BOOLEAN;
                                break;
                            case 5:
                                insn.op1 = Opcodes.T_CHAR;
                                break;
                            case 6:
                                insn.op1 = Opcodes.T_FLOAT;
                                break;
                            case 7:
                                insn.op1 = Opcodes.T_DOUBLE;
                                break;
                            case 8:
                                insn.op1 = Opcodes.T_BYTE;
                                break;
                            case 9:
                                insn.op1 = Opcodes.T_SHORT;
                                break;
                            case 10:
                                insn.op1 = Opcodes.T_INT;
                                break;
                            case 11:
                                insn.op1 = Opcodes.T_LONG;
                                break;
                        }
                    } else {
                        insn.op1 = op1;
                    }
                    break;
                }
                case 2: {//iload相关
                    int idx = MwDataUtils.readByte(input);
                    if (idx == 0xff) {
                        idx = MwDataUtils.readShort(input);
                    }
                    insn.op1 = idx;
                    break;
                }
                case 3: {//checkcast,instanceof相关
                    String clazz = (String) pool[MwDataUtils.readShort(input)];
                    insn.op1 = clazz;
                    break;
                }
                case 4: {//field操作相关
                    final Const field = (Const) pool[MwDataUtils.readShort(input)];

                    //常量池中field被去掉了类型信息，目前只能简单的读取一些常见的sdk恢复部分
                    //准确信息应该需要运行时获得
                    String desc = fieldRefTypeSet.getFieldDesc((String) field.obj, (String) field.obj2);

                    insn.op1 = new FieldRef((String) field.obj, (String) field.obj2, desc);
                    break;
                }
                case 5: {//invokemehod相关
                    final Const method = (Const) pool[MwDataUtils.readShort(input)];
                    insn.op1 = new MethodRef((String) method.obj, (String) method.obj2, (String) method.obj3);
                    break;
                }
                case 6: {//invokedynamic
                    System.err.println("insn " + insn);
                    break;
                }
                case 7: {//ifne等跳转指令
                    final int offset = MwDataUtils.readShort(input);
                    labels.put(offset, new Label());
                    insn.op1 = offset;
                    break;
                }
//                case 8: {
//                    break;
//                }
                case 9: {//ldc系列指令
                    final Object obj = pool[MwDataUtils.readShort(input)];
                    if (obj instanceof Const) {// ldc XX.class
                        insn.op1 = new ClazzRef((String) ((Const) obj).obj);
                    } else {
                        insn.op1 = obj;
                    }
                    break;
                }
                case 10: {//iinc
                    final int idx = MwDataUtils.readByte(input);
                    if (idx == 0xff) {
                        insn.op1 = MwDataUtils.readShort(input);
                        insn.op2 = (short) MwDataUtils.readShort(input);
                    } else {
                        insn.op1 = idx;
                        insn.op2 = (byte) MwDataUtils.readByte(input);
                    }
                    break;
                }
                case 11: {//tableswitch
                    final int low = MwDataUtils.readInt(input);
                    final int high = MwDataUtils.readInt(input);

                    final int def = MwDataUtils.readShort(input);

                    labels.put(def, new Label());

                    final int[] table = new int[high - low + 1];
                    for (int i = 0; i < table.length; i++) {
                        final int offset = MwDataUtils.readShort(input);
                        table[i] = offset;
                        //
                        labels.put(offset, new Label());
                    }
                    insn.op1 = new TableSwitch(low, high, def, table);
                    break;
                }
                case 12: {//lookupswitch
                    final HashMap<Integer, Integer> lookup = new HashMap<>();
                    int numPairs = MwDataUtils.readShort(input);
                    for (int i = 0; i < numPairs; i++) {
                        final int key = MwDataUtils.readInt(input);
                        final int off = MwDataUtils.readShort(input);
                        lookup.put(key, off);

                        labels.put(off, new Label());
                    }
                    insn.op1 = lookup;
                    break;
                }
                case 13: {//multianewarray
                    final String type = (String) pool[MwDataUtils.readShort(input)];
                    //dimensions
                    final int d = MwDataUtils.readByte(input);
                    insn.op1 = type;
                    insn.op2 = d;
                    break;
                }
            }
            insns[insnIdx] = insn;
        }
        //异常表相关
        final int exceptionTableSize = MwDataUtils.readByte(input);
        final TryCatch[] tryCatches = new TryCatch[exceptionTableSize];
        for (int i = 0; i < exceptionTableSize; i++) {
            final int start = MwDataUtils.readShort(input);
            final int end = MwDataUtils.readShort(input);
            final int handler = MwDataUtils.readShort(input);

            final String type;
            final int typeIdx = MwDataUtils.readShort(input);
            if (typeIdx != 0xffff) {
                type = (String) pool[typeIdx];
            } else {
                //catch all
                type = null;
            }
            tryCatches[i] = new TryCatch(start, end, handler, type);


            labels.put(start, new Label());
            labels.put(end, new Label());
            labels.put(handler, new Label());
        }
        interpret(mv, insns, labels, tryCatches);
    }

    private static void interpret(MethodVisitor mv, Insn[] insns, Map<Integer, Label> labels, TryCatch[] tryCatches) {
        mv.visitCode();
        //目前可以简单实现
        //最好应该每条指令单独通过asm生成，哪怕pdd以后指令变化也能很好的跟进

        for (int pc = 0; pc < insns.length; pc++) {
            final Insn insn = insns[pc];

            Label label;
            if ((label = labels.get(pc)) != null) {//添加label信息
                mv.visitLabel(label);
            }
            switch (insnFormats[insn.opcode]) {
                case 0: {
                    mv.visitInsn(insn.opcode);
                    break;
                }
                case 1: {
                    switch (insn.opcode) {
                        case 16:
                        case 17: {
                            mv.visitIntInsn(insn.opcode, (Integer) insn.op1);
                            break;
                        }
                        case 188: {//newarray
                            //更好应该需要对基本类型进行一个转换
                            mv.visitIntInsn(Opcodes.NEWARRAY, (Integer) insn.op1);
                        }
                    }
                    break;
                }
                case 2: {
                    mv.visitVarInsn(insn.opcode, (Integer) insn.op1);
                    break;
                }
                case 3: {
                    mv.visitTypeInsn(insn.opcode, (String) insn.op1);
                    break;
                }
                case 4: {
                    final FieldRef field = (FieldRef) insn.op1;

                    mv.visitFieldInsn(insn.opcode, field.owner, field.name, field.desc);
                    break;
                }
                case 5: {
                    final MethodRef method = (MethodRef) insn.op1;
                    mv.visitMethodInsn(insn.opcode, method.owner, method.name, method.desc, insn.opcode == Opcodes.INVOKEINTERFACE);
                    break;
                }
                case 7: {//跳转指令
                    Integer off = (Integer) insn.op1;
                    final Label label1 = labels.get(off);
                    mv.visitJumpInsn(insn.opcode, label1);
                    break;
                }
                case 9: {//ldc
                    if (insn.op1 instanceof ClazzRef) {//ldc class type
                        final Type clazz = Type.getType(((ClazzRef) insn.op1).type);
                        mv.visitLdcInsn(clazz);
                    } else {//ldc int,string
                        mv.visitLdcInsn(insn.op1);
                    }
                    break;
                }
                case 10: {
                    if (insn.op2 instanceof Byte) {
                        mv.visitIincInsn((Integer) insn.op1, (Byte) insn.op2);
                    } else if (insn.op2 instanceof Short) {
                        mv.visitIincInsn((Integer) insn.op1, (Short) insn.op2);
                    }
                    break;
                }
                case 11: {
                    TableSwitch tableSwitch = (TableSwitch) insn.op1;
                    final Label defLabel = labels.get(tableSwitch.def);

                    final Label[] labels1 = new Label[tableSwitch.table.length];
                    for (int i = 0; i < tableSwitch.table.length; i++) {
                        labels1[i] = labels.get(tableSwitch.table[i]);
                    }
                    mv.visitTableSwitchInsn(tableSwitch.low, tableSwitch.high, defLabel, labels1);
                    break;
                }
                case 12: {
                    Map<Integer, Integer> lookup = (Map<Integer, Integer>) insn.op1;
                    //以最大Int为key是默认label
                    final Label defLabel = labels.get(lookup.remove(Integer.MAX_VALUE));

                    final int size = lookup.size();
                    final int[] keys = new int[size];
                    final Label[] labels1 = new Label[size];
                    int i = 0;
                    for (Map.Entry<Integer, Integer> entry : lookup.entrySet()) {
                        keys[i] = entry.getKey();
                        labels1[i] = labels.get(entry.getValue());
                        i++;
                    }
                    mv.visitLookupSwitchInsn(defLabel, keys, labels1);
                    break;
                }
                case 13: {
                    mv.visitMultiANewArrayInsn((String) insn.op1, (Integer) insn.op2);
                    break;
                }

                default:

            }
        }

        //异常表
        for (TryCatch tryCatch : tryCatches) {
            mv.visitTryCatchBlock(labels.get(tryCatch.start), labels.get(tryCatch.end), labels.get(tryCatch.handler), tryCatch.type);
        }

        mv.visitEnd();

    }


    private static class TryCatch {
        final int start;
        final int end;
        final int handler;

        TryCatch(int start, int end, int handler, String type) {
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.type = type;
        }

        final String type;
    }

    private static class TableSwitch {
        final int low;
        final int high;
        final int def;
        final int[] table;

        TableSwitch(int low, int high, int def, int[] table) {
            this.low = low;
            this.high = high;
            this.def = def;
            this.table = table;
        }
    }

    private static class FieldRef {
        final String owner;
        final String name;
        final String desc;

        FieldRef(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }
    }

    private static class MethodRef {
        final String owner;
        final String name;
        final String desc;

        MethodRef(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }
    }

    private static class ClazzRef {
        final String type;

        public ClazzRef(String type) {
            this.type = type;
        }
    }

}

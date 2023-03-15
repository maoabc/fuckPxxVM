package pdd.vm;

import pdd.vm.mw.MwBin;

import java.io.File;
import java.io.IOException;

public class VmMain {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("mw.bin dest.jar");
        }
        MwBin.toClassesJar(new File(args[0]), new File(args[1]));
    }
}

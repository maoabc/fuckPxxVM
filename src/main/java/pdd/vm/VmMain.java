package pdd.vm;

import pdd.vm.mw.MwBin;

import java.io.File;
import java.io.IOException;

public class VmMain {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("mw.bin dest.jar");
        }
        File dest;
        if(args.length<2){
            dest=new File("out.jar");
        }else {
            dest=new File(args[1]);
        }
        MwBin.toClassesJar(new File(args[0]), dest);
    }
}

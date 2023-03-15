
拼xx的apk里.7z后缀的文件名，实际为tar.lzma格式文件，一开始用16进制打开还懵了。解压后mw1.bin为自定义的格式，也就是拼xx的vmp执行格式。
vm是用java实现的栈式虚拟机，甚至字节码都是标准的java bytecode。尝试分析其虚拟机执行，然后用java asm库生成class文件。

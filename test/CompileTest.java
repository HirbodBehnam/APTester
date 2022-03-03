import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class CompileTest {
    enum CompileStatus {
        PENDING,
        COMPILED,
        FAILED
    }

    public static CompileStatus compileStatus = CompileStatus.PENDING;

    public static void compileProject() {
        if (compileStatus != CompileStatus.PENDING)
            return;
        System.out.println("COMPILING");
        try {
            ProcessBuilder pb = new ProcessBuilder("javac", "Main.java");
            pb.directory(new File(APTest.javaFilePath));
            Process p = pb.start();
            Scanner scanner = new Scanner(p.getErrorStream());
            if (p.waitFor() != 0) {
                while (scanner.hasNextLine())
                    System.err.println(scanner.nextLine());
                System.err.println("COMPILE FAILED:");
                compileStatus = CompileStatus.FAILED;
                return;
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            compileStatus = CompileStatus.FAILED;
            return;
        }
        System.out.println("COMPILE DONE");
        System.out.println("===============");
        compileStatus = CompileStatus.COMPILED;
    }

    @Test
    public void compileTest() {
        compileProject();
        Assert.assertEquals(CompileStatus.COMPILED, compileStatus);
    }
}

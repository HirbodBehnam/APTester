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

    // Compile the project only if compileStatus is not pending
    public static void compileProject() {
        if (compileStatus != CompileStatus.PENDING)
            return;
        // Compile with javac
        System.out.println("COMPILING");
        try {
            ProcessBuilder pb = new ProcessBuilder("javac", "Main.java");
            pb.directory(new File(APTest.javaFilePath)); // Compile in source folder
            Process p = pb.start();
            Scanner scanner = new Scanner(p.getErrorStream());
            if (p.waitFor() != 0) { // Check for compile errors
                System.out.println("COMPILE FAILED");
                System.err.println("COMPILE FAILED:");
                while (scanner.hasNextLine())
                    System.err.println(scanner.nextLine());
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

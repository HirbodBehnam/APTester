import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

@RunWith(Parameterized.class)
public class APTest {
    public final static String javaFilePath = "src/main/java/";
    private final static String testsPath = "src/test/java/in";
    private final static String inputFilePathPrefix = "src/test/java/in/input";
    private final static String outputFilePathPrefix = "src/test/java/out/output";

    @Parameterized.Parameter(0)
    public String inputFilePath;
    @Parameterized.Parameter(1)
    public String outputFilePath;

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        // Get number of tests
        int numberOfTests = Objects.requireNonNull(new File(testsPath).listFiles(
                (dir, name) -> name.matches("input[0-9]+.txt"))
        ).length;
        System.err.println("Found " + numberOfTests + " tests");
        // Create the parameters
        Object[][] retVal = new Object[numberOfTests][2];
        for (int i = 1; i <= numberOfTests; i++) {
            retVal[i - 1][0] = inputFilePathPrefix + i + ".txt";
            retVal[i - 1][1] = outputFilePathPrefix + i + ".txt";
        }
        return Arrays.asList(retVal);
    }

    @BeforeClass
    public static void compileFiles() {
        CompileTest.compileProject();
    }

    @Test
    public void judgeTest() {
        if (CompileTest.compileStatus != CompileTest.CompileStatus.COMPILED)
            Assert.fail("no code to run! (Compile error)");
        // Debug stuff
        System.out.println("TEST:");
        System.out.println(this.inputFilePath);
        System.out.println(this.outputFilePath);
        System.out.println("########################");
        // Run java to test the app
        boolean ok;
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "Main");
            pb.directory(new File(javaFilePath)); // Run in the test folder
            pb.redirectInput(new File(this.inputFilePath)); // Set stdin to the test file
            Process p = pb.start();
            ok = answerIsOk(outputFilePath, new Scanner(p.getInputStream())); // FUCK JAVA; THIS IS STDOUT
            if (p.waitFor() != 0) { // Catch exceptions or java errors idk
                System.err.println("abnormal java exit code on test " + this.inputFilePath);
                Scanner sc = new Scanner(p.getErrorStream());
                while (sc.hasNextLine())
                    System.err.println(sc.nextLine());
                Assert.fail("Runtime Error");
                return;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Assert.fail("Process killed or something IDK");
            return; // Junit...
        }
        // Differentiate the outputs
        Assert.assertTrue("Wrong answer", ok);
    }

    // Thanks to Arshia Akhavan for this code
    private static boolean answerIsOk(String outputFilePath, Scanner resultScanner) {
        try {
            Scanner outputScanner = new Scanner(new FileInputStream(outputFilePath));
            while (outputScanner.hasNextLine() && resultScanner.hasNextLine())
                if (!(outputScanner.nextLine().trim().equals(resultScanner.nextLine().trim())))
                    return false;
            if (outputScanner.hasNextLine() || resultScanner.hasNextLine())
                return false;
        } catch (FileNotFoundException e) {
            System.err.println("cannot open the output file of test: " + outputFilePath);
            e.printStackTrace();
            return false;
        }
        return true;
    }
}


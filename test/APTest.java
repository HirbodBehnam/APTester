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
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

@RunWith(Parameterized.class)
public class APTest {
    public final static String javaFilePath = "src/main/java/";
    private final static String testsPath = "src/test/java/in";
    private final static String inputFilePathPrefix = "src/test/java/in/input";
    private final static String outputFilePathPrefix = "src/test/java/out/output";
    private final static String runtimeErrorMessage = "Runtime error";

    // This is a map of inputFilename -> result
    // If a key doesn't exist it means that we have to judge the test
    // Also "" means that the test was ok
    private final static HashMap<String, String> resultMap = new HashMap<>();

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
        String result = getResultOrCheckIfNeeded(inputFilePath, outputFilePath);
        if (!result.isEmpty())
            Assert.fail(result);
    }

    @Test
    public void runtimeTest() {
        if (CompileTest.compileStatus != CompileTest.CompileStatus.COMPILED)
            Assert.fail("no code to check! (Compile error)");
        if (getResultOrCheckIfNeeded(inputFilePath, outputFilePath).equals(runtimeErrorMessage))
            Assert.fail(runtimeErrorMessage);
    }

    private static String getResultOrCheckIfNeeded(String inputFilePath, String outputFilePath) {
        if (resultMap.containsKey(inputFilePath))
            return resultMap.get(inputFilePath);
        String result = testCode(inputFilePath, outputFilePath);
        resultMap.put(inputFilePath, result);
        return result;
    }

    private static String testCode(String inputFilePath, String outputFilePath) {
        // Debug stuff
        System.out.println("TEST:");
        System.out.println(inputFilePath);
        System.out.println(outputFilePath);
        System.out.println("########################");
        // Run java to test the app
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "Main");
            pb.directory(new File(javaFilePath)); // Run in the test folder
            pb.redirectInput(new File(inputFilePath)); // Set stdin to the test file
            Process p = pb.start();
            boolean ok = answerIsOk(outputFilePath, new Scanner(p.getInputStream())); // FUCK JAVA; THIS IS STDOUT
            if (p.waitFor() != 0) { // Catch exceptions or java errors idk
                System.err.println("abnormal java exit code on test " + inputFilePath);
                Scanner sc = new Scanner(p.getErrorStream());
                while (sc.hasNextLine())
                    System.err.println(sc.nextLine());
                return runtimeErrorMessage;
            }
            // Check answer
            return ok ? "" : "Wrong answer";
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Process killed or something IDK";
        }
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


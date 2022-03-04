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

    /**
     * This enum simply defines the test result
     */
    enum TestResult {
        PASS,
        RUNTIME_ERROR,
        WRONG_ANSWER,
        EXCEPTION_IN_JUDGE; // This should never happen


        @Override
        public String toString() {
            switch (this) {
                case PASS:
                    return "PASS";
                case WRONG_ANSWER:
                    return "Wrong Answer";
                case RUNTIME_ERROR:
                    return "Runtime Error";
                case EXCEPTION_IN_JUDGE:
                    return "Judge Error";
            }
            return super.toString();
        }
    }

    /**
     * This is a map of inputFilename -> result of test
     * If a key doesn't exist it means that we have to judge the test
     */
    private final static HashMap<String, TestResult> resultMap = new HashMap<>();

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
        System.out.println("Found " + numberOfTests + " tests");
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
        // Compile project at first
        CompileTest.compileProject();
    }

    @Test
    public void judgeTest() {
        // Fail if there is a compile error
        if (CompileTest.compileStatus != CompileTest.CompileStatus.COMPILED)
            Assert.fail("no code to run! (Compile error)");
        // Fail on anything but PASS
        TestResult result = getResultOrCheckIfNeeded(inputFilePath, outputFilePath);
        if (result != TestResult.PASS)
            Assert.fail(result.toString());
    }

    @Test
    public void runtimeTest() {
        // Fail if there is a compile error
        if (CompileTest.compileStatus != CompileTest.CompileStatus.COMPILED)
            Assert.fail("no code to check! (Compile error)");
        // Fail if there is a runtime
        if (getResultOrCheckIfNeeded(inputFilePath, outputFilePath) == TestResult.RUNTIME_ERROR)
            Assert.fail(TestResult.RUNTIME_ERROR.toString());
    }

    private static TestResult getResultOrCheckIfNeeded(String inputFilePath, String outputFilePath) {
        if (resultMap.containsKey(inputFilePath))
            return resultMap.get(inputFilePath);
        TestResult result = testCode(inputFilePath, outputFilePath);
        resultMap.put(inputFilePath, result);
        return result;
    }

    private static TestResult testCode(String inputFilePath, String outputFilePath) {
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
                return TestResult.RUNTIME_ERROR;
            }
            // Check answer
            return ok ? TestResult.PASS : TestResult.WRONG_ANSWER;
        } catch (IOException | InterruptedException e) {
            System.err.println("JUDGE EXCEPTION ON TEST " + inputFilePath);
            e.printStackTrace();
            return TestResult.EXCEPTION_IN_JUDGE;
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


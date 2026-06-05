import java.io.*;
import java.nio.file.*;
import java.util.Random;

public class Puzzle {

    // Seed-controlled "noise" so output is deterministic
    private static final long NOISE_SEED = 0xDEADBEEFL;

    // The real key fragments — when assembled in order, form the vector input
    // Example: final key = "4.2,-1.7,3.1|−2.5,0.8,−4.4|1.1,3.3,−2.2"
    private static final String[] REAL_FRAGMENTS = {
        "4.2,-1.7,",   // frag_0 → goes in a legit-looking binary file
        "3.1|-2.5,",   // frag_1 → buried in a large decoy text file
        "0.8,-4.4|",   // frag_2 → hidden in a dotfile
        "1.1,3.3,-2.2" // frag_3 → inside a file with a misleading name
    };

    // Red herring "fake" fragments — look like real vector data but are wrong
    private static final String[] FAKE_FRAGMENTS = {
        "9.9,-3.3,1.5",
        "0.0,1.0,0.0",
        "-7.2,4.1,2.8",
        "3.14,2.71,1.41",
        "1.0,-1.0,1.0"
    };

    public static void main(String[] args) throws Exception {

        Random rng = new Random(NOISE_SEED);

        // --- REAL FRAGMENT FILES ---

        // Fragment 0: buried in binary noise
        writeWithBinaryNoise("data.bin",    REAL_FRAGMENTS[0], rng);

        // Fragment 1: buried at line 3333 of a 7000-line decoy log
        writeBuriedInLog("system.log",      REAL_FRAGMENTS[1], 3333, 7000);

        // Fragment 2: hidden in a dotfile (hidden from casual ls)
        writeDotfile(".cache",              REAL_FRAGMENTS[2], rng);

        // Fragment 3: inside a file with a deceptive name
        writeWithBinaryNoise("README.bin",  REAL_FRAGMENTS[3], rng);


        // --- RED HERRING FILES ---

        // Fake 0: plain text, very visible — obvious trap
        Files.write(Paths.get("key.txt"),
            ("VECTOR_KEY: " + FAKE_FRAGMENTS[0]).getBytes());

        // Fake 1: in its own binary file, looks just like real ones
        writeWithBinaryNoise("config.bin",  FAKE_FRAGMENTS[1], rng);

        // Fake 2: buried in a second log at a similar line depth
        writeBuriedInLog("debug.log",       FAKE_FRAGMENTS[2], 3001, 6000);

        // Fake 3: in a dotfile just like the real one, different name
        writeDotfile(".config",             FAKE_FRAGMENTS[3], rng);

        // Fake 4: written in a file whose name implies it's important
        Files.write(Paths.get("ANSWER.txt"),
            ("Try harder: " + FAKE_FRAGMENTS[4]).getBytes());


        System.out.println("Environment initialized. Find the signal in the noise.");
    }


    // --- Helpers ---

    /**
     * Writes a fragment surrounded by deterministic random bytes.
     * Readable via: strings <file> | grep -E '[0-9.-]+,[0-9.-]+'
     */
    static void writeWithBinaryNoise(String filename, String fragment, Random rng)
            throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        byte[] before = new byte[128];
        byte[] after  = new byte[128];
        rng.nextBytes(before);
        rng.nextBytes(after);

        buf.write(before);
        buf.write(fragment.getBytes());
        buf.write(after);

        Files.write(Paths.get(filename), buf.toByteArray());
    }

    /**
     * Writes a large plaintext log with the fragment at a specific line.
     * Readable via: awk 'NR==<line>' <file>  OR  grep -n '[0-9.-]+,'
     */
    static void writeBuriedInLog(String filename, String fragment,
                                  int targetLine, int totalLines)
            throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename));
        for (int i = 1; i <= totalLines; i++) {
            if (i == targetLine) {
                pw.println("SYS_DATA: " + fragment);
            } else {
                pw.println("LOG_ENTRY_" + i + ": nominal");
            }
        }
        pw.close();
    }

    /**
     * Writes a hidden dotfile with noise + fragment.
     * Solver needs: ls -la  then  strings .<name>
     */
    static void writeDotfile(String filename, String fragment, Random rng)
            throws IOException {
        writeWithBinaryNoise(filename, fragment, rng);
    }
}
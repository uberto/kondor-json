import org.codehaus.mojo.animal_sniffer.SignatureBuilder;
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// Builds an Animal Sniffer signature of the Java API (java.* and javax.*) of an Android version, from the android.jar
// of its SDK platform. Run by scripts/generate-android-signature.sh.
public class BuildAndroidSignature {
    public static void main(String[] args) throws IOException {
        File androidJar = new File(args[0]);
        File signature = new File(args[1]);
        try (OutputStream out = new FileOutputStream(signature)) {
            SignatureBuilder builder = new SignatureBuilder(out, new PrintWriterLogger(System.err));
            builder.addInclude("java.*");
            builder.addInclude("javax.*");
            builder.process(androidJar);
            builder.close();
        }
    }
}

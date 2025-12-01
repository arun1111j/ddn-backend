import org.web3j.codegen.SolidityFunctionWrapperGenerator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GenerateWrapper {
    public static void main(String[] args) {
        try {
            String binFile = "target/contracts/DocumentNotarization.bin";
            String abiFile = "target/contracts/DocumentNotarization.abi";
            String outputDir = "src/main/java";
            String packageName = "com.notarize.contracts";
            String className = "DocumentNotarization";

            // Read bin and abi content
            String bin = new String(Files.readAllBytes(Paths.get(binFile)));
            String abi = new String(Files.readAllBytes(Paths.get(abiFile)));

            // Generate Java wrapper
            SolidityFunctionWrapperGenerator.main(new String[] {
                    "-a", abiFile,
                    "-b", binFile,
                    "-o", outputDir,
                    "-p", packageName,
                    "-c", className
            });

            System.out.println("Wrapper generated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to generate wrapper: " + e.getMessage());
        }
    }
}

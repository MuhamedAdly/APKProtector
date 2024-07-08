import java.util.*;
import java.util.regex.*;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.io.*;

public class APKProtector {
    private static byte[] secretKey;

    public static void main(String[] args) throws Exception {
        String apkPath = null;
        List<String> files = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--apk") && i + 1 < args.length) {
                apkPath = args[i + 1];
                i++;
            } else if (args[i].equals("--files")) {
                while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    String filePath = args[++i].replace(".", "/");
                    files.add(filePath);
                }
            }
        }

        if (apkPath == null || files.isEmpty()) {
            System.out.println("Usage: java APKProtector --apk <path-to-apk> --files <filePath1> [<filePath2> ...]");
            return;
        }

        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            System.out.println("APK file does not exist.");
            return;
        }

        // Decompile the APK
        String apkDir = apkFile.getName().replace(".apk", "");
        executeCommand(new String[]{"apktool", "d","-f", apkPath});
        
        String bundleId = getAppBundleId(apkPath);
        String customAppClassName = "application";

        // Generate a secret key
        secretKey = generateSecretKey(32); // Generating a 32-byte key
        
        // Generate the C++ file that will embed the secret key
        JNIHelper.generateAndCompileJNI(bundleId,apkDir,secretKey,customAppClassName);
       
        // Generate the custom application class and update the manifest file.
        ApplicationGenerator.writeApplicationSmaliFile(apkDir,bundleId,customAppClassName);
        ApplicationGenerator.updateAndroidManifest(apkDir,bundleId,customAppClassName);

        //Encrypt strings in Smali files
        Encryptor.encryptStrings(apkDir+"/"+"smali", bundleId,secretKey,customAppClassName,files);

        // Rebuild APK
        executeCommand(new String[]{"apktool", "b","-f", apkDir});

        // Sign the APK
        String unsignedApk = apkDir + "/dist/" + apkDir + ".apk";
        String signedApk = apkDir + "-signed.apk";
        signApk(unsignedApk, signedApk);

        System.out.println("Encrypted APK: " + signedApk);
    }

    private static byte[] generateSecretKey(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[length];
        secureRandom.nextBytes(key);
        return key;
    }

    private static String executeCommand(String[] command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // Merge stdout and stderr
        Process process = pb.start();

        // Read the output
        String commandOutput;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            commandOutput = reader.lines().collect(Collectors.joining("\n"));
        }

        // Check the exit code
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode);
        }

        return commandOutput;
    }

    private static void signApk(String inputApk, String outputApk) throws Exception {
         String buildToolsPath = System.getenv("ANDROID_BUILD_TOOLS");
        String cmd = buildToolsPath + "/apksigner sign --ks testkey.jks --ks-key-alias test --ks-pass pass:test123 --key-pass pass:test123 --out " + outputApk + " " + inputApk;
        Runtime.getRuntime().exec(cmd).waitFor();
    }

    private static String getAppBundleId(String apkPath) {
        try {
            String sdkPath = getAndroidSdkPath();
            if (sdkPath != null) {
                String aaptPath = findAapt(sdkPath);
                if (aaptPath != null) {
                    String output = executeCommand(new String[]{aaptPath, "dump", "badging", apkPath});
                    String bundleId = parsePackageName(output);
                    return bundleId;
                } else {
                    System.out.println("aapt not found in the Android SDK.");
                }
            } else {
                System.out.println("ANDROID_HOME or ANDROID_SDK_ROOT environment variable is not set.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "cann't find the bundle Id";
    }

    private static String parsePackageName(String output) {
        Pattern pattern = Pattern.compile("package: name='([^']+)'");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Package name not found";
    }

    private static String getAndroidSdkPath() {
        String sdkPath = System.getenv("ANDROID_HOME");
        if (sdkPath == null) {
            sdkPath = System.getenv("ANDROID_SDK_ROOT");
        }
        return sdkPath;
    }

    private static String findAapt(String sdkPath) throws IOException {
        File buildToolsDir = new File(sdkPath, "build-tools");
        if (!buildToolsDir.exists()) {
            throw new IOException("build-tools directory not found in SDK path: " + sdkPath);
        }

        List<File> aaptFiles = new ArrayList<>();
        findAaptInDirectory(buildToolsDir, aaptFiles);

        if (!aaptFiles.isEmpty()) {
            // Return the first found aapt tool
            return aaptFiles.get(0).getAbsolutePath();
        }
        return null;
    }

    private static void findAaptInDirectory(File dir, List<File> aaptFiles) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    findAaptInDirectory(file, aaptFiles);
                } else if (file.getName().equals("aapt") || file.getName().equals("aapt.exe")) {
                    aaptFiles.add(file);
                }
            }
        }
    }

}
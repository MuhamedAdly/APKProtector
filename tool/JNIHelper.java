import java.io.*;
import java.util.Arrays;

public class JNIHelper {
	
	public static void generateAndCompileJNI(String bundleId, String decompiledDir, byte[] secretKey, String customAppClass) {
        try {

            // Retrieve ANDROID_NDK_HOME environment variable
            String ndkPath = System.getenv("ANDROID_NDK_HOME");
            if (ndkPath == null || ndkPath.isEmpty()) {
                throw new RuntimeException("ANDROID_NDK_HOME environment variable is not set.");
            }

            // Path to store the generated C++ file
            String cppFilePath = "src/main/jni/encryptor-lib.cpp";
            
            // Create the directory for the generated C++ file if it doesn't exist
            new File(cppFilePath).getParentFile().mkdirs();

            // Create directories for jni and jniLibs
            String jniDirPath = "src/main/jni";
            String jniLibsDirPath = "src/main/jniLibs";
            new File(jniDirPath).mkdirs();
            new File(jniLibsDirPath).mkdirs();

            // Write sample C++ code to a file with the generated secret key and bundle ID
            writeCppFile(cppFilePath, secretKey, bundleId, customAppClass);

            // Generate the Android.mk and Application.mk files
            writeAndroidMkFile(jniDirPath);
            writeApplicationMkFile(jniDirPath);

            // Compile the C++ code using ndk-build
            compileWithNdkBuild(ndkPath);

            // Copy the jniLibs to the decompiled directory
            copyJniLibraries(jniLibsDirPath,decompiledDir);

            System.out.println("Native libraries generated and compiled successfully.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void writeCppFile(String filePath, byte[] secretKey, String bundleId, String customAppClass) throws IOException {
        // Convert byte array to a C++ array initializer
        StringBuilder keyArrayStr = new StringBuilder();
        for (byte b : secretKey) {
            keyArrayStr.append(String.format("0x%02X, ", b));
        }

        // Remove the last comma and space
        if (keyArrayStr.length() > 0) {
            keyArrayStr.setLength(keyArrayStr.length() - 2);
        }

        String cppCode = "#include <jni.h>\n" +
                         "#include <vector>\n" +
                         "\n" +
                         "extern \"C\" JNIEXPORT jbyteArray JNICALL\n" +
                         "Java_" + bundleId.replace('.', '_') + "_"+ customAppClass +"_getKey(\n" +
                         "        JNIEnv* env,\n" +
                         "        jobject /* this */) {\n" +
                         "    std::vector<uint8_t> key = {" + keyArrayStr.toString() + "};\n" +
                         "    jbyteArray jKey = env->NewByteArray(key.size());\n" +
                         "    env->SetByteArrayRegion(jKey, 0, key.size(), reinterpret_cast<jbyte*>(key.data()));\n" +
                         "    return jKey;\n" +
                         "}\n";

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(cppCode);
        }
    }

    private static void writeAndroidMkFile(String jniDirPath) throws IOException {
        String androidMk = "LOCAL_PATH := $(call my-dir)\n" +
                           "include $(CLEAR_VARS)\n" +
                           "LOCAL_MODULE := encryptor-lib\n" +
                           "LOCAL_SRC_FILES := encryptor-lib.cpp\n" +
                           "include $(BUILD_SHARED_LIBRARY)\n";

        try (FileWriter writer = new FileWriter(jniDirPath + "/Android.mk")) {
            writer.write(androidMk);
        }
    }

    private static void writeApplicationMkFile(String jniDirPath) throws IOException {
        String applicationMk =  "APP_STL := c++_static\n" +
                                "APP_ABI := armeabi-v7a arm64-v8a x86 x86_64\n" +
                                "APP_PLATFORM := android-21\n";

        try (FileWriter writer = new FileWriter(jniDirPath + "/Application.mk")) {
            writer.write(applicationMk);
        }
    }

    private static void compileWithNdkBuild(String ndkPath) throws IOException, InterruptedException {
        String projectDir = System.getProperty("user.dir");
        String command = ndkPath + "/ndk-build NDK_PROJECT_PATH=" + projectDir + "/src/main" +
                         " NDK_OUT=" + projectDir + "/src/main/obj" +
                         " NDK_LIBS_OUT=" + projectDir + "/src/main/jniLibs";

        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        // Capture and log output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
        }

        if (exitCode != 0) {
            throw new RuntimeException("Failed to compile native code using ndk-build");
        }
    }


    private static void copyJniLibraries(String jniLibsPath, String decompiledApkPath) throws IOException {
        File jniLibsDir = new File(jniLibsPath);
        File decompiledDir = new File(decompiledApkPath);
        
        if (!jniLibsDir.exists() || !jniLibsDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid JNI libs directory: " + jniLibsPath);
        }
        
        if (!decompiledDir.exists() || !decompiledDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid decompiled APK directory: " + decompiledApkPath);
        }
        
        // Iterate through each ABI directory (e.g., armeabi-v7a, x86, ...)
        for (File abiDir : jniLibsDir.listFiles()) {
            if (abiDir.isDirectory()) {
                File targetAbiDir = new File(decompiledApkPath, "lib/" + abiDir.getName());
                targetAbiDir.mkdirs(); // Create target ABI directory if not exists
                
                // Copy .so files from src/main/jniLibs/<ABI> to decompiled_apk/lib/<ABI>
                for (File libFile : abiDir.listFiles()) {
                    if (libFile.isFile() && libFile.getName().endsWith(".so")) {
                        copyFile(libFile, new File(targetAbiDir, libFile.getName()));
                    }
                }
            }
        }
    }

    private static void copyFile(File source, File destination) throws IOException {
        try (InputStream inputStream = new FileInputStream(source);
             OutputStream outputStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

}
import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {

	public static void encryptStrings(String dir, String bundleId, byte[] secretKey, String customAppClass, List<String> files) throws Exception {
	    File smaliDir = new File(dir);
	    for (File file : Objects.requireNonNull(smaliDir.listFiles())) {
	        if (file.isDirectory()) {
	            encryptStrings(file.getAbsolutePath(), bundleId, secretKey, customAppClass, files);
	        } else {
	            String relativePath = file.getAbsolutePath().replace(File.separator, "/");
	            if (relativePath.endsWith(".smali")) {
	                relativePath = relativePath.substring(0, relativePath.length() - ".smali".length());
	            }
	            
	            boolean isTargetFile = false;
	            for (String targetFilePath : files) {
	                if (relativePath.endsWith(targetFilePath)) {
	                	System.out.println(relativePath);
	                    isTargetFile = true;
	                    break;
	                }
	            }
	            if (isTargetFile) {
	                System.out.println("Processing file: " + file.getAbsolutePath());
	                List<String> lines = new ArrayList<>();
	                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
	                    String line;
	                    while ((line = reader.readLine()) != null) {
	                        line = encryptStringsInLine(line, bundleId, secretKey, customAppClass);
	                        lines.add(line);
	                    }
	                }
	                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
	                    for (String line : lines) {
	                        writer.write(line);
	                        writer.newLine();
	                    }
	                }
	            }
	        }
    	}
	}

    private static String encryptStringsInLine(String line, String bundleId,byte[] secretKey, String customAppClass) throws Exception {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        if (!line.trim().startsWith("const-string")) {
            return line;
        }
        while (true) {
            int pos = line.indexOf('"', start);
            if (pos == -1) break;
            int end = line.indexOf('"', pos + 1);
            if (end == -1) break;

            String original = line.substring(pos + 1, end);

            String encrypted = encrypt(original,secretKey);
            String register = extractRegister(line);
            String decryptionCall = "\n\tinvoke-static {"+register+"}, L"+bundleId.replace(".","/")+"/"+customAppClass+";->decryptString(Ljava/lang/String;)Ljava/lang/String;\n" +
                                    "\tmove-result-object " + register ;

            // Replace the original string with the encrypted string
            sb.append(line, start, pos + 1).append(encrypted).append(line, end, end + 1).append(decryptionCall);

            start = end + 1;
        }
        sb.append(line.substring(start));
        return sb.toString();
    }

    private static String extractRegister(String line) {
        String regex = "\\s*const-string\\s+([vp]\\d+),\\s*\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            return matcher.group(1); // Group 1 is the register (e.g., p1, v0)
        } else {
            return "";
        }
    }

    private static String encrypt(String strToEncrypt,byte[] secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        final SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes()));
    }
	
}
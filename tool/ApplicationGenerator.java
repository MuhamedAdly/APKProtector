import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class ApplicationGenerator {

	public static void writeApplicationSmaliFile(String decodedDir, String bundleId, String customAppClass) throws IOException {
        File smaliFile = new File(new File(decodedDir, "smali/" + bundleId.replace('.', '/')), customAppClass + ".smali");
        smaliFile.getParentFile().mkdirs();
        String smaliCode = 
    ".class public L"+bundleId.replace('.', '/')+"/"+ customAppClass +";\n" +
    ".super Landroid/app/Application;\n" +
    ".source \"application.java\"\n" +
    "\n" +
    "# direct methods\n" +
    ".method static constructor <clinit>()V\n" +
    "    .registers 1\n" +
    "    const-string v0, \"encryptor-lib\"\n" +
    "    invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V\n" +
    "    return-void\n" +
    ".end method\n" +
    "\n" +
    ".method public constructor <init>()V\n" +
    "    .registers 1\n" +
    "\n" +
    "    .line 10\n" +
    "    invoke-direct {p0}, Landroid/app/Application;-><init>()V\n" +
    "\n" +
    "    return-void\n" +
    ".end method\n" +
    "\n" +
    ".method public static decryptString(Ljava/lang/String;)Ljava/lang/String;\n" +
    "    .registers 4\n" +
    "\n" +
    "    const-string v0, \"AES\"\n" +
    "\n" +
    "    .line 25\n" +
    "    invoke-static {}, L"+bundleId.replace('.', '/')+"/"+ customAppClass +";->getKey()[B\n" +
    "\n" +
    "    move-result-object v1\n" +
    "\n" +
    "    if-eqz v1, :cond_35\n" +
    "\n" +
    "    .line 26\n" +
    "    array-length v2, v1\n" +
    "\n" +
    "    if-eqz v2, :cond_35\n" +
    "\n" +
    "    .line 30\n" +
    "    :try_start_b\n" +
    "    new-instance v2, Ljavax/crypto/spec/SecretKeySpec;\n" +
    "\n" +
    "    invoke-direct {v2, v1, v0}, Ljavax/crypto/spec/SecretKeySpec;-><init>([BLjava/lang/String;)V\n" +
    "\n" +
    "    .line 31\n" +
    "    invoke-static {v0}, Ljavax/crypto/Cipher;->getInstance(Ljava/lang/String;)Ljavax/crypto/Cipher;\n" +
    "\n" +
    "    move-result-object v0\n" +
    "\n" +
    "    const/4 v1, 0x2\n" +
    "\n" +
    "    .line 32\n" +
    "    invoke-virtual {v0, v1, v2}, Ljavax/crypto/Cipher;->init(ILjava/security/Key;)V\n" +
    "\n" +
    "    .line 33\n" +
    "    new-instance v1, Ljava/lang/String;\n" +
    "\n" +
    "    const/4 v2, 0x0\n" +
    "\n" +
    "    invoke-static {p0, v2}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B\n" +
    "\n" +
    "    move-result-object p0\n" +
    "\n" +
    "    invoke-virtual {v0, p0}, Ljavax/crypto/Cipher;->doFinal([B)[B\n" +
    "\n" +
    "    move-result-object p0\n" +
    "\n" +
    "    const-string v0, \"UTF-8\"\n" +
    "\n" +
    "    invoke-direct {v1, p0, v0}, Ljava/lang/String;-><init>([BLjava/lang/String;)V\n" +
    "    :try_end_28\n" +
    "    .catch Ljava/lang/Exception; {:try_start_b .. :try_end_28} :catch_29\n" +
    "\n" +
    "    return-object v1\n" +
    "\n" +
    "    :catch_29\n" +
    "    move-exception p0\n" +
    "\n" +
    "    .line 35\n" +
    "    invoke-virtual {p0}, Ljava/lang/Exception;->printStackTrace()V\n" +
    "\n" +
    "    .line 36\n" +
    "    new-instance v0, Ljava/lang/RuntimeException;\n" +
    "\n" +
    "    const-string v1, \"Decryption failed\"\n" +
    "\n" +
    "    invoke-direct {v0, v1, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;Ljava/lang/Throwable;)V\n" +
    "\n" +
    "    throw v0\n" +
    "\n" +
    "    .line 27\n" +
    "    :cond_35\n" +
    "    new-instance p0, Ljava/lang/RuntimeException;\n" +
    "\n" +
    "    const-string v0, \"Secret key is not available\"\n" +
    "\n" +
    "    invoke-direct {p0, v0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V\n" +
    "\n" +
    "    throw p0\n" +
    ".end method\n" +
    "\n" +
    ".method public static native getKey()[B\n" +
    ".end method\n";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(smaliFile))) {
            writer.write(smaliCode);
        }
        
    }

	public static void updateAndroidManifest(String path, String bundleId, String customAppClass) {
        try {
            // Construct the path to the AndroidManifest.xml file
            String manifestPath = path + "/AndroidManifest.xml"; // Adjust this path as necessary
            
            // Load the AndroidManifest.xml file
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(manifestPath));
            doc.getDocumentElement().normalize();

            // Get the <application> element
            NodeList applicationNodes = doc.getElementsByTagName("application");
            if (applicationNodes.getLength() > 0) {
                Element applicationElement = (Element) applicationNodes.item(0);

                // Set the android:name attribute to the custom application class
                applicationElement.setAttribute("android:name", bundleId + "." + customAppClass);

                // Add or update the android:extractNativeLibs attribute to true
                applicationElement.setAttribute("android:extractNativeLibs", "true");

                // Save the updated document back to the file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(manifestPath));
                transformer.transform(source, result);

                System.out.println("Updated AndroidManifest.xml successfully.");
            } else {
                System.out.println("No <application> element found in AndroidManifest.xml.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
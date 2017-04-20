import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.activation.MimetypesFileTypeMap;

public class MimeTest {

	public static void main(String[] args) {
		File f = new File("D:\\setup.zip");
	    System.out.println("Mime Type of " + f.getAbsolutePath() + " is " +
	                         new MimetypesFileTypeMap().getContentType(f));
	    
	    try {
			byte[] data = Files.readAllBytes(f.toPath());
			System.out.println("bytearray: ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}

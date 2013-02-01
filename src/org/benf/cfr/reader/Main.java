package org.benf.cfr.reader;

import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.bytestream.BaseByteData;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.getopt.BadParametersException;
import org.benf.cfr.reader.util.getopt.CFRParameters;
import org.benf.cfr.reader.util.getopt.GetOptParser;
import org.benf.cfr.reader.util.output.Dumper;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: lee
 * Date: 15/04/2011
 * Time: 18:15
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    public static byte[] getBytesFromFile(String fileName) throws IOException {
        File file = new File(fileName);
        InputStream is = new FileInputStream(fileName);

        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    public static void main(String[] args) {

        GetOptParser getOptParser = new GetOptParser();

        // Load the file, and pass the raw byteStream to the ClassFile constructor
        try {
            CFRParameters params = getOptParser.parse(args);

            //LoggerFactory.setGlobalLoggingLevel();
            byte[] content = getBytesFromFile(params.getFileName());
            ByteData data = new BaseByteData(content);
            ClassFile c = new ClassFile(data);
            c.analyse(params);
            Dumper d = new Dumper();
            String methname = params.getMethodName();
            if (methname == null) {
                c.Dump(d);
            } else {
                c.dumpMethod(methname, d);
            }
            d.print("");
        } catch (FileNotFoundException e) {
            System.err.println(e.toString());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(1);
        } catch (BadParametersException e) {
            System.err.print(e.toString());
        } catch (ConfusedCFRException e) {
            System.err.println(e.toString());
            for (Object x : e.getStackTrace()) {
                System.err.println(x);
            }
            System.exit(1);
        }


    }
}

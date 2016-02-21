package com.cortex.lambda.phantomjs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.json.JSONObject;

@SuppressWarnings("rawtypes")
public class Lambda implements RequestHandler<LinkedHashMap, String> {

    private String outputException(Exception e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(out);
        e.printStackTrace(p);
        return new String(out.toByteArray());
    }
    
    private String setupPhantomJs() {
    	try {
        	String[] paths = new String[] {
        		"phantomjs", "libssl.so.1.0.0", "libcrypto.so.1.0.0", 
        		"libicui18n.so.48", "libicuuc.so.48", "libicudata.so.48"
        	};
        	
        	for (String path : paths) {
        		Files.copy(
        			Paths.get("/var/task/" + path), Paths.get("/tmp/" + path), 
        			StandardCopyOption.REPLACE_EXISTING
       			);
        		
        		Files.setPosixFilePermissions(Paths.get("/tmp/" + path), PosixFilePermissions.fromString("rwxr-x---"));
        	}
    	}
    	catch (Exception e) {
    		return outputException(e);
    	}
    	
    	System.setProperty("phantomjs.binary.path", "/tmp/phantomjs");
    	
    	return null;
    }
    
    public String handleRequest(LinkedHashMap input, Context context) {
    	String setupPhantomJsEx = setupPhantomJs();
    	if (setupPhantomJsEx != null) {
    		context.getLogger().log(setupPhantomJsEx);
    		return "ERROR";
    	}
    	
        try {
            context.getLogger().log("Parameters: " + input);
            JSONObject json = new JSONObject(input);
            
            String url = json.getString("url");
            String bucket = json.getString("bucket");
            String key = System.currentTimeMillis() + ".png";
            File file = new File("/tmp/" + key);
        
            Long t = System.currentTimeMillis();
            PhantomJSDriver driver = PhantomBrowser.createInstance();
            context.getLogger().log("PhantomJs load: " + (System.currentTimeMillis() - t) + "ms");
            
            t = System.currentTimeMillis();
            driver.get(url);
            context.getLogger().log("Url load: " + (System.currentTimeMillis() - t) + "ms");
            
            TimeUnit.SECONDS.sleep(5);
            
            t = System.currentTimeMillis();
			byte[] bytes = ((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES);
			context.getLogger().log("Screenshot taken in: " + (System.currentTimeMillis() - t) + "ms");
			
			t = System.currentTimeMillis();
			FileUtils.writeByteArrayToFile(file, bytes);
			context.getLogger().log("Copied image bytes to file in : " + (System.currentTimeMillis() - t) + "ms");
			
			t = System.currentTimeMillis();
			
			AmazonS3 s3 = new AmazonS3Client();
			TransferManager tm = new TransferManager(s3);
			PutObjectRequest putObjectRequest = 
	        	new PutObjectRequest(bucket, key,  file)
	        	.withCannedAcl(CannedAccessControlList.PublicRead);
			Upload upload = tm.upload(putObjectRequest);
			upload.waitForCompletion();
			
		    context.getLogger().log("Upload to s3 in :" + (System.currentTimeMillis() - t) + "ms");
		    
		    FileUtils.deleteQuietly(file);
		    
            return "DONE";

        } catch (Exception ex) {
        	context.getLogger().log(outputException(ex));
            return "ERROR";
        }
    }

}
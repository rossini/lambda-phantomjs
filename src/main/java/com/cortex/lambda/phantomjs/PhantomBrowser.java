package com.cortex.lambda.phantomjs;

import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

public class PhantomBrowser {
	
	public static PhantomJSDriver createInstance() {
		String level = "NONE";
		
		DesiredCapabilities caps = new DesiredCapabilities();
		caps.setCapability("takesScreenshot", true);
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[] {
			"--ignore-ssl-errors=yes", "--webdriver-loglevel=" + level
		});
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS, new String[] {
			"--logLevel=" + level,	
		});
		caps.setJavascriptEnabled(true);
		PhantomJSDriver browser = new PhantomJSDriver(caps);	
		
		return browser;
	}
	
}

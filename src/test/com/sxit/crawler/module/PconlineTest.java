package com.sxit.crawler.module;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.commons.SystemConstant;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.mobilephone.pconline.PconlineMobilePhoneCrawlModule;

public class PconlineTest {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		System.setProperty(SystemConstant.APP_HOME_KEY, "E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data");
		System.setProperty(SystemConstant.APP_NAME_KEY, "PconlineMobilePhoneCrawlModule");
		System.setProperty(SystemConstant.MODULE_HOME_KEY, "E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data\\pconline");
		PconlineMobilePhoneCrawlModule pconlineMobilePhoneCrawlModule = new PconlineMobilePhoneCrawlModule();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName("PconlineMobilePhoneCrawlModule");
		crawlConfig.setAppId(41);
		pconlineMobilePhoneCrawlModule.setCrawlConfig(crawlConfig);
		pconlineMobilePhoneCrawlModule.execute();
	}
}

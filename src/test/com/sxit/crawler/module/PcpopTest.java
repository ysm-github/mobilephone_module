package com.sxit.crawler.module;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.commons.SystemConstant;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.mobilephone.pcpop.PcpopMobilePhoneCrawlModule;

public class PcpopTest {
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		System.setProperty(SystemConstant.APP_HOME_KEY, "E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data");
		System.setProperty(SystemConstant.APP_NAME_KEY, "PconlineMobilePhoneCrawlModule");
		System.setProperty(SystemConstant.MODULE_HOME_KEY, "E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data\\pcpop");
		PcpopMobilePhoneCrawlModule pcpopMobilePhoneCrawlModule = new PcpopMobilePhoneCrawlModule();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName("PcpopMobilePhoneCrawlModule");
		crawlConfig.setAppId(41);
		pcpopMobilePhoneCrawlModule.setCrawlConfig(crawlConfig);
		pcpopMobilePhoneCrawlModule.execute();
	}
}

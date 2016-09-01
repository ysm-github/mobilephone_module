package com.sxit.crawler.module;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.commons.SystemConstant;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.mobilephone.zol.ZolMobilePhoneCrawlModule;

public class ZolTest {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		System.setProperty(SystemConstant.APP_HOME_KEY, "E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data");
		System.setProperty(SystemConstant.MODULE_HOME_KEY, "E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data\\zol");
		ZolMobilePhoneCrawlModule zolMobilePhoneCrawlModule = new ZolMobilePhoneCrawlModule();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName("ZolMobilePhoneCrawlModule");
		crawlConfig.setAppId(41);
		zolMobilePhoneCrawlModule.setCrawlConfig(crawlConfig);
		zolMobilePhoneCrawlModule.execute();
	}
}

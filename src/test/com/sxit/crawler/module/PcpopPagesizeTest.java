package com.sxit.crawler.module;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.commons.SystemConstant;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.mobilephone.pcpop.PcpopMobilePhoneCrawlModule;
import com.sxit.crawler.mobilephone.pcpop.PcpopMobilePhoneDetailInfoCrawlerProcess;
import com.sxit.crawler.mobilephone.pcpop.PcpopMobilePhoneListCrawlerProcess;
import com.sxit.crawler.mobilephone.pcpop.PcpopRecordListCrawlerProcess;

public class PcpopPagesizeTest {
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
		PcpopRecordListCrawlerProcess process = new PcpopRecordListCrawlerProcess(pcpopMobilePhoneCrawlModule.USER_AGENT_PROVIDER, pcpopMobilePhoneCrawlModule, crawlConfig);
		Map<String, Object> param = new HashMap<String, Object>();
		process.process(param);
		System.out.println(process.getDataRecordNum());
	}
}

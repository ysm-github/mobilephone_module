package com.sxit.crawler.mobilephone.pconline;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.module.CrawlModule;

public class PconlineMobilePhoneCrawlModule extends CrawlModule{

	private static Logger log = LoggerFactory.getLogger(PconlineMobilePhoneCrawlModule.class);
	
	public static final String BRAND_ID_KEY = "brand.id";
	
	public static final String BRAND_NAME_KEY = "brand.name";
	
	public final static String RECORD_FILE_NAME = "pconline_com_cn_mobileparam.data";
	
	private final static String DEFAULT_JOB_NAME = PconlineMobilePhoneCrawlModule.class.getSimpleName();
	
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	
	public final String domain = "pconline.com.cn";
	
	@Override
	public void execute() {
		//数据表配置
		DatatableConfig datatableConfig = initDatatableConfig("pconline_datatable_config.xml");
		
		//存储url和品牌的对应关系
		Map<String, String> urls =  new ConcurrentHashMap<String, String>();
		
		
		//手机列表采集器，采集结果为手机的ID编号列表，其结果将放入ids集合变量中。
		PconlineMobilePhoneListCrawlerProcess  pconlineMobilePhoneListCrawlerProcess =
				new PconlineMobilePhoneListCrawlerProcess(USER_AGENT_PROVIDER, this, crawlConfig, urls);
		
		//手机详细信息采集，采集的结果直接入库，入库字段依据为datatableConfig中所配置的映射关系
		PconlineMobilePhoneDetailInfoCrawlerProcess pconlineMobilePhoneDetailInfoCrawlerProcess =
				new PconlineMobilePhoneDetailInfoCrawlerProcess(USER_AGENT_PROVIDER, this, crawlConfig, urls, datatableConfig);
		
		Map<String, Object> param = new HashMap<String, Object>();
		long startTime, endTime;
		
		//1、采集手机基本信息
		try {
			log.info("开始采集手机列表信息");
			startTime = System.currentTimeMillis();
			pconlineMobilePhoneListCrawlerProcess.process(param);
			endTime = System.currentTimeMillis();
			log.info("采集类目信息采集完成, 耗时{}， {}个手机需要抓取", (endTime - startTime), urls.size());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		//2、采集手机详细信息
		try {
			log.info("开始采集手机信息");
			startTime = System.currentTimeMillis();
			pconlineMobilePhoneDetailInfoCrawlerProcess.process(param);
			endTime = System.currentTimeMillis();
			log.info("手机信息采集完成, 耗时{}", (endTime - startTime));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}

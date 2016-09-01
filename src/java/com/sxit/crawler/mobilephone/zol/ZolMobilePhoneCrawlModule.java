package com.sxit.crawler.mobilephone.zol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.module.CrawlModule;

public class ZolMobilePhoneCrawlModule extends CrawlModule {
	
	private static Logger log = LoggerFactory.getLogger(ZolMobilePhoneCrawlModule.class);
	public static final String BRAND_ID_KEY = "brand.id";
	public static final String BRAND_NAME_KEY = "brand.name";
	public static final String PRD_ID_KEY = "prd.id";
	public static final String PARENT_APP_TYPE_NAME_KEY = "parent.app.type.name";
	public static final String APPTYPE_ENTITY_KEY = "apptype.entity";
	public final static String RECORD_FILE_NAME = "zol_com_cn_mobileparam.data";
	
	private final static String DEFAULT_JOB_NAME = ZolMobilePhoneCrawlModule.class.getSimpleName();
	
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	
	public final String domain = "zol.com.cn";
	

	private void execCrawler(DatatableConfig datatableConfig, String brandId, String brandName) {
		//保存需要采集的手机ID
		Set<String> ids = new ConcurrentSkipListSet<String>();
		
		//存储ID和品牌的对应关系
		Map<String, String> brandRelIdMap = new ConcurrentHashMap<String, String>();
		
		//手机列表采集器，采集结果为手机的ID编号列表，其结果将放入ids集合变量中。
		MobilePhoneListCrawlerProcess mobilePhoneListCrawlerProcess = new MobilePhoneListCrawlerProcess(USER_AGENT_PROVIDER, this, crawlConfig, ids, brandRelIdMap);
		
		//手机详细信息采集，采集的结果直接入库，入库字段依据为datatableConfig中所配置的映射关系
		MobilePhoneDetailInfoCrawlerProcess mobilePhoneDetailInfoCrawlerProcess = 
				new MobilePhoneDetailInfoCrawlerProcess(USER_AGENT_PROVIDER, this, crawlConfig, ids, datatableConfig, brandRelIdMap);
		
		Map<String, Object> param = new HashMap<String, Object>();
		long startTime, endTime;
		
		//1、采集手机基本信息
		try {
			log.info("开始采集手机列表信息");
			startTime = System.currentTimeMillis();
			param.put(BRAND_ID_KEY, brandId);
			param.put(BRAND_NAME_KEY, brandName);
			mobilePhoneListCrawlerProcess.process(param);
			endTime = System.currentTimeMillis();
			log.info("采集类目信息采集完成, 耗时{}， {}个手机需要抓取", (endTime - startTime), ids.size());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		//2、采集手机详细信息
		try {
			log.info("开始采集手机信息");
			startTime = System.currentTimeMillis();
			mobilePhoneDetailInfoCrawlerProcess.process(param);
			endTime = System.currentTimeMillis();
			log.info("手机信息采集完成, 耗时{}", (endTime - startTime));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void execute() {
		//数据表配置
		DatatableConfig datatableConfig = initDatatableConfig("zol_datatable_config.xml");
		
		log.info("开始装载手机品牌信息....");
		Map<String, String> brandMap = getBrandMap();//需要采集的品牌
		log.info("手机品牌信息装载完成，共有个品牌需要采集{}", brandMap.size());
		if (!CollectionUtils.isEmpty(brandMap)) {
			for (String brandId : brandMap.keySet()) {
				String brandName = brandMap.get(brandId);
				log.info("开始采集{}品牌", brandName);
				if (StringUtils.isNotBlank(brandId)) {
					execCrawler(datatableConfig, brandId, brandName);
				}
			}
		} else {
			log.info("没有任何品牌信息，采集程序直接退出。");
		}
		
		
	}
	public String getDomain() {
		return domain;
	}
	
	
	private Map<String, String> getBrandMap() {
		Map<String, String> brandMap = new HashMap<String, String>();
		try {
			ClassPathResource resource = new ClassPathResource("zol_brand_list.txt");
			List<String> brandStrs = FileUtils.readLines(resource.getFile());
			for (String brandStr : brandStrs) {
				String[] brands = StringUtils.split(brandStr, ",");
				if (null != brands && brands.length == 2) {
					String brandId = brands[0];
					String brandName = brands[1];
					brandMap.put(brandId, brandName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return brandMap;
	}
	
}


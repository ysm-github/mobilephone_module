package com.sxit.crawler.mobilephone.pconline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.core.CrawlJob;
import com.sxit.crawler.core.fetch.DefaultFetchExecutorBuilder;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchExecutorConfig;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.core.result.ResultExecutor;
import com.sxit.crawler.core.result.ResultExecutorBuilder;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;

public class PconlineMobilePhoneListCrawlerProcess extends CrawlProcess{

private Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * #brandId#    手机品牌ID
	 * #pageoffset# 数据起始位置（分页）
	 */
	private final static String URL = "http://product.pconline.com.cn/productadvanceresult.jsp?queryStr=smallType:20937%2520%25E5%2593%2581%25E7%2589%258C:(#brandId#)&pager.offset=#pageoffset#&sort=&type=2&smallTypeId=20937&areaId=4&lsType=0";
	
	private final static int PAGE_SIZE = 25;//页面数据大小
	
	private Map<String, String> urls;//key为url，value为对应的品牌
	
	
	public PconlineMobilePhoneListCrawlerProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig, Map<String, String> urls) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
		this.urls = urls;
	}
	
	private final class MyResultExecutorBuilder implements ResultExecutorBuilder {
		@Override
		public ResultExecutor buildResultExecutor(
				BlockingQueue<FetchEntry> resultQueue, FetchEntry fetchEntry) {
			return new ResultExecutor(resultQueue, fetchEntry) {
				@Override
				public void processResult() {
					if (!verifyRequired()) {
						return;
					}
					
					Document doc = parseHtmlContent(fetchEntry);
					if (null == doc) {
						return;
					}
					
					//提取页面所有的参数连接
					Elements elts = doc.select("a[href~=^http://product.pconline.com.cn/mobile/(.*)_detail.html$");
					if (CollectionUtils.isEmpty(elts))
						return;
					for (Element elt : elts) {
						String href = elt.attr("href");
						if (StringUtils.isNotBlank(href)) {
							urls.put(href, (String)fetchEntry.getParamData(PconlineMobilePhoneCrawlModule.BRAND_NAME_KEY));
						}
					}
//					RecordFileWriter.wrietToTextFile(new File(getCrawlModule().getDataDir(), "urls.txt"), urls, false);
				}
			};
		}
	}
	

	@Override
	public void process(Map<String, Object> param) {
		CrawlConfig config = new CrawlConfig();
		BeanUtils.copyProperties(getCrawlConfig(), config);
		config.setFetchQueueLength(2048);
		config.setResultQueueLength(2048);
		config.setFetchThreadPoolSize(10);
		config.setResultThreadPoolSize(5);
		config.setFetchExecutorBuilder(fetchExecutorBuilder);
		config.setResultExecutorBuilder(resultExecutorBuilder);
		CrawlJob crawlJob = new CrawlJob(config);
		crawlJob.startJob();
		log.info("开始装载手机品牌信息....");
		Map<String, String> brandMap = getBrandMap();
		log.info("手机品牌信息装载完成，共有个品牌需要采集{}", brandMap.size());
		if (!CollectionUtils.isEmpty(brandMap)) {
			for (String brandId : brandMap.keySet()) {
				String brandName = brandMap.get(brandId);
				int dataRows = getDataRows(brandId, brandName);
				log.info("开始采集品牌 ID:{}, Name:{}, 记录数:{}", new Object[]{brandId, brandName, dataRows});
				
				if (dataRows <= 0) 
					continue;
				
				int pageNums = (dataRows/PAGE_SIZE)+1;
				String specPageNums = System.getProperty("page.nums");
				if (StringUtils.isNotBlank(specPageNums)) {
					pageNums = Integer.parseInt(specPageNums);
				}
				/**
				 * #brandId#    手机品牌ID
				 * #pageoffset# 数据起始位置（分页）
				 */
				for (int i=0; i<=pageNums; i++) {
					String url = URL.replaceAll("#brandId#", brandId);
					url = url.replaceAll("#pageoffset#", String.valueOf(i*PAGE_SIZE));
					FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, userAgentProvider);
					fetchEntry.addParamData(PconlineMobilePhoneCrawlModule.BRAND_NAME_KEY, brandName);
					crawlJob.submit(fetchEntry);
				}
			}
		} else {
			log.info("没有任何品牌信息，采集程序直接退出。");
		}
		crawlJob.waitJobExit();
	}
	
	
	private int getDataRows(String brandId, String brandName) {
		PconlineRecordListCrawlerProcess process = new PconlineRecordListCrawlerProcess(userAgentProvider, crawlModule, crawlConfig);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put(PconlineMobilePhoneCrawlModule.BRAND_ID_KEY, brandId);
		param.put(PconlineMobilePhoneCrawlModule.BRAND_NAME_KEY, brandName);
		process.process(param);
		return process.getDataRecordNum();
	}
	
	private Map<String, String> getBrandMap() {
		Map<String, String> brandMap = new HashMap<String, String>();
		try {
			ClassPathResource resource = new ClassPathResource("pconline_brand_list.txt");
			List<String> brandStrs = FileUtils.readLines(resource.getFile());
			for (String brandStr : brandStrs) {
				if (StringUtils.isBlank(brandStr)) 
					continue;
				String[] brands = StringUtils.split(brandStr, ",");
				if (null != brands && brands.length == 2) {
					String brandId = brands[0];
					String brandName = brands[1];
					if (StringUtils.isNotBlank(brandId)) {
						brandId = brandId.trim();
						brandId = StringUtils.deleteWhitespace(brandId);
						brandMap.put(brandId, brandName);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return brandMap;
	}
	

}

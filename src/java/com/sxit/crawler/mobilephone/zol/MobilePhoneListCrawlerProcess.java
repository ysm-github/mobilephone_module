package com.sxit.crawler.mobilephone.zol;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
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
import com.sxit.crawler.utils.RecordFileWriter;

/**
 * 抓取手机列表
 * @author Administrator
 *
 */
public class MobilePhoneListCrawlerProcess extends CrawlProcess{

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final static String URL = "http://detail.zol.com.cn/cell_phone_advSearch/subcate57_1_#brand_id#_1_1_0_#page_no#.html";
	
	private final static int PAGE_SIZE = 30;//页面数据大小
	
	private Set<String> ids;
	
	private final Map<String, String> brandRelIdMap;
	
	public MobilePhoneListCrawlerProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig, Set<String> ids, Map<String, String> brandRelIdMap) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
		this.ids = ids;
		this.brandRelIdMap = brandRelIdMap;
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
					
					//提取列表部分
					Elements elts = doc.select("div.list_box>ul.result_list>li");
					if (CollectionUtils.isEmpty(elts))
						return;
					
					for (Element elt : elts) {
						//提取单个手机里面的ID
						Element inputElt = elt.select("div.check_pic>label.check>input").first();
						String val = inputElt.val();
						if (StringUtils.isNotBlank(val)) {
							val = StringUtils.deleteWhitespace(val.trim());
							brandRelIdMap.put(val, (String)fetchEntry.getParamData(ZolMobilePhoneCrawlModule.BRAND_NAME_KEY));
							ids.add(val);
						}
					}
					
					RecordFileWriter.wrietToTextFile(new File(getCrawlModule().getDataDir(), "ids.txt"), ids, false);
				}
			};
		}
	}
	

	@Override
	public void process(Map<String, Object> param) {
		if (CollectionUtils.isEmpty(param))
			return;
		
		CrawlConfig config = new CrawlConfig();
		BeanUtils.copyProperties(getCrawlConfig(), config);
		config.setFetchQueueLength(100);
		config.setResultQueueLength(200);
		config.setFetchThreadPoolSize(10);
		config.setResultThreadPoolSize(5);
		config.setFetchExecutorBuilder(fetchExecutorBuilder);
		config.setResultExecutorBuilder(resultExecutorBuilder);
		CrawlJob crawlJob = new CrawlJob(config);
		crawlJob.startJob();
		String brandId = (String)param.get(ZolMobilePhoneCrawlModule.BRAND_ID_KEY);
		String brandName = (String)param.get(ZolMobilePhoneCrawlModule.BRAND_NAME_KEY);
		int dataRows = getDataRows(brandId, brandName);
		log.info("开始采集品牌 ID:{}, Name:{}, 记录数:{}", new Object[]{brandId, brandName, dataRows});
		
		if (dataRows <= 0) 
			return;
		
		int pageNums = (dataRows/PAGE_SIZE)+1;
		String specPageNums = System.getProperty("page.nums");
		if (StringUtils.isNotBlank(specPageNums)) {
			pageNums = Integer.parseInt(specPageNums);
		}
		for (int i=1; i<=pageNums; i++) {
			String url = URL.replaceAll("#brand_id#", brandId);
			url = url.replaceAll("#page_no#", String.valueOf(i));
			FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, userAgentProvider);
			fetchEntry.addParamData(ZolMobilePhoneCrawlModule.BRAND_NAME_KEY, brandName);
			crawlJob.submit(fetchEntry);
		}
		crawlJob.waitJobExit();
	}
	
	
	private int getDataRows(String brandId, String brandName) {
		RecordListCrawlerProcess process = new RecordListCrawlerProcess(userAgentProvider, crawlModule, crawlConfig);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put(ZolMobilePhoneCrawlModule.BRAND_ID_KEY, brandId);
		param.put(ZolMobilePhoneCrawlModule.BRAND_NAME_KEY, brandName);
		process.process(param);
		return process.getDataRecordNum();
	}
}

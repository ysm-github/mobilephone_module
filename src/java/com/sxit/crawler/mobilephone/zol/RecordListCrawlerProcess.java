package com.sxit.crawler.mobilephone.zol;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.core.CrawlJob;
import com.sxit.crawler.core.fetch.DefaultFetchExecutorBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchExecutorConfig;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.core.result.ResultExecutor;
import com.sxit.crawler.core.result.ResultExecutorBuilder;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;

/**
 * 记录数抓取,用于抓取手机品牌下的记录数
 * @author Administrator
 *
 */
public class RecordListCrawlerProcess extends CrawlProcess{
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public final static String URL = "http://detail.zol.com.cn/index.php?c=Ajax&a=getsertotal&subcateId=57&paramStr=#paramStr#"; 
	
	/**
	 * 默认记录数
	 */
	private final static int DEFAULT_RECORD_NUM = 3000;
	
	/**
	 * 当前类目所对应的记录数量
	 */
	private int dataRecordNum;
	
	public int getDataRecordNum() {
		return dataRecordNum;
	}
	public RecordListCrawlerProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
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
					String recordStr = fetchEntry.getResult().getPageContent().toString();
					try {
						dataRecordNum = Integer.parseInt(recordStr.trim());
					} catch (Exception e) {
						e.printStackTrace();
						log.warn("记录数获取错误,URL->{}, Error->{}", fetchEntry.getUrl(), e.getMessage());
						dataRecordNum = DEFAULT_RECORD_NUM;
					}
				}
			};
		}
	}
	


	@Override
	public void process(Map<String, Object> param) {
		if (CollectionUtils.isEmpty(param)) 
			return;
		
		String brandId = (String)param.get(ZolMobilePhoneCrawlModule.BRAND_ID_KEY);
		if (StringUtils.isBlank(brandId))
			return;
		String brandName = (String)param.get(ZolMobilePhoneCrawlModule.BRAND_NAME_KEY);
		
		String url = URL.replaceAll("#paramStr#", brandId);
		log.info("获取品牌下的手机总数,品牌名称:{},ID:{},URL:{}", new Object[]{brandName, brandId, url});
		
		CrawlConfig config = new CrawlConfig();
		BeanUtils.copyProperties(getCrawlConfig(), config);
		config.setAllToOne();//将所有的队列，线程池设置为1
		config.setFetchExecutorBuilder(fetchExecutorBuilder);
		config.setResultExecutorBuilder(resultExecutorBuilder);
		
		CrawlJob crawlJob = new CrawlJob(config);
		crawlJob.startJob();
		crawlJob.submitUrl(url, userAgentProvider);
		crawlJob.waitJobExit();
		
	}
}

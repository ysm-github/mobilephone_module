package com.sxit.crawler.mobilephone.pcpop;

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
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchExecutorConfig;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.core.result.ResultExecutor;
import com.sxit.crawler.core.result.ResultExecutorBuilder;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;
import com.sxit.crawler.utils.RecordFileWriter;

public class PcpopMobilePhoneListCrawlerProcess extends CrawlProcess{
	private Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * #pageno#  页面序号
	 */
	private final static String URL = "http://product.pcpop.com/Mobile/00000_#pageno#.html";
	
	
	private Set<String> ids;
	
	public PcpopMobilePhoneListCrawlerProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig, Set<String> ids) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
		this.ids = ids;
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
					Elements elts = doc.select("div.mt8>div.l3>div.bor2>div.product>dl>dt>span");
					if (CollectionUtils.isEmpty(elts))
						return;
					for (Element elt : elts) {
						String id = elt.attr("id");
						if (StringUtils.isNotBlank(id)) {
							ids.add(id);
						}
					}
					RecordFileWriter.wrietToTextFile(new File(getCrawlModule().getDataDir(), "urls.txt"), ids, false);
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
		int pageNums = getPageNums();
		String specPageNums = System.getProperty("page.nums");
		if (StringUtils.isNotBlank(specPageNums)) {
			pageNums = Integer.parseInt(specPageNums);
		}
		/**
		 * #brandId#    手机品牌ID
		 * #pageoffset# 数据起始位置（分页）
		 */
		log.info("页面数量："+pageNums);
		for (int i=1; i<=pageNums; i++) {
			String url = URL.replaceAll("#pageno#", i+"");
			crawlJob.submitUrl(url, userAgentProvider);
		}
		crawlJob.waitJobExit();
	}
	
	
	private int getPageNums() {
		PcpopRecordListCrawlerProcess process = new PcpopRecordListCrawlerProcess(userAgentProvider, crawlModule, crawlConfig);
		Map<String, Object> param = new HashMap<String, Object>();
		process.process(param);
		return process.getDataRecordNum();
	}
	

}

package com.sxit.crawler.mobilephone.pcpop;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.BeanUtils;

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
import com.sxit.crawler.utils.TextUtils;

public class PcpopRecordListCrawlerProcess extends CrawlProcess{

//	private Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * #brandId#手机品牌ID
	 */
	public final static String URL = "http://product.pcpop.com/Mobile/00000_1.html"; 
	
	
	/**
	 * 默认记录数
	 */
	private final static int DEFAULT_RECORD_NUM = 1000;
	
	/**
	 * 当前类目所对应的记录数量
	 */
	private int dataRecordNum;
	
	public int getDataRecordNum() {
		return dataRecordNum;
	}
	public PcpopRecordListCrawlerProcess(UserAgentProvider userAgentProvider,
			CrawlModule crawlModule, CrawlConfig crawlConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		fetchExecutorConfig.setWaitTime(20);
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
					Document doc = Jsoup.parse(recordStr);
					Element elt = doc.select("div.mt8>div.l3>ul.b_list>li.b3").first();
					if (null != elt) {
						String pageHtml = elt.html();
						recordStr = TextUtils.extrValueByRegx("<span>\\s*<b>\\s*(.*)\\s*</b>\\s*/(.*)页\\s*</span>", pageHtml, 2);
					}
					try {
						if (StringUtils.isNotBlank(recordStr)) {
							recordStr = recordStr.trim();
							dataRecordNum = Integer.parseInt(recordStr.trim());
						}
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
		
		CrawlConfig config = new CrawlConfig();
		BeanUtils.copyProperties(getCrawlConfig(), config);
		config.setAllToOne();//将所有的队列，线程池设置为1
		config.setFetchExecutorBuilder(fetchExecutorBuilder);
		config.setResultExecutorBuilder(resultExecutorBuilder);
		
		CrawlJob crawlJob = new CrawlJob(config);
		crawlJob.startJob();
		crawlJob.submitUrl(URL, userAgentProvider);
		crawlJob.waitJobExit();
		
	}
	
	public static void main(String[] args) {
		String pageHtml = "<a class=\"next\" target=\"_self\" href=\"http://product.pcpop.com/Mobile/00000_2.html\"></a><span><b>1</b>/201页</span>";
		String val = TextUtils.extrValueByRegx("<span>\\s*<b>\\s*(.*)\\s*</b>\\s*/(.*)页\\s*</span>", pageHtml, 2);
		System.out.println(val);
	}

}

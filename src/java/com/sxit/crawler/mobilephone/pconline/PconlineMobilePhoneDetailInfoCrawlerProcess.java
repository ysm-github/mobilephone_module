package com.sxit.crawler.mobilephone.pconline;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.CrawlConfig;
import com.sxit.crawler.core.CrawlJob;
import com.sxit.crawler.core.fetch.DefaultFetchExecutorBuilder;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchExecutorConfig;
import com.sxit.crawler.core.fetch.FetchHTTP;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.core.result.ResultExecutor;
import com.sxit.crawler.core.result.ResultExecutorBuilder;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;
import com.sxit.crawler.utils.Md5Utils;
import com.sxit.crawler.utils.RecordFileWriter;

public class PconlineMobilePhoneDetailInfoCrawlerProcess extends CrawlProcess{

	private Logger log = LoggerFactory.getLogger(getClass());
	
	
	private Map<String, String> urls;//需要采集的连接
	
	private DatatableConfig datatableConfig;
	
	private DatatableOperator datatableOperator;
	
	private final JdbcTemplate jdbcTemplate;
	
	public PconlineMobilePhoneDetailInfoCrawlerProcess(
			UserAgentProvider userAgentProvider, CrawlModule crawlModule,
			CrawlConfig crawlConfig, Map<String, String> urls, DatatableConfig datatableConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
		this.urls = urls;
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
		this.datatableConfig = datatableConfig;
		this.jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		this.datatableOperator = new DatatableOperator(this.datatableConfig, jdbcTemplate);
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
					
					Map<String, Object> paramMap = new LinkedHashMap<String, Object>();
					
					
					String prdName = null;
					Elements prdNameElt = doc.select("div.header>div.subheader>div.pro-tit>div.tit>i.name");
					if (null != prdNameElt) {
						prdName = prdNameElt.text();
						prdName = StringUtils.isNotBlank(prdName) ? prdName.trim() : "";
					}
					if (StringUtils.isNotBlank(prdName)) {
						paramMap.put("BASE_PRD_NAME", prdName);
					} else {
						return;
					}
					
					
					/////////////////////////////////////////////////////////////
					//提取详细参数
					Elements elts = doc.select("table#JparamTable>tbody>tr");
					if (CollectionUtils.isEmpty(elts))
						return;
					
					for (Element elt : elts) {
						//提取主要参数中各个参数项目
						Element keyElt = elt.select("th").first();
						Element valElt = elt.select("td").first();
						
						String key = keyElt.text();
						String val = valElt.text();
						if (StringUtils.isNotBlank(key)) {
							key = key.trim();
							paramMap.put(key, val);
						}
					}
					/////////////////////////////////////////////////////////////
					//将数据写入文件
					paramMap.put("BASE_BRAND", fetchEntry.getParamData(PconlineMobilePhoneCrawlModule.BRAND_NAME_KEY));
					paramMap.put("SYS_URL", fetchEntry.getUrl());
					paramMap.put("SYS_URL_MD5", Md5Utils.getMD5(fetchEntry.getUrl()));
					datatableOperator.saveData(paramMap);
//					System.out.println("URL:"+fetchEntry.getUrl());
//					System.out.println("DATA:"+paramMap.toString());
//					System.out.println("-------------------------------------------------");
					RecordFileWriter.writeToFile(new File(getCrawlModule().getDataDir(), PconlineMobilePhoneCrawlModule.RECORD_FILE_NAME), paramMap);
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
		log.info("开始采集详细手机品牌信息....");
		
		if (!CollectionUtils.isEmpty(urls)) {
			for (String url : urls.keySet()) {
				Map<String, Object> row = new HashMap<String, Object>();
				row.put(datatableConfig.getUniqueColumn(), Md5Utils.getMD5(url));
				if (!datatableOperator.existsData(row)) {
					try {
						String brandName = urls.get(url);
						FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, userAgentProvider);
						fetchEntry.addParamData(PconlineMobilePhoneCrawlModule.BRAND_NAME_KEY, brandName);
						crawlJob.submit(fetchEntry);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			log.info("没有任何品牌信息，采集程序直接退出。");
		}
		crawlJob.waitJobExit();
	}

	
	public static void main(String[] args) {
		String url = "http://product.pconline.com.cn/mobile/nokia/129739_detail.html";
		FetchHTTP fetchHTTP = new FetchHTTP();
		FetchEntry entity = FetchEntityBuilder.buildFetchEntry(url);
//		entity.setCharset("gb2312");
		entity = fetchHTTP.process(entity);
		System.out.println(entity.getResult().getPageContent());
		System.out.println(entity.toString());
	}
}

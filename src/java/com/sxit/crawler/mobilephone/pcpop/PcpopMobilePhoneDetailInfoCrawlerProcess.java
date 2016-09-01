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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableConfig;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
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
import com.sxit.crawler.utils.Md5Utils;
import com.sxit.crawler.utils.RecordFileWriter;

public class PcpopMobilePhoneDetailInfoCrawlerProcess extends CrawlProcess{
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final String URL = "http://product.pcpop.com/#id#/1/Detail.html";
	
	private Set<String> ids;//需要采集
	
	private DatatableConfig datatableConfig;
	
	private DatatableOperator datatableOperator;
	
	private final JdbcTemplate jdbcTemplate;
	
	public PcpopMobilePhoneDetailInfoCrawlerProcess(
			UserAgentProvider userAgentProvider, CrawlModule crawlModule,
			CrawlConfig crawlConfig, Set<String> ids, DatatableConfig datatableConfig) {
		super(userAgentProvider, crawlModule, crawlConfig);
		this.ids = ids;
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
					
					Map<String, Object> paramMap = new HashMap<String, Object>();
					Elements elts = null;
					String brandName = null;
					elts = doc.select("div.wz>span>a[title$=手机][href~=http://product.pcpop.com/Mobile/(.*).htm]");
					for (Element elt : elts) {
						brandName = elt.attr("title");
						if (StringUtils.isNotBlank(brandName)) {
							brandName = brandName.trim();
							if (brandName.length() > 2) {
								brandName = brandName.substring(0, brandName.length()-2);
								break;
							}
						}
					}
					
					
					/////////////////////////////////////////////////////////////
					//提取详细参数
					elts = doc.select("div.w9801125>div.l1125>table.tab1125>tbody>tr[trid=0]");
					if (CollectionUtils.isEmpty(elts))
						return;
					
					Element headElt = elts.first();//产品名称
					if (null != headElt) {
						//提取主要参数中各个参数项目
						Element keyElt = headElt.select("th.ri01").first();
						Element valElt = headElt.select("th[name=PRODUCTTDID0]").first();
						String key = keyElt.text();
						String val = valElt.text();
						if (StringUtils.isNotBlank(key)) {
							key = key.trim();
							paramMap.put(key, val);
						}
					}
					
					
					for (int i=1; i<elts.size(); i++) {
						Element elt  = elts.get(i);
						//提取主要参数中各个参数项目
						Element keyElt = elt.select("td.ri01").first();
						Element valElt = elt.select("td[name=PRODUCTTDID0]").first();
						try {
							String key = keyElt.text();
							String val = valElt.text();
							if (StringUtils.isNotBlank(key)) {
								key = key.trim();
								paramMap.put(key, val);
							}
						} catch (Exception e) {
							if (log.isDebugEnabled()) {
								log.warn("提取数据出错:URL:{}.", fetchEntry.getUrl());
								log.error(e.getMessage(), e);
							}
							
						}
					}
					/////////////////////////////////////////////////////////////
					//将数据写入文件
					paramMap.put("BASE_BRAND", brandName);
					paramMap.put("SYS_URL", fetchEntry.getUrl());
					paramMap.put("SYS_URL_MD5", Md5Utils.getMD5(fetchEntry.getUrl()));
					datatableOperator.saveData(paramMap);
//					System.out.println("URL:"+fetchEntry.getUrl());
//					System.out.println("DATA:"+paramMap.toString());
//					System.out.println("-------------------------------------------------");
					RecordFileWriter.writeToFile(new File(getCrawlModule().getDataDir(), PcpopMobilePhoneCrawlModule.RECORD_FILE_NAME), paramMap);
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
		
		if (!CollectionUtils.isEmpty(ids)) {
			for (String id : ids) {
				try {
					String url = URL.replaceAll("#id#", id);
					Map<String, Object> row = new HashMap<String, Object>();
					row.put(datatableConfig.getUniqueColumn(), Md5Utils.getMD5(url));
					if (!datatableOperator.existsData(row)) {
						crawlJob.submitUrl(url, userAgentProvider);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			log.info("没有任何品牌信息，采集程序直接退出。");
		}
		crawlJob.waitJobExit();
	}

	
}

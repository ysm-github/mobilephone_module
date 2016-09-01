package com.sxit.crawler.mobilephone.zol;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchExecutorConfig;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.core.result.ResultExecutor;
import com.sxit.crawler.core.result.ResultExecutorBuilder;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.module.CrawlProcess;
import com.sxit.crawler.utils.Md5Utils;
import com.sxit.crawler.utils.RecordFileWriter;
import com.sxit.crawler.utils.TextUtils;

public class MobilePhoneDetailInfoCrawlerProcess extends CrawlProcess{

	private Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * id_prefix由id的前3位加以构成
	 */
	private final static String URL = "http://detail.zol.com.cn/#id_prefix#/#id#/param.shtml";
	
	
	private Set<String> ids;//需要采集的手机编号
	
	private DatatableConfig datatableConfig;
	
	private DatatableOperator datatableOperator;
	
	private final JdbcTemplate jdbcTemplate;
	
	private final Map<String, String> brandRelIdMap;
	
	public MobilePhoneDetailInfoCrawlerProcess(
			UserAgentProvider userAgentProvider, CrawlModule crawlModule,
			CrawlConfig crawlConfig, Set<String> ids, DatatableConfig datatableConfig, Map<String, String> brandRelIdMap) {
		super(userAgentProvider, crawlModule, crawlConfig);
		this.ids = ids;
		FetchExecutorConfig fetchExecutorConfig = new FetchExecutorConfig();
		this.fetchExecutorBuilder = new DefaultFetchExecutorBuilder(fetchExecutorConfig);
		this.resultExecutorBuilder = new MyResultExecutorBuilder();
		this.datatableConfig = datatableConfig;
		this.jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		this.datatableOperator = new DatatableOperator(this.datatableConfig, jdbcTemplate);
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
					
					Map<String, Object> paramMap = new LinkedHashMap<String, Object>();
					
					/////////////////////////////////////////////////////////////
					String mobilePrdName = null;//手机型号
					Element headElt = doc.select("html>head").first();
					String headHtml = headElt.html();
					mobilePrdName = TextUtils.extrValueByRegx("var\\s*\\S*proName\\s*\\S*=\\s*\\S*\"(.*)\";", headHtml);
					paramMap.put("BASE_PRD_NAME", mobilePrdName);
					
					String price = null;//价格
					Element priceElt = doc.select("div.product_side_mod>ul.param>li>span.price").first();
					if (null != priceElt) {
						price = priceElt.text();
						price = StringUtils.isNotBlank(price) ? price.trim() : "";
						paramMap.put("BASE_PRICE", mobilePrdName);
					}
					
					
					//提取主要参数
					Elements elts = doc.select("div.main_param>dl.param_box>dd#mParam>ul.main_param_list>li");
					if (CollectionUtils.isEmpty(elts))
						return;
					
					for (Element elt : elts) {
						//提取主要参数中各个参数项目
						String text = elt.text();
						int idx = text.indexOf("：");
						String key = null;
						String val = null;
						if (idx >= 0) {
							key = text.substring(0, idx);
							val = text.substring(idx+1, text.length());
						} else {
							key = text;
						}
						paramMap.put(key, val);
					}
					/////////////////////////////////////////////////////////////
					
					
					/////////////////////////////////////////////////////////////
					//提取详细参数
					elts = doc.select("div.param_content>ul.category_param_list>li");
					if (CollectionUtils.isEmpty(elts))
						return;
					
					for (Element elt : elts) {
						//提取主要参数中各个参数项目
						Element keyElt = elt.select("span[id^=newPmName_]").first();
						Element valElt = elt.select("span[id^=newPmVal_]").first();
						
						String key = keyElt.text();
						String val = valElt.text();
						paramMap.put(key, val);
					}
					/////////////////////////////////////////////////////////////
					//将数据写入文件
					paramMap.put("BASE_BRAND", fetchEntry.getParamData(ZolMobilePhoneCrawlModule.BRAND_NAME_KEY));
					paramMap.put("SYS_URL", fetchEntry.getUrl());
					paramMap.put("SYS_URL_MD5", Md5Utils.getMD5(fetchEntry.getUrl()));
					datatableOperator.saveData(paramMap);
					RecordFileWriter.writeToFile(new File(getCrawlModule().getDataDir(), ZolMobilePhoneCrawlModule.RECORD_FILE_NAME), paramMap);
				}
			};
		}
	}
	
	@Override
	public void process(Map<String, Object> param) {
		CrawlConfig config = new CrawlConfig();
		BeanUtils.copyProperties(getCrawlConfig(), config);
		config.setFetchQueueLength(200);
		config.setResultQueueLength(100);
		config.setFetchThreadPoolSize(10);
		config.setResultThreadPoolSize(10);
		config.setFetchExecutorBuilder(fetchExecutorBuilder);
		config.setResultExecutorBuilder(resultExecutorBuilder);
		
		CrawlJob crawlJob = new CrawlJob(config);
		crawlJob.startJob();
		log.info("开始采集详细手机品牌信息....");
		
		if (!CollectionUtils.isEmpty(ids)) {
			for (String id : ids) {
				try {
					String idPrefixStr = StringUtils.substring(id, 0, 3);
					int idPrefix = Integer.parseInt(idPrefixStr)+1;
					String url = URL.replaceAll("#id_prefix#", String.valueOf(idPrefix));
					url = url.replaceAll("#id#", id);
					
					Map<String, Object> row = new HashMap<String, Object>();
					row.put(datatableConfig.getUniqueColumn(), Md5Utils.getMD5(url));
					if (!datatableOperator.existsData(row)) {
						FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, userAgentProvider);
						fetchEntry.addParamData(ZolMobilePhoneCrawlModule.BRAND_NAME_KEY, brandRelIdMap.get(id));
						fetchEntry.addParamData(ZolMobilePhoneCrawlModule.PRD_ID_KEY, id);
						crawlJob.submit(fetchEntry);
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

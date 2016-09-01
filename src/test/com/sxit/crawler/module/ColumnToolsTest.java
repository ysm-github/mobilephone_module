package com.sxit.crawler.module;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.LoggingMXBean;

import org.apache.commons.io.FileUtils;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class ColumnToolsTest {

	public static void main(String[] args) throws Exception{
		File f = new File("E:/workspaces/sxit/crawl/mobilephone_module/data/column.txt");
		List<String> lines = FileUtils.readLines(f);
		Multimap<String, String> columnCommentMap = ArrayListMultimap.create();
//		Set<String> set = new LinkedHashSet<String>();
		Set<String> set = new TreeSet<String>();
		if (!CollectionUtils.isEmpty(lines)) {
			for (String line : lines) {
				String[] vals = line.split(",");
				if (vals != null && vals.length == 2) {
					String comment = vals[0];
					String column = vals[1];
					columnCommentMap.put(column, comment);
					set.add(column);
				}
			}
		}
		System.out.println("=================================================");
		StringBuffer sb1 = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();
		for (String column : set) {
			sb1.append(column).append(" varchar2(1000),\r\n");
			sb2.append("comment on column TBAS_MOBILE_PARAM_ZOL.").append(column).append(" is '").append(columnCommentMap.get(column)).append("'");
			sb2.append(";\r\n");
		}
		System.out.println(sb1);
		System.out.println("=================================================");
		System.err.println(sb2);
	}
}

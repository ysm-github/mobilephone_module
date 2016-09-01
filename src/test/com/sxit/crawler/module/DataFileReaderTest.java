package com.sxit.crawler.module;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sxit.crawler.utils.RecordFileWriter;
import com.sxit.crawler.utils.RecordFileWriter.ObjectConvertCallBack;

public class DataFileReaderTest {

	/**
	 * 读数据
	 * @param args
	 */
	public static void main(String[] args) {
		Set<String> keySet = new LinkedHashSet<String>();
// 		File file = new File("E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data\\zol\\data\\zol_com_cn_mobileparam.data");
		File file = new File("E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data\\pconline\\data\\pconline_com_cn_mobileparam.data");
//		File file = new File("E:\\workspaces\\sxit\\crawl\\mobilephone_module\\data\\pcpop\\data\\pcpop_com_mobileparam.data");
		final List<Map<String, Object>> listMap = new ArrayList<Map<String,Object>>();
		RecordFileWriter.readFromFile(file, new ObjectConvertCallBack() {
			@Override
			public void convertObject(Object obj) {
				Map<String, Object> map = (Map<String, Object>)obj;
				listMap.add(map);
			}
		});
//		System.out.println(listMap.toString());
		System.out.println("=================================");
		int i=0;
		for (Map<String, Object> map : listMap) {
			i++;
			keySet.addAll(map.keySet());
		}
		System.out.println(i);
		System.out.println(keySet);
//		for (String key : keySet) {
//			try {
//				String str = new String(key.getBytes("GB18030"), "UTF-8");
//				System.out.println(str);
//			} catch (UnsupportedEncodingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
}

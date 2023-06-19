package com.dhh.svn.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dhh.bot.lib.Lib;
import com.dhh.bot.lib.MacdUtil;
import com.dhh.bot.pojo.KRecord;

import service.svm_predict;
import service.svm_scale;
import service.svm_train;

public class SvmPolicyTest 
{

	private static int START_NUM = 0;
	private static int K_PERIOD = 3;
	private static final int TRAIN_COUNT = 10000;
	private static final int TEST_COUNT = 1;
	
	private static List<KRecord> k15list = new ArrayList<KRecord>();
	private static List<KRecord> k5list = new ArrayList<KRecord>();
	private static List<KRecord> k3list = new ArrayList<KRecord>();
	private static List<KRecord> k1list = new ArrayList<KRecord>();
	private static List<Double> macdlisto = new ArrayList<Double>();
	private static List<Double> macdlistc = new ArrayList<Double>();
	private static List<Double> macdlisth = new ArrayList<Double>();
	private static List<Double> macdlistl = new ArrayList<Double>();
	
	private static int total = 0;
	private static int right = 0;
	private static double fall = 0;
	private static double rais = 0;
	private static double asserts = 0;
	private static int tradecount = 0;
	
	public static void main(String[] args)
	{
//		SvmPolicyTest.getAttr(null);
		try {
			new SvmPolicyTest().init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void init() throws Exception
	{
		//训练
//		processData("l");
//		train("l");
//		predict("l");
//		
//		processData("h");
//		train("h");
//		predict("h");
//		
//		processData("o");
//		train("o");
//		predict("o");
		List<KRecord> k15list = Lib.getKline("E:\\个人作品\\extdata\\data\\okcoin\\kdata\\kdata_15_tmp.csv");
		List<KRecord> k5list = Lib.getKline("E:\\个人作品\\extdata\\data\\okcoin\\kdata\\kdata_5_tmp.csv");
		List<KRecord> k3list = Lib.getKline("E:\\个人作品\\extdata\\data\\okcoin\\kdata\\kdata_3_tmp.csv");
		List<KRecord> k1list = Lib.getKline("E:\\个人作品\\extdata\\data\\okcoin\\kdata\\kdata_1_tmp.csv");
		List<Double> macd15list = MacdUtil.getMACD(k15list, 9, 26, 12, "c"); 
		List<Double> macd5list = MacdUtil.getMACD(k5list, 9, 26, 12, "c"); 
		List<Double> macd3list = MacdUtil.getMACD(k3list, 9, 26, 12, "c"); 
		List<Double> macd1list = MacdUtil.getMACD(k1list, 9, 26, 12, "c"); 
		
		processData(k1list,k3list,k5list,k15list,macd1list,macd3list,macd5list,macd15list);
	}
	
	private void processData(List<KRecord> k1list, List<KRecord> k3list, List<KRecord> k5list,
			List<KRecord> k15list, List<Double> macd1list, List<Double> macd3list, List<Double> macd5list,
			List<Double> macd15list) throws Exception
	{
		String trainfilepath = "trainfile\\train_c";
		String testfilepath = "testfile\\test_c";
				
		File targetDataFile = new File("trainfile\\svmdata");
		File trainDataFile = new File(trainfilepath);
		File testDataFile = new File(testfilepath);
		
		File f1 = new File("trainfile");
		File f2 = new File("testfile");
		if(!f1.exists())
		{
			f1.mkdirs();
		}
		if(!f2.exists())
		{
			f2.mkdirs();
		}
		
		if(!targetDataFile.exists())
		{
			targetDataFile.createNewFile();
		}
		if(!trainDataFile.exists())
		{
			trainDataFile.createNewFile();
		}
		if(!testDataFile.exists())
		{
			testDataFile.createNewFile();
		}
		parseDataC(k1list,k3list,k5list,k15list,macd1list,macd3list,macd5list,macd15list, targetDataFile);
		//
		Lib.splitData(k3list, targetDataFile, trainDataFile, testDataFile, TRAIN_COUNT, TEST_COUNT, START_NUM);
		//压缩数据
		svm_scale.main(new String[]{"-l", "0", "-u", "1", "-s", "trainfile\\scaling_parameters", "->", trainfilepath+".scale", trainfilepath});
		svm_scale.main(new String[]{"-l", "0", "-u", "1", "-r", "trainfile\\scaling_parameters", "->", testfilepath+".scale", testfilepath});
	}

	private void train(String type) throws Exception
	{
		/********开始拟合数据********/
		String datafilepath = "trainfile\\train_" + type;
		String modelfilepath = "trainfile\\model_" + type;
		
//		String []arg ={"-s", "1", "-c", "512", "-g", "0.00048828125", "-h", "0", datafilepath+".scale", modelfilepath};
		String []arg ={"-s", "1", "-c", "0.03", "-g", "0.2", "-t", "3", "-h", "0", "-m", "300", datafilepath+".scale", modelfilepath};
//		String []arg ={"-s", "1", "-c", "512", "-g", "0.00048828125", "-h", "0", "-m", "300", datafilepath+".scale", modelfilepath};
//		String []arg ={"-s", "1", "-h", "0", "-t", "3", "-h", "0", "-m", "300", datafilepath+".scale", modelfilepath};
		svm_train.main(arg); //调用      
	}
	
	private Map<String, Object> predict(String type) throws Exception 
	{
		String datafilepath = "testfile\\test_" + type;
		String modelfilepath = "trainfile\\model_" + type;
		String outfilepath = "testfile\\out_" + type;
		
		String[] parg={datafilepath+".scale", modelfilepath, outfilepath}; 
		return svm_predict.main(parg);  //调用   
	}

	private void parseDataC(List<KRecord> k1list, List<KRecord> k3list, List<KRecord> k5list,
			List<KRecord> k15list, List<Double> macd1list, List<Double> macd3list, List<Double> macd5list,
			List<Double> macd15list, File targetDataFile) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetDataFile));
		
		try 
		{
			int k1index = 0;
			int k5index = 0;
			int k15index = 0;
			for(int i=30; i<(k3list.size()-1); i++)
			{
				KRecord k3_0 = k3list.get(i+1);
				KRecord k3_1 = k3list.get(i);
				KRecord k3_2 = k3list.get(i-1);
				KRecord k3_3 = k3list.get(i-2);
				KRecord k3_4 = k3list.get(i-3);
				double macd3_1 = macd3list.get(i);
				double macd3_2 = macd3list.get(i-1);
				double macd3_3 = macd3list.get(i-2);
				double macd3_4 = macd3list.get(i-3);
				
				k1index = this.getKIndexByTime(k1list, k3_1.getTimestamp(), k1index);
				k5index = this.getKIndexByTime(k5list, k3_1.getTimestamp(), k5index);
				k15index = this.getKIndexByTime(k15list, k3_1.getTimestamp(), k5index);
				
				KRecord k1_0 = k1list.get(k1index+1);
				KRecord k1_1 = k1list.get(k1index);
				KRecord k1_2 = k1list.get(k1index-1);
				KRecord k1_3 = k1list.get(k1index-2);
				KRecord k1_4 = k1list.get(k1index-3);
				double macd1_1 = macd1list.get(k1index);
				double macd1_2 = macd1list.get(k1index-1);
				double macd1_3 = macd1list.get(k1index-2);
				double macd1_4 = macd1list.get(k1index-3);
				
				KRecord k5_0 = k5list.get(k5index+1);
				KRecord k5_1 = k5list.get(k5index);
				KRecord k5_2 = k5list.get(k5index-1);
				KRecord k5_3 = k5list.get(k5index-2);
				KRecord k5_4 = k5list.get(k5index-3);
				double macd5_1 = macd5list.get(k5index);
				double macd5_2 = macd5list.get(k5index-1);
				double macd5_3 = macd5list.get(k5index-2);
				double macd5_4 = macd5list.get(k5index-3);
				
				KRecord k15_0 = k15list.get(k15index+1);
				KRecord k15_1 = k15list.get(k15index);
				KRecord k15_2 = k15list.get(k15index-1);
				KRecord k15_3 = k15list.get(k15index-2);
				KRecord k15_4 = k15list.get(k15index-3);
				double macd15_1 = macd15list.get(k15index);
				double macd15_2 = macd15list.get(k15index-1);
				double macd15_3 = macd15list.get(k15index-2);
				double macd15_4 = macd15list.get(k15index-3);
				
				String line = String.format("%s 1:%s 2:%s 3:%s 4:%s 5:%s 6:%s 7:%s 8:%s 9:%s 10:%s 11:%s 12:%s 13:%s 14:%s 15:%s 16:%s 17:%s 18:%s 19:%s 20:%s 21:%s 22:%s 23:%s 24:%s 25:%s 26:%s 27:%s 28:%s 29:%s 30:%s 31:%s 32:%s 33:%s 34:%s 35:%s 36:%s 37:%s 38:%s 39:%s 40:%s 41:%s 42:%s 43:%s 44:%s 45:%s 46:%s 47:%s 48:%s 49:%s 50:%s 51:%s 52:%s 53:%s 54:%s 55:%s 56:%s 57:%s 58:%s 59:%s 60:%s 61:%s 62:%s 63:%s 64:%s 65:%s 66:%s 67:%s 68:%s 69:%s 70:%s 71:%s 72:%s 73:%s 74:%s 75:%s 76:%s 77:%s 78:%s 79:%s 80:%s 81:%s\n",
						k3_0.getPlast()>k3_0.getPopen()?1:-1,
/*						macd1_1,macd1_2,macd1_3,macd1_4,
						macd3_1,macd3_2,macd3_3,macd3_4,
						macd5_1,macd5_2,macd5_3,macd5_4,
						macd15_1,macd15_2,macd15_3,macd15_4,
						k1_1.getPopen(),k1_1.getPlast(),k1_1.getPhigh(),k1_1.getPlow(),
						k1_2.getPopen(),k1_2.getPlast(),k1_2.getPhigh(),k1_2.getPlow(),
						k1_3.getPopen(),k1_3.getPlast(),k1_3.getPhigh(),k1_3.getPlow(),
						k1_4.getPopen(),k1_4.getPlast(),k1_4.getPhigh(),k1_4.getPlow(),
						
						k3_1.getPopen(),k3_1.getPlast(),k3_1.getPhigh(),k3_1.getPlow(),
						k3_2.getPopen(),k3_2.getPlast(),k3_2.getPhigh(),k3_2.getPlow(),
						k3_3.getPopen(),k3_3.getPlast(),k3_3.getPhigh(),k3_3.getPlow(),
						k3_4.getPopen(),k3_4.getPlast(),k3_4.getPhigh(),k3_4.getPlow(),
						
						k5_1.getPopen(),k5_1.getPlast(),k5_1.getPhigh(),k5_1.getPlow(),
						k5_2.getPopen(),k5_2.getPlast(),k5_2.getPhigh(),k5_2.getPlow(),
						k5_3.getPopen(),k5_3.getPlast(),k5_3.getPhigh(),k5_3.getPlow(),
						k5_4.getPopen(),k5_4.getPlast(),k5_4.getPhigh(),k5_4.getPlow(),
						
						k15_1.getPopen(),k15_1.getPlast(),k15_1.getPhigh(),k15_1.getPlow(),
						k15_2.getPopen(),k15_2.getPlast(),k15_2.getPhigh(),k15_2.getPlow(),
						k15_3.getPopen(),k15_3.getPlast(),k15_3.getPhigh(),k15_3.getPlow(),
						k15_4.getPopen(),k15_4.getPlast(),k15_4.getPhigh(),k15_4.getPlow(),
						
						k1_0.getPopen(),k3_0.getPopen(),k5_0.getPopen(),k15_0.getPopen(),
						
						k1_1.getPopen()-k1_1.getPlast(),k1_2.getPopen()-k1_2.getPlast(),k1_3.getPopen()-k1_3.getPlast(),k1_4.getPopen()-k1_4.getPlast(),
						k3_1.getPopen()-k3_1.getPlast(),k3_2.getPopen()-k3_2.getPlast(),k3_3.getPopen()-k3_3.getPlast(),k3_4.getPopen()-k3_4.getPlast(),
						k5_1.getPopen()-k5_1.getPlast(),k5_2.getPopen()-k5_2.getPlast(),k5_3.getPopen()-k5_3.getPlast(),k5_4.getPopen()-k5_4.getPlast(),
						k15_1.getPopen()-k15_1.getPlast(),k15_2.getPopen()-k15_2.getPlast(),k15_3.getPopen()-k15_3.getPlast(),k15_4.getPopen()-k15_4.getPlast()*/
								macd1_1,
								macd1_2,
								macd1_3,
								macd1_4,
								macd3_1,
								macd3_2,
								macd3_3,
								macd3_4,
								macd5_1,
								macd5_2,
								macd5_3,
								macd5_4,
								macd15_1,
								macd15_2,
								macd15_3,
								k1_1.getPopen(),
								k1_1.getPlast(),
								k1_1.getPhigh(),
								k1_1.getPlow(),
								k1_2.getPopen(),
								k1_2.getPlast(),
								k1_2.getPhigh(),
								k1_2.getPlow(),
								k1_3.getPopen(),
								k1_3.getPlast(),
								k1_3.getPhigh(),
								k1_3.getPlow(),
								k1_4.getPopen(),
								k1_4.getPlast(),
								k1_4.getPhigh(),
								k1_4.getPlow(),
								k3_1.getPopen(),
								k3_1.getPlast(),
								k3_1.getPhigh(),
								k3_1.getPlow(),
								k3_2.getPopen(),
								k3_2.getPlast(),
								k3_2.getPhigh(),
								k3_2.getPlow(),
								k3_3.getPopen(),
								k3_3.getPlast(),
								k3_3.getPhigh(),
								k3_3.getPlow(),
								k3_4.getPopen(),
								k3_4.getPlast(),
								k3_4.getPhigh(),
								k3_4.getPlow(),
								k5_1.getPlast(),
								k5_1.getPhigh(),
								k5_1.getPlow(),
								k5_2.getPopen(),
								k5_2.getPhigh(),
								k5_3.getPlast(),
								k5_3.getPhigh(),
								k5_3.getPlow(),
								k15_1.getPlast(),
								k15_1.getPhigh(),
								k15_1.getPlow(),
								k15_2.getPopen(),
								k15_2.getPhigh(),
								k15_2.getPlow(),
								k1_0.getPopen(),
								k3_0.getPopen(),
								k5_0.getPopen(),
								k15_0.getPopen(),
								k1_1.getPopen()-k1_1.getPlast(),
								k1_2.getPopen()-k1_2.getPlast(),
								k1_3.getPopen()-k1_3.getPlast(),
								k1_4.getPopen()-k1_4.getPlast(),
								k3_1.getPopen()-k3_1.getPlast(),
								k3_2.getPopen()-k3_2.getPlast(),
								k3_3.getPopen()-k3_3.getPlast(),
								k3_4.getPopen()-k3_4.getPlast(),
								k5_1.getPopen()-k5_1.getPlast(),
								k5_2.getPopen()-k5_2.getPlast(),
								k5_3.getPopen()-k5_3.getPlast(),
								k5_4.getPopen()-k5_4.getPlast(),
								k15_1.getPopen()-k15_1.getPlast(),
								k15_2.getPopen()-k15_2.getPlast(),
								k15_3.getPopen()-k15_3.getPlast(),
								k15_4.getPopen()-k15_4.getPlast()	
						);
				
				writer.write(line);
				writer.flush();
			}
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		finally
		{
			if(writer != null)
			{
				writer.close();
			}
		}
		
	}

	private int getKIndexByTime(List<KRecord> klist, long timestamp, int start) 
	{
		if(klist != null && klist.size()>3)
		{
			for(int i=0; i<klist.size()-1; i++)
			{
				KRecord k = klist.get(i);
				KRecord knext = klist.get(i+1);
				if(k.getTimestamp()<=timestamp && knext.getTimestamp()>timestamp)
				{
					return i;
				}
			}
		}
		return -1;
	}
	

	public static void getAttr(int... args)
	{
		String tmp = "macd1_1,macd1_2,macd1_3,macd1_4,macd3_1,macd3_2,macd3_3,macd3_4,macd5_1,macd5_2,macd5_3,macd5_4,macd15_1,macd15_2,macd15_3,macd15_4,k1_1.getPopen(),k1_1.getPlast(),k1_1.getPhigh(),k1_1.getPlow(),k1_2.getPopen(),k1_2.getPlast(),k1_2.getPhigh(),k1_2.getPlow(),k1_3.getPopen(),k1_3.getPlast(),k1_3.getPhigh(),k1_3.getPlow(),k1_4.getPopen(),k1_4.getPlast(),k1_4.getPhigh(),k1_4.getPlow(),k3_1.getPopen(),k3_1.getPlast(),k3_1.getPhigh(),k3_1.getPlow(),k3_2.getPopen(),k3_2.getPlast(),k3_2.getPhigh(),k3_2.getPlow(),k3_3.getPopen(),k3_3.getPlast(),k3_3.getPhigh(),k3_3.getPlow(),k3_4.getPopen(),k3_4.getPlast(),k3_4.getPhigh(),k3_4.getPlow(),k5_1.getPopen(),k5_1.getPlast(),k5_1.getPhigh(),k5_1.getPlow(),k5_2.getPopen(),k5_2.getPlast(),k5_2.getPhigh(),k5_2.getPlow(),k5_3.getPopen(),k5_3.getPlast(),k5_3.getPhigh(),k5_3.getPlow(),k5_4.getPopen(),k5_4.getPlast(),k5_4.getPhigh(),k5_4.getPlow(),k15_1.getPopen(),k15_1.getPlast(),k15_1.getPhigh(),k15_1.getPlow(),k15_2.getPopen(),k15_2.getPlast(),k15_2.getPhigh(),k15_2.getPlow(),k15_3.getPopen(),k15_3.getPlast(),k15_3.getPhigh(),k15_3.getPlow(),k15_4.getPopen(),k15_4.getPlast(),k15_4.getPhigh(),k15_4.getPlow(),k1_0.getPopen(),k3_0.getPopen(),k5_0.getPopen(),k15_0.getPopen(),k1_1.getPopen()-k1_1.getPlast(),k1_2.getPopen()-k1_2.getPlast(),k1_3.getPopen()-k1_3.getPlast(),k1_4.getPopen()-k1_4.getPlast(),k3_1.getPopen()-k3_1.getPlast(),k3_2.getPopen()-k3_2.getPlast(),k3_3.getPopen()-k3_3.getPlast(),k3_4.getPopen()-k3_4.getPlast(),k5_1.getPopen()-k5_1.getPlast(),k5_2.getPopen()-k5_2.getPlast(),k5_3.getPopen()-k5_3.getPlast(),k5_4.getPopen()-k5_4.getPlast(),k15_1.getPopen()-k15_1.getPlast(),k15_2.getPopen()-k15_2.getPlast(),k15_3.getPopen()-k15_3.getPlast(),k15_4.getPopen()-k15_4.getPlast()";
		String[] tmparr = tmp.split(",");
		
		String tplStr = "%s";
		String tplval = "";
		int j = 1;
		for(int i=0; i<tmparr.length; i++)
		{
			if(args != null && args.length>0)
			{
				boolean f = false;
				for(int arg : args)
				{
					if(i==arg)
					{
						f = true;
						break;
					}
				}
				if(f)
				{
					continue;
				}
			}

			
			tplStr += " " + j + ":%s";
			tplval += ","+tmparr[i];
			j++;
		}
		
		System.out.println(tplStr+"\\n");
		System.out.println(tplval);
	}
	
}

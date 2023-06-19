package com.dhh.svn.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import service.svm_predict;
import service.svm_scale;
import service.svm_train;

import com.dhh.bot.lib.MacdUtil;
import com.dhh.bot.pojo.KRecord;

public class DataProcess4Regress 
{

	private static final int kcount = 1;
	private static int START_NUM = 0;
	private static final int TRAIN_COUNT = 1000;
	private static final int TEST_COUNT = 10;
	private static double totalProfit = 0;
	private static final String DATA_FILE = "E:\\个人作品\\extdata\\data\\okcoin\\kdata\\kdata_3.csv";
	
	private static List<String[]> datalist = new ArrayList<String[]>();
	private static List<KRecord> klist = new ArrayList<KRecord>();
	private static List<Double> macdlist = new ArrayList<Double>();
	
	public static void main(String[] args) throws Exception 
	{
		init();
//		for(int i=0; i<100; i++)
//		{
//			START_NUM += TEST_COUNT;
//			totalProfit = totalProfit + dotest();
//			System.out.println(String.format("当前总利润:[%s]", totalProfit));
//		}
//		START_NUM = 25*20;
		totalProfit = totalProfit + dotest();
		System.out.println(String.format("当前总利润:[%s]", totalProfit));
	}
	
	private static void init() 
	{
		List<String> kstrlist = readDataFile(DATA_FILE);
		for(String str : kstrlist)
		{
			if(str != null)
			{
				String[] strarr = str.split(",");
				datalist.add(strarr);
				
				KRecord k = new KRecord();
				k.setTimestamp(Long.parseLong(strarr[0]));
				k.setPopen(Double.parseDouble(strarr[1]));
				k.setPlast(Double.parseDouble(strarr[2]));
				k.setPhigh(Double.parseDouble(strarr[3]));
				k.setPlow(Double.parseDouble(strarr[4]));
				klist.add(k);
			}
		}
		macdlist = MacdUtil.getMACD(klist, 9, 12, 26);
		System.out.println("初始化完成");
	}

	private static double dotest() throws Exception 
	{
		File targetDataFile = new File("trainfile\\k1data");
		File trainDataFile = new File("trainfile\\train_k1");
		File testDataFile = new File("testfile\\test_k1");
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
		
		//
		parseData(targetDataFile);
		//
		splitData(targetDataFile, trainDataFile, testDataFile, START_NUM, TRAIN_COUNT, TEST_COUNT);
		//压缩数据
		svm_scale.main(new String[]{"-l", "0", "-u", "1", "-s", "trainfile\\scaling_parameters", "->", "trainfile\\train_k1.scale", "trainfile\\train_k1"});
		svm_scale.main(new String[]{"-l", "0", "-u", "1", "-r", "trainfile\\scaling_parameters", "->", "testfile\\test_k1.scale", "testfile\\test_k1"});
		fit("testfile\\out_r.txt");
		
		//
		parseData1(targetDataFile);
		//
		splitData(targetDataFile, trainDataFile, testDataFile, START_NUM, TRAIN_COUNT, TEST_COUNT);
		//压缩数据
		svm_scale.main(new String[]{"-l", "0", "-u", "1", "-s", "trainfile\\scaling_parameters", "->", "trainfile\\train_k1.scale", "trainfile\\train_k1"});
		svm_scale.main(new String[]{"-l", "0", "-u", "1", "-r", "trainfile\\scaling_parameters", "->", "testfile\\test_k1.scale", "testfile\\test_k1"});
		
		fit("testfile\\out_r_l.txt");
		
		List<double[]> lhplist = getLowHightPrice("testfile\\out_r_l.txt", "testfile\\out_r.txt");
  
		int rightC = 0;
		int notbuyC = 0;
		double profit = 0;
		int j = 0;
		for(int i=(TRAIN_COUNT+START_NUM); i<(START_NUM+TRAIN_COUNT+TEST_COUNT); i++)
		{
			KRecord k = klist.get(i+30);
			double macd0 = macdlist.get(i+30);
			double macd1 = macdlist.get(i+30-1);
			double[] lh = lhplist.get(j);
			if(k.getPlow()<lh[0] && k.getPhigh()>lh[1])
			{
				System.out.println(String.format("%s: %s, 预测:[%s], 差价:[%s]", j+2, "right", Arrays.toString(lh), lh[1]-lh[0]));
				rightC += 1;
				profit = profit + (lh[1]-lh[0]);
			}
			else if(k.getPlow()>lh[0] || (macd0<macd1&&Math.abs(macd0)>2&&Math.abs(macd0-macd1)>0.2*Math.abs(macd0)))
			{
				notbuyC += 1;
				System.out.println(String.format("%s: 未买入:最低价:[%s], 预测低价:[%s]", j+2, k.getPlow(), lh[0]));
			}
			else
			{
				if(lh[0]>k.getPopen())
				{
					profit = profit + (k.getPlast()-k.getPopen());	
					System.out.println(String.format("%s: %s, 差价:[%s], macd0:[%s], macd1:[%s], k:[%s], lh:%s", j+2, "wrong", k.getPlast()-k.getPopen(), macd0,macd1, k, Arrays.toString(lh)));
				}
				else
				{
					profit = profit + (k.getPlast()-lh[0]);
					System.out.println(String.format("%s: %s, 差价:[%s], macd0:[%s], macd1:[%s], k:[%s], lh:%s", j+2, "wrong", k.getPlast()-lh[0], macd0,macd1, k, Arrays.toString(lh)));
				}
			}
			j++;
		}
		
		System.out.println(String.format("结果: 胜率:[%s], 利润:[%s]", (rightC*1.0/(TEST_COUNT-notbuyC)),profit));
		return profit;
	}

	private static List<double[]> getLowHightPrice(String lowfile, String highfile) 
	{
		List<double[]> retlist = new ArrayList<double[]>();
		List<String> lowlist = readDataFile(lowfile);
		List<String> highlist = readDataFile(highfile);
		for(int i=0; i<lowlist.size(); i++)
		{
			double[] lh = new double[2];
			lh[0] = Double.parseDouble(lowlist.get(i).trim());
			lh[1] = Double.parseDouble(highlist.get(i).trim());
			retlist.add(lh);
		}
		return retlist;
	}

	private static List<String> readDataFile(String file) 
	{
		BufferedReader reader = null;
		List<String> datalist = new ArrayList<String>();
		try 
		{
			reader = new BufferedReader(new FileReader(file));
			String line = "";
			while((line=reader.readLine()) != null)
			{
				datalist.add(line);
			}
			
		} catch (Exception e) 
		{
		}
		finally
		{
			if(reader != null)
			{
				try {
					reader.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		return datalist;
	}

	private static void fit(String retPath) throws Exception
	{
		/********开始拟合数据********/
		
		String []arg ={"-s", "3", "-c", "64", "-g", "1", "-p", "0.3", "-h", "0", "trainfile\\train_k1.scale", "trainfile\\model_r.txt"};
//		String []arg ={"trainfile\\train_k1", "trainfile\\model_r.txt"};
		 
		String []parg={"testfile\\test_k1.scale", "trainfile\\model_r.txt", retPath}; 
//		String []parg={"trainfile\\train_k1", "trainfile\\model_r.txt", "testfile\\out_r.txt"}; 
		System.out.println("........SVM运行开始..........");
				        //创建一个训练对象 
//		svm_train t = new svm_train(); 
		       //创建一个预测或者分类的对象 
//		svm_predict p= new svm_predict(); 
		
		svm_train.main(arg); //调用      
		svm_predict.main(parg);  //调用   
	}

	private static void splitData(File orgDataFile, File trainDataFile,
			File testDataFile,int start, int trainCount, int testCount) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(orgDataFile));
		BufferedWriter writerTrain = new BufferedWriter(new FileWriter(trainDataFile));
		BufferedWriter writerTest = new BufferedWriter(new FileWriter(testDataFile));
		
		try 
		{
			String line = "";
			int i=0;
			while((line=reader.readLine()) != null)
			{
				if(i < (trainCount+start) && i>=start)
				{
					writerTrain.write(line + "\n");
					writerTrain.flush();
				}
				
				int tmpB = i-(trainCount+start);
				if(tmpB < testCount && tmpB >= 0)
				{
					writerTest.write(line + "\n");
					writerTrain.flush();
				}
				i++;
			}
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		finally
		{
			if(reader != null)
			{
				reader.close();
			}
			if(writerTrain != null)
			{
				writerTrain.close();
			}
			if(writerTest != null)
			{
				writerTest.close();
			}
		}
	}

	private static void parseData(File targetDataFile) throws Exception
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetDataFile));
		
		try 
		{
			for(int i=30; i<(datalist.size()-1); i++)
			{
				double macd = macdlist.get(i);
				String[] d0 = datalist.get(i-1);
				String[] d1 = datalist.get(i);
				String rline = String.format("%s 1:%s 2:%s 3:%s 4:%s 5:%s 6:%s\n", d1[2], d0[1], d0[2], d0[3], d0[4], d0[5], macd);
				
				writer.write(rline);
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
	
	private static void parseData1(File targetDataFile) throws Exception
	{		
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetDataFile));
		try 
		{
			for(int i=30; i<(datalist.size()-1); i++)
			{
				double macd = macdlist.get(i);
				String[] d0 = datalist.get(i-1);
				String[] d1 = datalist.get(i);
				String rline = String.format("%s 1:%s 2:%s 3:%s 4:%s 5:%s 6:%s\n", d1[4], d0[1], d0[2], d0[3], d0[4], d0[5], macd);
				
				writer.write(rline);
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
}

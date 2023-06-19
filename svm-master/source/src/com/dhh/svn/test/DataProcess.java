package com.dhh.svn.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import service.svm_scale;

public class DataProcess 
{

	private static final int kcount = 3;
	
	public static void main(String[] args) throws Exception 
	{
		File orgDataFile = new File("F:\\dhh\\n\\kdata_1.csv");
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
		parseData(orgDataFile, targetDataFile);
		//
		splitData(targetDataFile, trainDataFile, testDataFile, 3200, 1200);
		
		//压缩数据
		svm_scale.main(new String[]{"-l", "0", "-u", "1", "-s", "trainfile\\scaling_parameters", "->", "trainfile\\train_k1.scale", "trainfile\\train_k1"});
		svm_scale.main(new String[]{"-l", "0", "-u", "1", "-r", "trainfile\\scaling_parameters", "->", "testfile\\test_k1.scale", "testfile\\test_k1"});
	}

	private static void splitData(File orgDataFile, File trainDataFile,
			File testDataFile, int trainCount, int testCount) throws Exception
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
				if(i < trainCount)
				{
					writerTrain.write(line + "\n");
					writerTrain.flush();
				}
				
				int tmpB = i-trainCount;
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

	private static void parseData(File orgDataFile, File targetDataFile) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(orgDataFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetDataFile));
		
		try 
		{
			String line = "";
			List<String[]> dataList = new ArrayList<String[]>();
			while((line=reader.readLine()) != null)
			{
				String[] data = line.split(",");
				dataList.add(data);
			}

			for(int i=0; i<(dataList.size()-2*kcount); i++)
			{
				double vol = 0;
				Double[] cparr = new Double[kcount];
				List<String[]> dl = dataList.subList(i, i+kcount);
				for(int j=0; j<dl.size(); j++)
				{
					String[] d = dl.get(j);
					vol += Double.parseDouble(d[5]);
					cparr[j] = Double.parseDouble(d[2]);
				}
				String classic = calClassic(i, cparr, dataList);
				String rline = String.format("%s 1:%s 2:%s 3:%s 4:%s\n", classic, cparr[0], cparr[1], cparr[2], vol);
				
				writer.write(rline);
				writer.flush();
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
			if(writer != null)
			{
				writer.close();
			}
		}
		
	}

	private static String calClassic(int index, Double[] arr, List<String[]> dataList) 
	{
		double next1 = Double.parseDouble(dataList.get(index+kcount)[2]);
		double next2 = Double.parseDouble(dataList.get(index+kcount+1)[2]);
		double next3 = Double.parseDouble(dataList.get(index+kcount+2)[2]);
		double arrLast = arr[arr.length-1];
		double arrLast2 = arr[arr.length-2];
		if((arrLast>arrLast2&&next1>arrLast) || (arrLast>arrLast2 && next2>next1) || (next2>next1 && next3>next2 && next3>next1) || (next2>next1 && next2>arrLast))
		{
			if(next3-arrLast>0.0003*arrLast || next2-arrLast>0.0002*arrLast)
			{
				return "1";
			}
		}
		return "-1";
	}
}

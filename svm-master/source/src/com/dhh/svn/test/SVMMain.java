package com.dhh.svn.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import service.svm_predict;
import service.svm_train;

public class SVMMain 
{
	public static void main(String[] args) throws IOException 
	{  
		String []arg ={"-s", "3", "-c", "64", "-g", "1", "-p", "0.25", "-h", "0", "trainfile\\train_k1.scale", "trainfile\\model_r.txt"};
//		String []arg ={"trainfile\\train_k1", "trainfile\\model_r.txt"};
		 
//		String []parg={"testfile\\test_k1.scale", "trainfile\\model_r.txt", "testfile\\out_r.txt"}; 
		String []parg={"testfile\\test_k1.scale", "trainfile\\model_r.txt"}; 
//		String []parg={"trainfile\\train_k1", "trainfile\\model_r.txt", "testfile\\out_r.txt"}; 
		System.out.println("........SVM运行开始..........");
				        //创建一个训练对象 
//		svm_train t = new svm_train(); 
		       //创建一个预测或者分类的对象 
//		svm_predict p= new svm_predict(); 
		
		svm_train.main(arg); //调用      
		Map<String, Object> ret = svm_predict.main(parg);  //调用
		
		System.out.println(ret.get("datalist").toString());
	} 
}

package Main;

import java.io.FileInputStream;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import liblinear.InvalidInputDataException;
import protein.GoSet;
import protein.proteinCommon;
import protein.proteinSet;

public class learningOfGO {
	
	public static GoSet aGoSet = new GoSet();
	public static String GoDirectory;
	public static String Access2NameDirectory;
	public static String TrainDirectory;
	public static String MeaDirectory;
	public static String blastResult;
	public static String Task;
	public static int ThreadNum;
	public static double liblinearC;
	public static String modelDir;
	public static String predResultFile;
	public static String resultOutPattern;
	public static String resultOutFile;
	public static liblinear.Parameter liblinearParam   = new liblinear.Parameter(liblinear.SolverType.L2R_LR, 1, 1000, 0.05);

	
	public static int FeatureSize = 8000+400+4;
	public static DecimalFormat doubleFormat;
	public static Properties config;
	
	public static double MFOcut = 0.5;
	public static double BPOcut = 0.5;
	public static double CCOcut = 0.5;
	
	public static void ReadConfig() throws IOException
	{
		FileInputStream in = new FileInputStream("../bin/config.in");
		config = new Properties();
		config.load(in);
		doubleFormat = new DecimalFormat( "0.00000"); 
		GoDirectory = config.getProperty("GoDirectory");
		Access2NameDirectory = config.getProperty("Access2NameDirectory");
		TrainDirectory = config.getProperty("TrainDirectory");
		MeaDirectory   = config.getProperty("MeaDirectory");
		blastResult = config.getProperty("blastResult");
		Task = config.getProperty("Task");
		ThreadNum = Integer.parseInt(config.getProperty("ThreadNum"));
		liblinearC = Double.parseDouble(config.getProperty("liblinearC"));
		modelDir = config.getProperty("modelDir");
		predResultFile = config.getProperty("predResultFile");
		resultOutPattern = config.getProperty("resultOutPattern");
		resultOutFile = config.getProperty("resultOutFile");
	}
	public static void main(String[] args) throws IOException, InvalidInputDataException 
	{
		
		learningOfGO.ReadConfig();
		learningOfGO.aGoSet.Load(GoDirectory);
		//����������Ϣ
		System.out.println("TrainSet = " + TrainDirectory);
		System.out.println("MeasureSet = " + MeaDirectory);
		proteinSet.LoadAccess2NameMap(Access2NameDirectory);
		//���뵰����access�����ֵ�ӳ���ϵ
		
		
		proteinSet measure = new proteinSet();   //�����ʲ��Լ�
		measure.AddAnnotation(MeaDirectory + "Ann");    //���뵰���ʱ�ע
		//measure.filterCAFA2Species();
		
		measure.removeGoNotIn(learningOfGO.aGoSet);				//�Ƴ��������GO�����еı�ע
		measure.addGOFather(learningOfGO.aGoSet);					//���ݹ��ܵĴ��ݹ�ϵ���븸�׽ڵ�
		measure.removeAnnotation(8150,3674,5575);			//�Ƴ�MFO��BPO��CCO�������ڵ�
		
		
		proteinSet train =   new proteinSet();
		train.AddAnnotation(TrainDirectory + "Ann");
		//train.filterCAFA2Species();

		train.removeGoNotIn(learningOfGO.aGoSet);
		train.addGOFather(learningOfGO.aGoSet);
		train.removeAnnotation(8150,3674,5575);
		
		System.out.println("Measure size = " + measure.size());
		System.out.println("Train size = " + train.size());
		
		
		
		
		if (Task.equals("NaiveSpecies"))
		{
			ArrayList<String> SpeciesList = new ArrayList<String>(Arrays.asList
					("_ARATH","_BACSU","_DANRE","_DICDI","_DROME","_ECOLI",
					 "_HUMAN","_MOUSE","_PSEAE","_RAT"  ,"_SCHPO","_XENLA","_YEAST"));
			measure.naiveSpeciesBaseline(train,SpeciesList); 
		}
		if (Task.equals("Naive"))
		{
			measure.ScoreNaiveBaseline(train);
			//measure.OutputPredScore("../OutFile/NaiveResult");
		}
		
		if (Task.equals("LiblinearTrain"))
		{
			liblinearParam   = new liblinear.Parameter(liblinear.SolverType.L2R_LR, liblinearC, 1000, 0.05);
			train.setPredList(train);
			train.OutputPredList("predList");
			
			train.loadFastaSequence(TrainDirectory + "Seq");
			train.tranSequence2TriSparseFeature();
			//train.outputLibTrainFile("TrainSparseFeature", 1);
			train.setLiblinearFeatureFromSparseFeature();
			train.libLinearTrain(modelDir,ThreadNum); 
		}
		
		if (Task.equals("LiblinearPred"))
		{
			measure.loadFastaSequence(MeaDirectory + "Seq");
			measure.tranSequence2TriSparseFeature();
			//measure.outputLibTrainFile("MeasureSparseFeature", 1);
			measure.setLiblinearFeatureFromSparseFeature();   
			
			measure.setPredList(train);
			measure.sortPredListBaseFrequency();
			measure.libLinearPredict(modelDir);
			
			//measure.analyPredScore(learning.aGoSet);
			measure.removeLowPred(2000);
			measure.OutputGOPredScore(predResultFile);
		}
		
		if (Task.equals("L2RFileGenerate"))
		{
			proteinSet.updateNaiveIndex(train);
			
			String L2RTrainSetBlastResult = config.getProperty("L2RTrainSetBlastResult");
			String L2RTrainSetDirectory = config.getProperty("L2RTrainSetDirectory");
			String L2RMeasureSetBlastResult = config.getProperty("L2RMeasureSetBlastResult");
			String L2RMeasureSetDirectory = config.getProperty("L2RMeasureSetDirectory");
			
			proteinSet L2RTrainSet = new proteinSet();
			L2RTrainSet.prepareL2RFileGenerate(L2RTrainSetBlastResult , L2RTrainSetDirectory ,train,"L2RTrain");
			
			L2RTrainSet.calL2RcandidateRecall("L2RTrainSet recall");
			
			L2RTrainSet.OutputRanklibTrainFile("RankFile/TrainMFORanklib.txt",'F');
			L2RTrainSet.OutputRanklibTrainFile("RankFile/TrainBPORanklib.txt",'P');
			L2RTrainSet.OutputRanklibTrainFile("RankFile/TrainCCORanklib.txt",'C');	
			
			proteinSet L2RMeasureSet = new proteinSet();
			L2RMeasureSet.prepareL2RFileGenerate(L2RMeasureSetBlastResult , L2RMeasureSetDirectory ,train,"L2RMeasure");
			L2RMeasureSet.calL2RcandidateRecall("L2RMeasureSet recall");
			
			L2RMeasureSet.OutputRanklibMeasureFile("RankFile/MeasureMFORanklib.txt",'F');
			L2RMeasureSet.OutputRanklibMeasureFile("RankFile/MeasureBPORanklib.txt",'P');
			L2RMeasureSet.OutputRanklibMeasureFile("RankFile/MeasureCCORanklib.txt",'C');	
		}
		
		if (Task.equals("L2Revalution"))
		{
			System.out.println("Begin L2R evalution");
			measure.addRankScore("../InFile/RankFile/MeasureMFORanklib.txt","../InFile/RankFile/MFO.score");
			measure.addRankScore("../InFile/RankFile/MeasureBPORanklib.txt","../InFile/RankFile/BPO.score");
			measure.addRankScore("../InFile/RankFile/MeasureCCORanklib.txt","../InFile/RankFile/CCO.score");
			System.out.println("Finish L2R evalution");
		}
		
		if (Task.equals("Blast"))
		{
			measure.addBlastResultBitScore(blastResult);
			measure.blastBaseline(train);
		}
		
		if (Task.equals("BlastKnn"))
		{
			System.out.println("Begin Blast Weight Knn Score");
			measure.addBlastResultBitScore(blastResult);	//����Blast��Ԥ����
			measure.BlastKnnBaseline(train);	//���д�Ȩ�ص�KNNԤ�⡣
			measure.OutputGOPredScore(predResultFile);
		}
		
		if (Task.equals("knn"))
		{
			System.out.println("Begin Knn Score");
			measure.addSimiliar("../InFile/PubMed/Similiar");	//����Blast��Ԥ����
			measure.knn(train);	//���д�Ȩ�ص�KNNԤ�⡣
		}
		
		//measure.addBlastResultSimility("../InFile/blastResult/psiBlastResult.out");
		//measure.readMSAlignResult("../InFile/blastResult/psiBlastResult.outfmt4");
		//measure.outputMSA("error.out");
		
		
		System.out.println(Task);
		//measure.evalutionEveryProtein("everyProteinResult","MFO");
		//�ֱ�����ÿ�����׵�Ԥ��Ч��
		
		//measure.setPredList(train);      	//��ѵ�����л����ҪԤ��ı�ǩ������
											//���ĳ��labelû����ѵ�����г��֣��Ͳ���Ԥ��
											//�Զ�����label���ֵ�Ƶ�ʴӸߵ�������
		
		//measure.addNoScoreLabelRandom();	//���ڲ��Լ���û�д�ֵ�label�Զ�����һ��������ҵ�����ʹ�ֵ�ֵ
		
		measure.evalutionFmaxAndAUPR(resultOutFile,resultOutPattern,"all");
		
		
		measure.evalutionSpecies(MFOcut, BPOcut, CCOcut, resultOutFile,"_HUMAN","_MOUSE","_ARATH","_ECOLI","_RAT","_YEAST","_SCHPO");
		//�ֱ������������ֵ�Ԥ��Ч��
		
		//measure.setPredList(train);
		//measure.evalEveryLabelAUC(Task + "EveryLabelResult");
		//����2�зֱ�����ÿ��label��Ԥ��Ч��
		//����label��ѵ�����е�Ƶ��һ�μ���
		
		//measure.CalTopkRecall(10,50,100);
		   
	}
}

package ProdTestDiff;

import java.util.HashMap;

//class that describes a method and its coverage
public class MethodCoverage {
  private String fileName;
  private String methodName;
  private String className;
  private HashMap<Integer, Integer> lineMap;

  //regular constructor
  public MethodCoverage(String className, String fileName, String methodName){
    this.className = className;
    this.fileName = fileName;
    this.methodName = methodName;
    this.lineMap = new HashMap<>();
  }

  //empty constructor
  public MethodCoverage(){

  }

  //add a line (key=lineNum, val=hits) to the hashmap
  public void addLine(int lineNum, int hits){
    lineMap.put(lineNum, hits);
  }

  //get the name of the file containing the method
  public String getFileName() {
    return fileName;
  }

  //get the name of the method
  public String getMethodName() {
    return methodName;
  }

  //get the name of the class containing the method
  public String getClassName() {
    return className;
  }

  //get the hashmap of lines
  public HashMap<Integer, Integer> getLineMap() {
    return lineMap;
  }
}

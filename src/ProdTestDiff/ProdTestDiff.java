package ProdTestDiff;


import java.io.*;
import java.nio.file.Files;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


//Main driver class. It first parses the two xml files, then generates
//a set of lines to describe based on if the line in production had more
//hits than in test, then uses ProdTestHTMLWriter to generate an html file,
//and also generates a csv file on its own.
public class ProdTestDiff {
  private HashMap<String, MethodCoverage> prodMap;
  private HashMap<String, MethodCoverage> testMap;
  private String sourceDir;
  private String HTMLFileName;
  private String CSVFileName;
  private static String outputDir = "diff_reports";
  public ProdTestDiff(String prodCoverageFile, String testCoverageFile, String buildNum){
    prodMap = generateMethodCoverageMap(prodCoverageFile);
    testMap = generateMethodCoverageMap(testCoverageFile);
    this.sourceDir = buildNum + "/source";
    this.HTMLFileName = outputDir + "/diffProdTest_" + buildNum + ".html";
    this.CSVFileName = outputDir + "/diffProdTest_" + buildNum + ".csv";
  }

  public static void main(String[] args){
    int argsLength = 3;
    if(args.length != argsLength){
      System.err.println("Usage: java -jar" +
        " ProdTestDiff.jar prod_coverage.xml test_coverage.xml" +
        " buildNum");
      System.exit(-1);
    }
    String prodCoverageXMLFileName = args[0];
    String testCoverageXMLFileName = args[1];
    String buildNum = args[2];

    try{
      File f = new File(outputDir);
      if(!f.exists()) {
        f.mkdir();
      }
    } catch (Exception e){
      e.printStackTrace();
    }

    //copies these resource files
    copyPasteFile("styles.css", outputDir);
    copyPasteFile("jquery.tablesorter.js", outputDir);
    copyPasteFile("jquery-3.2.1.min.js", outputDir);
    copyPasteFile("script.js", outputDir);

    ProdTestDiff diff = new ProdTestDiff(prodCoverageXMLFileName, testCoverageXMLFileName, buildNum);
    diff.printHTMLSummary();
    diff.printCSVSummary();
  }


  public void printHTMLSummary(){
    //generate the hashmaps
    //print the results to a html file
    ProdTestHTMLWriter writer = new ProdTestHTMLWriter(HTMLFileName);
    writer.writeHeaderProdTest();

    //iterate over each class:method in production
    for (Map.Entry<String, MethodCoverage> mapEntry : prodMap.entrySet()) {
      String className = mapEntry.getValue().getClassName();
      String fileName = mapEntry.getValue().getFileName();
      String methodName = mapEntry.getValue().getMethodName();
      HashMap<Integer, Integer> prodLineMap = mapEntry.getValue().getLineMap();
      //replace all is for rendering the < symbol
      //get the map from lines to hits for the current method
      TreeSet<Integer> intersectionLineMap = new TreeSet<>();
      //if ProdTestDiff also has the line that production has
      if(testMap.containsKey(mapEntry.getKey())) {
        HashMap<Integer, Integer> testLineMap = testMap.get(mapEntry.getKey()).getLineMap();
        //loops over each line in production
        for(Map.Entry<Integer, Integer> prodLineEntry: prodLineMap.entrySet()){
          Integer prodLineNumber = prodLineEntry.getKey();
          Integer prodLineHits = prodLineEntry.getValue();
          if(testLineMap.containsKey(prodLineNumber)){
            //if there is a hit in production with no hits in test
            if(testLineMap.get(prodLineNumber) == 0 && prodLineHits > 0){
              intersectionLineMap.add(prodLineNumber);
            }
          }
        }

        int lineIndex = 0;

        //print out a red row for each line
        if(!intersectionLineMap.isEmpty()){
          //get array of lines from the file
          String[] sedLines = getFileLines(intersectionLineMap, fileName);
          for(Integer lineNum: intersectionLineMap) {
            //print out each line with hits on a separate row
            writer.writeTableRow("red", className, methodName,
              Integer.toString(lineNum), Integer.toString(prodLineMap.get(lineNum)),
              sedLines[lineIndex]);
            lineIndex++;
          }
        }
      }

    }
    writer.closeHTMLFile();
  }



  public void printCSVSummary(){
    //generate the hashmaps
    //print the results to a csv file
    try {
      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(CSVFileName)));

      //header
      writer.println("Class,Method,Line Num,Hits,Line with more hits in prod");

      //iterate over each class:method in production
      for (Map.Entry<String, MethodCoverage> mapEntry : prodMap.entrySet()) {
        String className = mapEntry.getValue().getClassName();
        String fileName = mapEntry.getValue().getFileName();
        String methodName = mapEntry.getValue().getMethodName();
        HashMap<Integer, Integer> prodLineMap = mapEntry.getValue().getLineMap();
        //replace all is for rendering the < symbol
        //get the map from lines to hits for the current method
        TreeSet<Integer> intersectionLineMap = new TreeSet<>();
        //if ProdTestDiff also has the line that production has
        if (testMap.containsKey(mapEntry.getKey())) {
          HashMap<Integer, Integer> testLineMap = testMap.get(mapEntry.getKey()).getLineMap();
          //loops over each line in production
          for (Map.Entry<Integer, Integer> prodLineEntry : prodLineMap.entrySet()) {
            Integer prodLineNumber = prodLineEntry.getKey();
            Integer prodLineHits = prodLineEntry.getValue();
            if (testLineMap.containsKey(prodLineNumber)) {
              //if there is a hit in production with no hits in test
              if (testLineMap.get(prodLineNumber) == 0 && prodLineHits > 0) {
                intersectionLineMap.add(prodLineNumber);
              }
            }
          }

          int lineIndex = 0;

          //print out a red row for each line
          if (!intersectionLineMap.isEmpty()) {
            //get array of lines from the file
            String[] sedLines = getFileLines(intersectionLineMap, fileName);
            for (Integer lineNum : intersectionLineMap) {
              //print out each line with hits on a separate row
              writer.println(className + "," + methodName + "," +
                Integer.toString(lineNum) + "," + Integer.toString(prodLineMap.get(lineNum)) + "," +
                sedLines[lineIndex]);
              lineIndex++;
            }
          }
        }

      }
      writer.close();
    } catch(IOException e){
      e.printStackTrace();
    }
  }


  //get specific lines from the file
  private String[] getFileLines(Set<Integer> intersectionLineSet, String fileName){
    //use find to get filePath
    //if multiple files of same name, there will be a problem
    File file = new File(fileName);
    String filePath = getShellCmdOutput(new String[]{"find", sourceDir, "-name", file.getName()});
    String[] filePathSplit = filePath.split("\n");
    //multiple files, so check that we have the right one
    if(filePathSplit.length > 1){
      //loop over each path to see that it contains the path given by fileName
      for(String path: filePathSplit){
        if(path.contains(fileName)){
          filePath = path;
        }
      }
    }
    else {
      filePath = filePath.split("\n")[0];
    }
    String[] fileLines = new String[intersectionLineSet.size()];

    //try reading in the file line by line, and if the line we're reading is
    //in the intersection set, add it to the array
    try {
      //keeps track of the index in the array
      int lineIndex = 0;
      BufferedReader br = new BufferedReader(new FileReader(filePath));
      //keeps track of the actual line number
      int lineNum = 1;
      String input;
      while((input = br.readLine()) != null){
        if(intersectionLineSet.contains(lineNum)){
          fileLines[lineIndex] = input;
          lineIndex++;
        }
        lineNum++;
      }
    } catch (IOException e){
      e.printStackTrace();
    }
    return fileLines;
  }

  //parse the cobertura code coverage xml file
  private static HashMap<String, MethodCoverage> generateMethodCoverageMap(String fileName){
    HashMap<String, MethodCoverage> methodHashMap = new LinkedHashMap<>();
    try{
      //ignore the DTD 
      //step is only necessary if program is not connected to internet
      SAXParserFactory saxfac = SAXParserFactory.newInstance();
      saxfac.setValidating(false);
      saxfac.setFeature("http://xml.org/sax/features/validation", false);
      saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      saxfac.setFeature("http://xml.org/sax/features/external-general-entities", false);
      saxfac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      //create a new SAX parser
      SAXParser saxParser = saxfac.newSAXParser();
      //our handler keeps track of lines covered per method
      DefaultHandler handler = new DefaultHandler() {
        MethodCoverage methodCoverage = new MethodCoverage();
        String methodName;
        String className;
        String fileName;

        public void startElement(String uri, String localName,String qName,
                                 Attributes attributes) throws SAXException {

          //if we're starting on a method, get method name and initialize coverage obj
          if(qName.equalsIgnoreCase("method")){
            methodName = attributes.getValue("name");
            methodCoverage = new MethodCoverage(className, fileName, methodName);
          }
          //if starting on a class, get filename and class name
          else if(qName.equalsIgnoreCase("class")){
            className = attributes.getValue("name");
            fileName = attributes.getValue("filename");
          }
          //if we're on a line, get the line and hits
          else if(qName.equalsIgnoreCase("line")){
            methodCoverage.addLine(
                    Integer.parseInt(attributes.getValue("number")),
                    Integer.parseInt(attributes.getValue("hits")));
          }
        }
        public void endElement(String uri, String localName,
                               String qName) throws SAXException {
          //if a method is ending, create the key using the class name and method name
          //the value is the number of lines covered
          if(qName.equalsIgnoreCase("method")){
            methodHashMap.put(className + "::" + methodName, methodCoverage);
            methodName = "";
          }
          //class is ending, clear className and fileName fields
          else if(qName.equalsIgnoreCase("class")){
            className = "";
            fileName = "";
          }
        }


      };
      saxParser.parse(fileName, handler);
    }

    catch(Exception e){
      e.printStackTrace();
    }
    return methodHashMap;
  }

  //execute a shell command and get the output
  private static String getShellCmdOutput(String[] shellCmd) {
    StringBuilder output = new StringBuilder();
    try {
      //call the command
      ProcessBuilder pb = new ProcessBuilder(shellCmd);
      //for speeding up grep
      pb.environment().put("LANG", "C");
      Process p = pb.start();
      p.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      //read the input while there is still more to read
      while((line = reader.readLine()) != null){
        output.append(line);
        output.append("\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return output.toString();
  }


  //copies a file to the output dir
  private static void copyPasteFile(String fileName, String outputDir){
    try {
      File filePathNew = new File(outputDir + "/" + fileName);
      Files.copy(ProdTestDiff.class.getResourceAsStream("/" + fileName),
          filePathNew.toPath(), REPLACE_EXISTING);

    } catch (IOException e) {

      e.printStackTrace();
    }
  }

}

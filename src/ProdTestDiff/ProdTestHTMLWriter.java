package ProdTestDiff;

import java.io.IOException;
import java.io.PrintWriter;

//class to print out the html file
public class ProdTestHTMLWriter {
  private PrintWriter writer;
  private String csvFileName;
  public ProdTestHTMLWriter(String fileName){
    try {
      writer = new PrintWriter(fileName);
      csvFileName = fileName.replace(".html", ".csv");
    }
    catch (IOException e){
      e.printStackTrace();
    }
  }
  //write the html file header
  protected void writeHeaderProdTest(){
    writer.println("<!DOCTYPE html>");
    writer.println("<html lang = \"en\">");
    writer.println("<head>");
    writer.println("  <link rel=\"stylesheet\" href=\"styles.css\">");
    writer.println("  <script src=\"jquery-3.2.1.min.js\"></script>");
    writer.println("  <script src=\"jquery.tablesorter.js\"></script>");
    writer.println("  <script src=\"script.js\"></script>");
    writer.println("</head>");


    writer.println("<body>");

    writer.println("<div>");
    writer.println("<a href=\"" + csvFileName + "\" download=\"" + csvFileName + "\">Download CSV</a>");
    writer.println("</div>");

    writer.println("<table id=\"myTable\">");

    writer.println("  <colgroup>");
    writer.println("    <col style=\"width:35%\">");
    writer.println("    <col style=\"width:30%\">");
    writer.println("    <col style=\"width:5%\">");
    writer.println("    <col style=\"width:5%\">");
    writer.println("    <col style=\"width:25%\">");
    writer.println("  </colgroup>");

    writer.println("  <thead>");
    writer.println("  <tr>");
    writer.println("    <th>Class</th>");
    writer.println("    <th>Method</th>");
    writer.println("    <th class=\"small_header\"> Line Num</th>");
    writer.println("    <th class=\"small_header\"> Hits</th>");
    writer.println("    <th class=\"small_header\"> Line with more hits in prod</th>");
    writer.println("  </tr>");
    writer.println("  </thead>");
    writer.println("<tbody>");
  }


  protected void closeHTMLFile(){
    writer.println("</tbody>");
    writer.println("</table>");
    writer.println("</body>");
    writer.close();
  }

  //write a table row
  protected void writeTableRow(String trClass, String className,
                               String methodName,
                               String lineNum,
                               String lineHits,
                               String line){
    writer.println("  <tr class=\""+ trClass + "\">");
    writer.println("    <td>" + className + "</td>");
    writer.println("    <td>" + methodName + "</td>");
    writer.println("    <td>" + lineNum + "</td>");
    writer.println("    <td>" + lineHits + "</td>");
    writer.println("    <td>" + line + "</td>");
    writer.println("  </tr>");
  }

  protected void print(String str){
    writer.print(str);
  }
  protected void println(String str){
    writer.println(str);
  }
}

import java.text.SimpleDateFormat;
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import groovy.json.*
 
def flowFile = session.get()
if(!flowFile) return
 
def date = new Date()
flowFile = session.write(flowFile, {inputStream, outputStream ->
    try {
            def text = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
            //Removing space from all the attribute names
 
            //First replacing closing tag </ with <#
            String xml1 = text.replaceAll("<\\/", "<#")
            xml1 = xml1.replaceAll("<Rows.*.>", "<Rows>")
            xml1 = xml1.replaceAll("\\/>", "#>")
    
            //Remove all spaces from tags
            String xml2 = xml1.replaceAll("<([^?>\n]*)>", { full, word -> "${full}".replaceAll("\\s","") })
    
            //Remove all backslashes from tags
               String xml3 = xml2.replaceAll("<([^?>\n]*)>", { full, word -> "${full}".replaceAll("\\/","") })
    
            //Restore closing tags by replacing <# with </
               String finalXml = xml3.replaceAll("<#", "</")
            finalXml = finalXml.replaceAll("#>", "/>")
            
            // Parse it
            def parsed = new XmlParser().parseText(finalXml)
 
            Iterator iterator =  parsed.iterator();
            List outerArray = new ArrayList();
            
            while (iterator.hasNext()) {
                Object outerObj = iterator.next();
                Iterator innerIterator = outerObj.iterator();
                HashMap newProp = new HashMap();
                while (innerIterator.hasNext()) {
                    Object innerobj = innerIterator.next();
                    String newKey   = innerobj.name()
                    String newValue = innerobj.text()
                    newProp.put(newKey, newValue)
                }
                outerArray.add(newProp)
                def json = JsonOutput.toJson(outerArray)
                outputStream.write(JsonOutput.toJson(json).getBytes(StandardCharsets.UTF_8))
                outerArray.clear()
            }
        }
        catch(e) {
            log.error("Error during processing of spreadsheet", e)
            //session.transfer(inputStream, REL_FAILURE)
        }
} as StreamCallback);
 
def filename = flowFile.getAttribute('filename').split('\\.')[0] + '_' + new SimpleDateFormat("YYYYMMdd-HHmmss").format(date)+'.json'
 
flowFile = session.putAttribute(flowFile, 'filename', filename) 
session.transfer(flowFile, REL_SUCCESS)
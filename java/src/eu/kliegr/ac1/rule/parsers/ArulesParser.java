/*
 * Monotonicity Exploiting Association Rule Classification (MARC)
 *
 *     Copyright (C)2014-2017 Tomas Kliegr
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.kliegr.ac1.rule.parsers;

import eu.kliegr.ac1.data.AttributeType;
import eu.kliegr.ac1.data.AttributeValue;
import eu.kliegr.ac1.data.AttributeValueType;
import eu.kliegr.ac1.rule.Antecedent;
import eu.kliegr.ac1.rule.AttributeNotFoundException;
import eu.kliegr.ac1.rule.Consequent;
import eu.kliegr.ac1.rule.Data;
import eu.kliegr.ac1.rule.Rule;
import eu.kliegr.ac1.rule.RuleMultiItem;
import eu.kliegr.ac1.rule.RuleQuality;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author tomas
 */
public class ArulesParser {
    private final Pattern pattern = Pattern.compile("([^,]*?)=([^,]*)");
    private final Pattern intervalPattern = Pattern.compile("(\\[|\\()( ?-?(?:Inf|Infinity|\\d+(?:(?:\\.\\d+)?(?:[Ee][-+]?\\d+)?)?)); ?(-?(?:Inf|Infinity|(?:\\d+(?:(?:\\.\\d+)?(?:[Ee][-+]?\\d+)?)?)))(\\]|\\))");
    private final Data data;

    /**
     *
     * @param data
     */
    public ArulesParser(Data data)
    {
        this.data=data;
    }

    /**
     *
     * @param path
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception
     */
    public ArrayList<Rule> parseFileForRules (String path) throws FileNotFoundException, IOException, Exception
    {
        ArrayList<Rule> rules = new ArrayList();
       
        BufferedReader br = new BufferedReader(new FileReader(path));
        //skip first line
        String line = br.readLine();
        while (line!=null)
        {
            line = br.readLine();
            Rule r = parseRule(line);
            if (r!=null) {
                rules.add(r);
            }
        }
        br.close();
        return rules;
    }
    
    /**
     *
     * @param text
     * @return
     * @throws Exception
     */
    public Rule parseRule(String text) throws Exception{        
    
    if(text== null || text.length()==0) {
        return null;
        }
    String[] parts  = text.split("\",");
    if (parts.length<2 | parts.length>3 ) {
        return null;
        }
    //skip ruleid if present
    int offset=0;
    if (parts.length ==3) {
        offset=1;
        }
    int ruleid = Integer.parseInt(parts[0].replace("\"", ""));
    String[] ruleparts = parts[0+offset].replace("\"","").split("\\} => \\{");
    for (int i = 0; i< ruleparts.length; i++)
    {
        ruleparts[i]= ruleparts[i].replace("{", "").replace("}", "").trim();
    }            
            
    ArrayList<RuleMultiItem> antParts = parseRulePart(ruleparts[0]);
    Antecedent ant  = new Antecedent(antParts);    
    ArrayList<RuleMultiItem> conParts = parseRulePart(ruleparts[1]);
    Consequent con  = new Consequent(conParts.get(0));
    
    String[] ruleQualityParts  = parts[1+offset].split(",");
    //convert relative counts to absolute
    float support = Float.parseFloat(ruleQualityParts[0]);
    float confidence = Float.parseFloat(ruleQualityParts[1]);
    RuleQuality qm = new RuleQuality(support,confidence,data.getDataTable().getCurrentTransactionCount());
    Rule r = new Rule(ant, con, qm,  null,ruleid,data); 
    return r;
}
    
    /**
     *
     * @param rule
     * @param confidence
     * @param support
     * @param ruleid
     * @return
     * @throws AttributeNotFoundException
     * @throws InvalidAttributeTypeException
     */
    public Rule parseRule(String rule, float confidence, float support, int ruleid) throws AttributeNotFoundException,InvalidAttributeTypeException {        
    
    String[] ruleparts = rule.replace("\"","").split("\\} => \\{");
    for (int i = 0; i< ruleparts.length; i++)
    {
        ruleparts[i]= ruleparts[i].replace("{", "").replace("}", "").trim();
    }            
            
    ArrayList<RuleMultiItem> antParts = parseRulePart(ruleparts[0]);
    Antecedent ant  = new Antecedent(antParts);    
    ArrayList<RuleMultiItem> conParts = parseRulePart(ruleparts[1]);
    Consequent con  = new Consequent(conParts.get(0));
    
    //convert relative counts to absolute
    RuleQuality qm = new RuleQuality(support,confidence,data.getDataTable().getCurrentTransactionCount());
    Rule r = new Rule(ant, con, qm,  null,ruleid,data); 
    return r;
}
    private ArrayList<RuleMultiItem>  parseRulePart(String part) throws AttributeNotFoundException,InvalidAttributeTypeException {
        ArrayList<RuleMultiItem> out = new ArrayList<>();
        
        String[] attrs = part.split(",");
        
        for(String attr:attrs){
            //the ? in the regex ensures that if there is "=" in the literal it is matched as part of the value            
            Matcher matcher = pattern.matcher(attr);	
            if (matcher.matches()){
                String attname=matcher.group(1);
                String value=matcher.group(2);
                
                Matcher intervalMatcher = intervalPattern.matcher(value);
                ArrayList<AttributeValue> values;
                AttributeType attType = data.getDataTable().getAttribute(attname).getType();
                boolean matchesNumeric = intervalMatcher.matches();
                if (attType == AttributeType.nominal && matchesNumeric){
                    
                    String message = "Passed attribute value looks like interval, but the attribute is nominal!. \n Offending attribute:" + attname +  "\n Rule part:" + part;                    
                    throw new InvalidAttributeTypeException(message);

                }
                      
                if (matchesNumeric)
                {
                    
                    boolean fromInclusive = intervalMatcher.group(1).equals("[");
                    boolean toInclusive = intervalMatcher.group(4).equals("]");
                    float leftMargin = Float.parseFloat(intervalMatcher.group(2).replace("Inf", "Infinity"));
                    float rightMargin= Float.parseFloat(intervalMatcher.group(3).replace("Inf", "Infinity"));
                        
                    values = new ArrayList(data.getValuesInRange(attname,leftMargin, fromInclusive, rightMargin, toInclusive, false));
                }
                else
                {
                    AttributeValue val = data.getValue(attname, value, AttributeValueType.breakpoint);
                    values = new ArrayList();
                    values.add(val);
                    
                }
                RuleMultiItem rmi = data.makeRuleItem(values, attname);
                out.add(rmi);
           }
         }
    return out;
}
    private static final Logger LOG = Logger.getLogger(ArulesParser.class.getName());

}

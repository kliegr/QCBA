/* Monotonicity Exploiting Association Rule Classification (MARC)

    Copyright (C)2014-2017 Tomas Kliegr

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package eu.kliegr.ac1.rule.extend;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */


public class ExtendRuleConfig {
    public final double minImprovement ;
    public final double  minCondImprovement;
    public final double  minConfidence;
    public final  ExtensionStrategyEnum extType;
    private final static Logger LOGGER = Logger.getLogger(ExtendRuleConfig.class.getName());
    public ExtendRuleConfig( double minImprovement, double minCondImprovement, double minConfidence,ExtensionStrategyEnum extType)
    {
        this.minImprovement = minImprovement;
        this.minCondImprovement = minCondImprovement;
        this.minConfidence = minConfidence;
        this.extType = extType;
        
    }
    public ExtendRuleConfig()
    {
        this.minCondImprovement = -0.05;
        this.minImprovement=0;
        this.minConfidence=0.5;
        this.extType = ExtensionStrategyEnum.ConfImprovementAgainstLastConfirmedExtension;
    }
    
    public boolean conditionalAcceptRule(double curRuleconfidence, double lastConfirmedRuleConfidence)
    {
        if (curRuleconfidence - lastConfirmedRuleConfidence  >= minCondImprovement)
        {
            LOGGER.finest("Change in confidence in conditional accept band, trying extensions");
            return true;
        }
        else
        {
            LOGGER.finest("Change in confidence outside conditional accept band, NOT trying extensions");
            return false;
        }
    }
        
    public boolean acceptRule(double curRuleconfidence, double lastConfirmedRuleConfidence, double seedRuleConfidence, double curRuleSupport, double lastConfirmedRuleSupport)
    {
        LOGGER.log(Level.FINE, MessageFormat.format("Extension type: {0}, curRuleconfidence: {1}, lastConfirmedRuleConfidence: {2},  seedRuleConfidence:  {3}, curRuleSupport:  {4}, lastConfirmedRuleSupport: {5}", extType,curRuleconfidence,  lastConfirmedRuleConfidence,  seedRuleConfidence,  curRuleSupport,  lastConfirmedRuleSupport));

        boolean returnVal=false;
        if (curRuleSupport <= lastConfirmedRuleSupport)
        {
            returnVal= false;
        }
        switch (extType)
        {
            case ConfImprovementAgainstLastConfirmedExtension:
                if ((curRuleconfidence - lastConfirmedRuleConfidence) >= minImprovement)
                {                    
                    returnVal= true;                    
                }
                break;
            case ConfImprovementAgainstSeedRule:
                if ((curRuleconfidence - seedRuleConfidence) >= minImprovement)
                {
                    returnVal= true;
                }
                break;
            case MinConf:
                if (curRuleconfidence>=minConfidence)
                {
                    returnVal= true;
                }     
                break;
        }
        if (returnVal)
        {
            LOGGER.finest("Improvement in confidence meeting criteria, not accepting");            
        }
        else{
            LOGGER.finest("Improvement in confidence NOT meeting criteria, not accepting");            
        }        
        return returnVal;
    }
    
}

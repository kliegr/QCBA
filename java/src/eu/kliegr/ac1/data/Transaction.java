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
package eu.kliegr.ac1.data;


import eu.kliegr.ac1.rule.TestRule;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class Transaction implements Comparable {
   private final static Logger LOGGER = Logger.getLogger(Transaction.class.getName()); 
   private TestRule coveringRule;
    int internalTID;
    //list of attribute values associated with this transaction
    private final ArrayList<AttributeValue> attributeValues = new ArrayList();
    private final int firstTID;

    /**
     *
     * @param TID
     * @param firstTID
     */
    protected Transaction(int TID, int firstTID) {
        this.internalTID = TID;
        this.firstTID = firstTID;
    }
    
    /**
     *
     * @param coveringRule
     */

    public void setCoveringRule(TestRule coveringRule)
    {
        this.coveringRule  = coveringRule;
    }
    
    /**
     *
     * @return
     */
    public TestRule getCoveringRule()
    {
        return coveringRule;
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("T: ").append(internalTID).append(":");
        String sep ="";
        for (AttributeValue value : attributeValues)
        {
            sb.append(sep);
            sb.append(value.getAttribute().getName()).append("=").append(value.toString());
            sep =" & ";
        }
        return sb.toString();
    }

    /**
     *
     * @return
     */
    public int getInternalTID()
    {
        return internalTID;
    }

    /**
     *
     * @return
     */
    public int getFirstTID()
    {
        return firstTID;
    }

    /**
     *
     * @return
     */
    public AttributeValue getTarget()
    {
        for (AttributeValue v : attributeValues )
        {
            if (v.getAttribute().isTargetAttribute)
            {
                return v;
            }
        }
        return null;
    }  

    /**
     *
     * @param at
     * @return
     */
    public AttributeValue getValue(Attribute at)
    {
        for (AttributeValue v : attributeValues )
        {
            if (v.getAttribute() == at)
            {
                return v;
            }
        }
        return null;
    }

    /**
     *
     * @return
     */
    public String getExternalTID()
    {
       //String extTID = attributeValues.stream().filter((a)->a.getAttribute().isIDAttribute).findFirst().ifPresent(get().value);
       
       Optional<AttributeValue> extTID = attributeValues.stream().filter((a)->a.getAttribute().isIDAttribute).findFirst();
       if (extTID.isPresent())
       {
           return extTID.get().value;
       }
       else
       {
           // TID is not set
           return "";
       }

                  
    }

    /**
     *
     * @param value
     */
    public void registerAttributeValue(AttributeValue value)
    {
        attributeValues.add(value);
    }

    /**
     *
     */
    protected void deregisterFromAllAttributeValues()
    {
        attributeValues.stream().map((value) -> {
            LOGGER.log(Level.FINEST, "Deregistering trans with ID={0}(internal TID={1}) from values= '{'{2}'}'", new Object[]{getExternalTID(), getInternalTID(), value.toString()});
            return value;
        }).forEach((value) -> {
            value.removeTransaction(this);
        });
    }

    /**
     *
     */
    protected void reregisterWithAllAttributeValues()
    {
        attributeValues.stream().map((value) -> {
            LOGGER.log(Level.FINEST, "Registering trans with ID={0}(internal TID={1}) with values= '{'{2}'}'", new Object[]{getExternalTID(), getInternalTID(), value.toString()});
            return value;
        }).forEach((value) -> {
            value.addTransaction(this,false);
        });
    }


    @Override
    public int compareTo(Object o) {
        Transaction t2 = (Transaction) o;
        return (this.internalTID - t2.internalTID);
    }
    
}

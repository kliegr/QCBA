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
package eu.kliegr.ac1;

import eu.kliegr.ac1.data.AttributeType;
import java.util.ArrayList;

public abstract class BaseConfig {

    /**
     *
     */
    protected String basePath;    

    /**
     *
     */
    protected String csvSeparator;

    /**
     *
     */
    protected ArrayList<AttributeType> attType;

    /**
     *
     */
    protected String targetAttribute = "XClass";    

    /**
     *
     */
    protected  String IDcolumnName;
    private  String outputPathWithoutSuffix;
    private  String outputPath;

    /**
     *
     */
    protected String dataPath;



    /**
     *
     * @return
     */

    public String getCSVSeparator()
    {
        return csvSeparator;
    }
    
    /**
     *
     * @return
     */
    public String getDataPath()
    {
        return dataPath;
        
        
    }

    /**
     *
     * @param types
     * @return
     */

    protected ArrayList<AttributeType> parseAttributeTypes(String[] types) {
        ArrayList<AttributeType> attTypes= new ArrayList();
            for (int i = 0;i< types.length;i++)
            {
                String type;
                int repeats=1;
                int repeatFlagStart = types[i].indexOf('#');
                if (repeatFlagStart >-1)
                {
                    repeats=Integer.parseInt(types[i].substring(repeatFlagStart+1));
                    type = types[i].substring(0,repeatFlagStart);
                }
                else
                {
                    type=types[i];
                }
                for (int j=1;j<=repeats;j++)
                {
                    attTypes.add(AttributeType.valueOf(type));
                }
                
            }
            
        return attTypes;
    }

    /**
     *
     * @return
     */
    public ArrayList<AttributeType> getAttributeType()
    {
        return attType;
    }

    /**
     *
     * @return
     */
    public String getTargetAttribute()
    {
        return targetAttribute;
    }

    /**
     *
     * @return
     */
    public String getIDcolumnName()
    {
        return IDcolumnName;
    }

    /**
     * @return the OutputFileName
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     *
     * @param suffix
     * @return
     */
    public String getOutputPath(String suffix) {
        return outputPathWithoutSuffix +"." + suffix ;
    }

    /**
     *
     * @param outputPath
     */
    protected void setOutputPath(String outputPath) {        
        this.outputPath = outputPath;
        this.outputPathWithoutSuffix = outputPath.substring(0, outputPath.lastIndexOf('.'));
    }
}

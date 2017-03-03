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
package eu.kliegr.ac1.performance;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

public class StopWatches {

    LinkedHashMap<String, StopWatch> stopwatches = new LinkedHashMap();

    @Override
    public String toString() {

        return stopwatches.entrySet().stream().map((s) -> s.getValue().toString()).reduce("\n ***Performance [ms]***", (s, s1) -> s + "\n" + s1);
    }

    /**
     *
     * @param name
     */
    public void startStopWatch(String name) {
        StopWatch s = new StopWatch(name);
        s.start();
        stopwatches.put(name, s);
    }

    /**
     *
     * @param name
     */
    public void stopStopWatch(String name) {
        stopwatches.get(name).stop();
    }

    /**
     *
     * @return
     */
    public HashMap<String, StopWatch> getWatches() {
        return stopwatches;
    }
    private static final Logger LOG = Logger.getLogger(StopWatches.class.getName());

}

/*
 * Copyright (C) 2013 mewin<mewin001@hotmail.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.mewin.wgtf;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.mewin.wgtf.util.SavingValue;
import de.mewin.wgtf.util.ValueSaver;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 *
 * @author mewin<mewin001@hotmail.de>
 */
public class RegionTimeManager
{
    private ValueSaver saver;
    private WorldGuardPlugin wgp;
    
    @SavingValue
    private HashMap<String, HashMap<String, Long>> regionTimes;
    
    public RegionTimeManager(WorldGuardPlugin wgp, File saveFile)
    {
        saver = new ValueSaver(this, saveFile);
        this.wgp = wgp;
        regionTimes = new HashMap<String, HashMap<String, Long>>();
    }
    
    public long getLocalTime(Location loc)
    {
        RegionManager rm = wgp.getRegionManager(loc.getWorld());
        if (rm == null)
        {
            return loc.getWorld().getTime();
        }
        ApplicableRegionSet regions = rm.getApplicableRegions(loc);
        Iterator<ProtectedRegion> itr = regions.iterator();
        Map<ProtectedRegion, Long> regionsToCheck = new HashMap<ProtectedRegion, Long>();
        Set<ProtectedRegion> ignoredRegions = new HashSet<ProtectedRegion>();
        
        while(itr.hasNext()) {
            ProtectedRegion region = itr.next();
            
            if (ignoredRegions.contains(region) 
                    || getRegionTime(loc.getWorld().getName(), region.getId()) == null) {
                continue;
            }
            
            ProtectedRegion parent = region.getParent();
            
            while(parent != null) {
                ignoredRegions.add(parent);

                parent = parent.getParent();
            }

            regionsToCheck.put(region, getRegionTime(loc.getWorld().getName(), region.getId()));
        }
        
        if (regionsToCheck.size() >= 1) {
            Iterator<Map.Entry<ProtectedRegion, Long>> itr2 = regionsToCheck.entrySet().iterator();
            int minPriority = Integer.MIN_VALUE;
            Long returnValue = null;
            
            while(itr2.hasNext()) {
                Map.Entry<ProtectedRegion, Long> entry = itr2.next();
                
                ProtectedRegion region = entry.getKey();
                Long value = entry.getValue();
                
                if (ignoredRegions.contains(region)) {
                    continue;
                }
                
                if (region.getPriority() < minPriority || region.getPriority() == minPriority)
                {
                    continue;
                }
                
                minPriority = region.getPriority();
                returnValue = value;
            }
            
            return returnValue;
        } else {
            ProtectedRegion global = rm.getRegion("__global__");
            if (rm == null || global == null)
            {
                return loc.getWorld().getTime();
            }
            Long value = getRegionTime(loc.getWorld().getName(), "__global__");
            
            if (value != null)
            {
                return value;
            }
            else
            {
                return loc.getWorld().getTime();
            }
        }
    }
    
    public Long getRegionTime(String world, String region)
    {
        if (!regionTimes.containsKey(world))
        {
            return null;
        }
        else if (!regionTimes.get(world).containsKey(region))
        {
            return null;
        }
        else
        {
            return regionTimes.get(world).get(region);
        }
    }
    
    public void updateRegionTimes()
    {
        for (World w : Bukkit.getWorlds())
        {
            RegionManager rm = wgp.getRegionManager(w);
            if (rm != null)
            {
                for (ProtectedRegion region : rm.getRegions().values())
                {
                    Integer fixTime = region.getFlag(WGTimeFlagsPlugin.FIX_TIME_FLAG);
                    Integer deltaTime = region.getFlag(WGTimeFlagsPlugin.DELTA_TIME_FLAG);
                    Integer minTime = region.getFlag(WGTimeFlagsPlugin.MIN_TIME_FLAG);
                    Integer maxTime = region.getFlag(WGTimeFlagsPlugin.MAX_TIME_FLAG);
                    
                    if (fixTime != null)
                    {
                        setRegionTime(w.getName(), region.getId(), fixTime);
                    }
                    else if (deltaTime != null)
                    {
                        setRegionTime(w.getName(), region.getId(), w.getTime() + deltaTime);
                    }
                    else if (minTime != null || maxTime != null)
                    {
                        Long curTime = getRegionTime(w.getName(), region.getId());
                        
                        if (curTime == null)
                        {
                            curTime = w.getTime();
                        }
                        
                        if (minTime == null)
                        {
                            minTime = 0;
                        }

                        if (maxTime == null)
                        {
                            maxTime = 24000;
                        }

                        if ((minTime > maxTime && curTime < minTime && curTime > maxTime)
                                || curTime < minTime
                                || curTime > maxTime)
                        {
                            setRegionTime(w.getName(), region.getId(), minTime);
                        }
                    }
                }
            }
        }
    }
    
    public void setRegionTime(String world, String region, long time)
    {
        if (!regionTimes.containsKey(world))
        {
            regionTimes.put(world, new HashMap<String, Long>());
        }
        
        regionTimes.get(world).put(region, time);
    }
    
    public void resetRegionTime(String world, String region)
    {
        if (regionTimes.containsKey(world))
        {
            regionTimes.get(world).remove(region);
            
            if (regionTimes.get(world).isEmpty())
            {
                regionTimes.remove(world);
            }
        }
    }
}
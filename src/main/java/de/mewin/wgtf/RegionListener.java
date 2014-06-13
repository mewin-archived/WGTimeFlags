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

import com.mewin.WGRegionEvents.events.RegionEnteredEvent;
import com.mewin.WGRegionEvents.events.RegionLeftEvent;
import com.mewin.util.Util;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author mewin<mewin001@hotmail.de>
 */
public class RegionListener implements Listener
{
    private WorldGuardPlugin wgp;
    
    public RegionListener(WorldGuardPlugin wgp)
    {
        this.wgp = wgp;
    }
    
    @EventHandler
    public void onRegionEntered(RegionEnteredEvent e)
    {
        updateFlags(e.getPlayer());
    }
    
    @EventHandler
    public void onRegionLeft(RegionLeftEvent e)
    {
        updateFlags(e.getPlayer());
    }
    
    private void updateFlags(Player player)
    {
        if (!player.hasPermission("wgtimeflags.ignore"))
        {
            Integer minTime = Util.getFlagValue(wgp, player.getLocation(), WGTimeFlagsPlugin.MIN_TIME_FLAG);
            Integer maxTime = Util.getFlagValue(wgp, player.getLocation(), WGTimeFlagsPlugin.MAX_TIME_FLAG);
            Integer fixTime = Util.getFlagValue(wgp, player.getLocation(), WGTimeFlagsPlugin.FIX_TIME_FLAG);
            Integer deltaTime = Util.getFlagValue(wgp, player.getLocation(), WGTimeFlagsPlugin.DELTA_TIME_FLAG);

            if (minTime == null
                    && maxTime == null
                    && fixTime == null
                    && deltaTime == null)
            {
                player.resetPlayerTime();
            }
            else if (fixTime != null)
            {
                player.setPlayerTime(fixTime, false);
            }
            else if (deltaTime != null)
            {
                player.setPlayerTime(deltaTime % 24000, true);
            }
        }
    }
}
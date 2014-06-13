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

import com.mewin.WGCustomFlags.WGCustomFlagsPlugin;
import com.mewin.util.Util;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author mewin<mewin001@hotmail.de>
 */
public class WGTimeFlagsPlugin extends JavaPlugin
{
    public static final IntegerFlag MAX_TIME_FLAG = new IntegerFlag("max-time");
    public static final IntegerFlag MIN_TIME_FLAG = new IntegerFlag("min-time");
    public static final IntegerFlag FIX_TIME_FLAG = new IntegerFlag("fix-time");
    public static final IntegerFlag DELTA_TIME_FLAG = new IntegerFlag("delta-time");

    private WGCustomFlagsPlugin wgcf;
    private WorldGuardPlugin wgp;
    private RegionListener listener;
    
    @Override
    public void onEnable()
    {
        if (!getWGCF())
        {
            getLogger().log(Level.SEVERE, "Could not find WGCustomFlags.");
            getPluginLoader().disablePlugin(this);
            return;
        }
        else
        {
            getLogger().log(Level.INFO, "Hooked into WGCustomFlags.");
        }
        
        if (!getWorldGuard())
        {
            getLogger().log(Level.SEVERE, "Could not find WorldGuard.");
            getPluginLoader().disablePlugin(this);
            return;
        }
        else
        {
            getLogger().log(Level.INFO, "Hooked into WorldGuard.");
        }
        
        wgcf.addCustomFlag(MAX_TIME_FLAG);
        wgcf.addCustomFlag(MIN_TIME_FLAG);
        wgcf.addCustomFlag(FIX_TIME_FLAG);
        wgcf.addCustomFlag(DELTA_TIME_FLAG);
        
        loadConfig();
        
        listener = new RegionListener(wgp);
        getServer().getPluginManager().registerEvents(listener, this);
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable()
        {
            @Override
            public void run()
            {
                checkFlags();
            }
        }, 1L, getConfig().getInt("time-check-interval", 1000) / 20L);
    }
    
    private void loadConfig()
    {
        File confDir = getDataFolder();
        
        if (!confDir.exists())
        {
            if (!confDir.mkdirs())
            {
                getLogger().log(Level.WARNING, "Could not create configuration folder.");
            }
            else
            {
                File confFile = new File(confDir, "config.yml");
                if (!confFile.exists())
                {
                    FileOutputStream out = null;
                    try
                    {
                        if (!confFile.createNewFile())
                        {
                            getLogger().log(Level.WARNING, "Could not create default configuration.");
                        }
                        else
                        {
                            out = new FileOutputStream(confFile);
                            int r;
                            InputStream in = WGTimeFlagsPlugin.class.getResourceAsStream("/config.yml");
                            while ((r = in.read()) > -1)
                            {
                                out.write(r);
                            }
                        }
                    }
                    catch(Exception ex)
                    {
                        getLogger().log(Level.WARNING, "Could not create default configuration: ", ex);
                    }
                    finally
                    {
                        if (out != null)
                        {
                            try
                            {
                                out.close();
                            }
                            catch(Exception ex)
                            {
                                getLogger().log(Level.WARNING, "Could not close config file after writing: ", ex);
                            }
                        }
                    }
                }
                try
                {
                    getConfig().load(confFile);
                }
                catch(Exception ex)
                {
                    getLogger().log(Level.WARNING, "Could not read configuration: ", ex);
                }
            }
        }
    }
    
    private boolean getWGCF()
    {
        Plugin plug = getServer().getPluginManager().getPlugin("WGCustomFlags");
        if (plug == null || !(plug instanceof WGCustomFlagsPlugin))
        {
            return false;
        }
        else
        {
            wgcf = (WGCustomFlagsPlugin) plug;
            return true;
        }
    }
    
    private boolean getWorldGuard()
    {
        Plugin plug = getServer().getPluginManager().getPlugin("WorldGuard");
        if (plug == null || !(plug instanceof WorldGuardPlugin))
        {
            return false;
        }
        else
        {
            wgp = (WorldGuardPlugin) plug;
            return true;
        }
    }
    
    private void checkFlags()
    {
        for (Player player : getServer().getOnlinePlayers())
        {
            if (!player.hasPermission("wgtimeflags.ignore"))
            {
                Integer minTime = Util.getFlagValue(wgp, player.getLocation(), MIN_TIME_FLAG);
                Integer maxTime = Util.getFlagValue(wgp, player.getLocation(), MAX_TIME_FLAG);
                Integer fixTime = Util.getFlagValue(wgp, player.getLocation(), FIX_TIME_FLAG);
                Integer deltaTime = Util.getFlagValue(wgp, player.getLocation(), DELTA_TIME_FLAG);
                
                if (fixTime == null
                        && deltaTime == null)
                {
                    int curTime = (int) (player.getPlayerTime() % 24000);

                    if (minTime != null || maxTime != null)
                    {
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
                            player.setPlayerTime(minTime - player.getWorld().getTime(), true);
                        }
                    }
                }
            }
        }
    }
}
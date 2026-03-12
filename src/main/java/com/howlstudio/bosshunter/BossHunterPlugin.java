package com.howlstudio.bosshunter;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
/** BossHunter — Track boss kills, announce server-wide, first-kill rewards, /bosses leaderboard. */
public final class BossHunterPlugin extends JavaPlugin {
    private BossManager mgr;
    public BossHunterPlugin(JavaPluginInit init){super(init);}
    @Override protected void setup(){
        System.out.println("[BossHunter] Loading...");
        mgr=new BossManager(getDataDirectory());
        CommandManager.get().register(mgr.getBossesCommand());
        CommandManager.get().register(mgr.getBossKillCommand());
        System.out.println("[BossHunter] Ready. "+mgr.getBossCount()+" boss types tracked.");
    }
    @Override protected void shutdown(){if(mgr!=null)mgr.save();System.out.println("[BossHunter] Stopped.");}
}

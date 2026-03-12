package com.howlstudio.bosshunter;
import com.hypixel.hytale.component.Ref; import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.*; import java.util.*;
import java.util.stream.Collectors;
public class BossManager {
    private final Path dataDir;
    private final Map<String,Integer> totalKills=new LinkedHashMap<>(); // bossType→global kills
    private final Map<UUID,Map<String,Integer>> playerKills=new HashMap<>(); // player→boss→kills
    private final Map<String,String> firstKillers=new LinkedHashMap<>(); // bossType→playerName
    private final Set<String> bosses=new LinkedHashSet<>(List.of("ender_dragon","wither","elder_guardian","raid_captain","evoker","ravager","pillager_captain"));
    public BossManager(Path d){this.dataDir=d;try{Files.createDirectories(d);}catch(Exception e){}load();}
    public int getBossCount(){return bosses.size();}
    public void recordKill(UUID uid,String name,String bossType){
        String bt=bossType.toLowerCase();
        totalKills.merge(bt,1,Integer::sum);
        playerKills.computeIfAbsent(uid,k->new HashMap<>()).merge(bt,1,Integer::sum);
        if(!firstKillers.containsKey(bt)){firstKillers.put(bt,name);broadcast("§6[Boss] §e"+name+"§r was the §6FIRST§r to defeat §c"+bt+"§r! Server-wide bonus activated!");}
        else{broadcast("§6[Boss] §e"+name+"§r defeated §c"+bt+"§r! (kill #"+totalKills.get(bt)+")");}
        save();
    }
    public List<Map.Entry<UUID,Integer>> getTopHunters(){
        Map<UUID,Integer> totals=new HashMap<>();
        for(var e:playerKills.entrySet())totals.put(e.getKey(),e.getValue().values().stream().mapToInt(Integer::intValue).sum());
        return totals.entrySet().stream().sorted(Map.Entry.<UUID,Integer>comparingByValue().reversed()).limit(10).collect(Collectors.toList());
    }
    private final Map<UUID,String> names=new HashMap<>();
    public void save(){try{StringBuilder sb=new StringBuilder();for(var e:totalKills.entrySet())sb.append("boss="+e.getKey()+":"+e.getValue()+"\n");for(var e:firstKillers.entrySet())sb.append("first="+e.getKey()+":"+e.getValue()+"\n");Files.writeString(dataDir.resolve("bosses.txt"),sb.toString());}catch(Exception e){}}
    private void load(){try{Path f=dataDir.resolve("bosses.txt");if(!Files.exists(f))return;for(String l:Files.readAllLines(f)){if(l.startsWith("boss=")){String[]p=l.substring(5).split(":");if(p.length==2)totalKills.put(p[0],Integer.parseInt(p[1]));}else if(l.startsWith("first=")){String[]p=l.substring(6).split(":",2);if(p.length==2)firstKillers.put(p[0],p[1]);}}}catch(Exception e){}}
    private void broadcast(String msg){try{for(PlayerRef p:Universe.get().getPlayers())p.sendMessage(Message.raw(msg));}catch(Exception e){}System.out.println(msg);}
    public AbstractPlayerCommand getBossesCommand(){
        return new AbstractPlayerCommand("bosses","View boss kill stats. /bosses | /bosses top"){
            @Override protected void execute(CommandContext ctx,Store<EntityStore> store,Ref<EntityStore> ref,PlayerRef playerRef,World world){
                String sub=ctx.getInputString().trim().toLowerCase();
                if(sub.equals("top")){
                    playerRef.sendMessage(Message.raw("=== Top Boss Hunters ==="));
                    var top=getTopHunters();if(top.isEmpty()){playerRef.sendMessage(Message.raw("  No kills yet."));return;}
                    int i=1;for(var e:top){playerRef.sendMessage(Message.raw("  "+i+++". "+e.getValue()+" boss kills"));};return;
                }
                playerRef.sendMessage(Message.raw("=== Boss Kill Tracker ==="));
                for(String b:bosses){int k=totalKills.getOrDefault(b,0);String first=firstKillers.getOrDefault(b,"none");playerRef.sendMessage(Message.raw("  §c"+b+"§r — "+k+" global kills | First kill: §6"+first));}
                Map<String,Integer> mine=playerKills.getOrDefault(playerRef.getUuid(),Map.of());
                if(!mine.isEmpty()){playerRef.sendMessage(Message.raw("  Your kills:"));for(var e:mine.entrySet())if(e.getValue()>0)playerRef.sendMessage(Message.raw("    §c"+e.getKey()+"§r: "+e.getValue()));}
            }
        };
    }
    public AbstractPlayerCommand getBossKillCommand(){
        return new AbstractPlayerCommand("bosskill","[Admin] Record a boss kill. /bosskill <boss_type>"){
            @Override protected void execute(CommandContext ctx,Store<EntityStore> store,Ref<EntityStore> ref,PlayerRef playerRef,World world){
                String boss=ctx.getInputString().trim();if(boss.isEmpty()){playerRef.sendMessage(Message.raw("Usage: /bosskill <boss_type>"));return;}
                recordKill(playerRef.getUuid(),playerRef.getUsername(),boss);
            }
        };
    }
}

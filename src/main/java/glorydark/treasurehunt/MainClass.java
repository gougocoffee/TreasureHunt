package glorydark.treasurehunt;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Position;
import cn.nukkit.level.particle.DustParticle;
import cn.nukkit.level.particle.FlameParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.nukkit.utils.Utils.readFile;

public class MainClass extends PluginBase implements Listener {

    public static List<String> treasurePositions;
    public static Skin treasureSkin;
    public Integer maxCollectCount = 30;
    public List<String> rewardCommands;

    public boolean isKnockback;

    public static Config langConfig;

    public static String path;

    public static double scale;

    public static List<TreasureEntity> treasureEntities = new ArrayList<>();

    @Override
    public void onLoad() {
        this.getLogger().info(TextFormat.GREEN+"TreasureHunt onLoad");
    }

    @Override
    public void onEnable() {
        path = this.getDataFolder().getPath();
        this.saveResource("config.yml", false);
        this.saveResource("skins/skin.png", false);
        this.saveResource("skins/skin.json", false);
        this.saveResource("lang.properties", false);
        Config config = new Config(path+"/config.yml", Config.YAML);
        this.loadSkin();
        scale = config.getDouble("scale", 1.0);
        treasurePositions = new ArrayList<>(config.getStringList("treasurePositions"));
        this.spawnAllTreasures();
        this.maxCollectCount = config.getInt("maxCollectCount",30);
        this.rewardCommands = new ArrayList<>(config.getStringList("rewardCommands"));
        this.isKnockback = config.getBoolean("isKnockback", true);
        langConfig = new Config(path+"/lang.properties",Config.PROPERTIES);
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getCommandMap().register("", new BaseCommand("treasurehunt"));
        this.getLogger().info(TextFormat.GREEN+"TreasureHunt enabled");
        if(config.getBoolean("isParticleMarked", false)) {
            this.getServer().getScheduler().scheduleRepeatingTask(this, () -> {
                for (Entity entity : treasureEntities) {
                    for (Player player : Server.getInstance().getOnlinePlayers().values()) {
                        if (player.getLevel() == entity.getLevel() && player.distance(entity.getPosition()) < 5) {
                            Position pos = new Position(entity.x, entity.y+0.5, entity.z, entity.level);
                            if (!getPlayerCollect(player.getName()).contains(entity.namedTag.getString("treasurePositionText"))) {
                                ParticleEffect particleeffect = ParticleEffect.BLUE_FLAME;
                                for(int angle = 0;angle < 720;angle++){
                                    double x1 = pos.x + 1 * Math.cos(angle*3.14/180);
                                    double z1 = pos.z + 1 * Math.sin(angle*3.14/180);
                                    if(angle%30==0) {
                                        pos.getLevel().addParticleEffect(new Position(x1, pos.y, z1), particleeffect);
                                    }
                                }
                            } else {
                                ParticleEffect particleeffect = ParticleEffect.FALLING_DUST_GRAVEL;
                                for(int angle = 0;angle < 720;angle++){
                                    double x1 = pos.x + 1 * Math.cos(angle*3.14/180);
                                    double z1 = pos.z + 1 * Math.sin(angle*3.14/180);
                                    if(angle%30==0) {
                                        pos.getLevel().addParticleEffect(new Position(x1, pos.y, z1), particleeffect);
                                    }
                                }
                            }
                        }
                    }
                }
            }, 40);
        }
    }

    @Override
    public void onDisable() {
        for(Entity e: treasureEntities){
            e.kill();
            e.close();
        }
        treasureEntities = new ArrayList<>();
        treasurePositions = new ArrayList<>();
        this.getLogger().info(TextFormat.GREEN+"TreasureHunt onDisable");
    }

    public void loadSkin(){
        File skinFile = new File(path+"/skins/skin.png");
        File jsonFile = new File(path+"/skins/skin.json");
        Skin skin = new Skin();
        try {
            skin.setSkinData(ImageIO.read(skinFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        skin.setSkinId(jsonFile.getName());
        String geometryName = "default";
        for(String s:new Config(jsonFile, Config.JSON).getKeys(false)){
            if(s.startsWith("geometry")){
                geometryName = s;
            }
        }
        skin.setSkinResourcePatch("{\"geometry\":{\"default\":\""+geometryName+"\"}}");
        skin.generateSkinId(jsonFile.getName());
        skin.setGeometryName(geometryName);
        try {
            skin.setGeometryData(readFile(jsonFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        skin.setTrusted(true);
        treasureSkin = skin;
    }

    public void spawnAllTreasures(){
        for(String string: treasurePositions) {
            spawnTreasure(string);
        }
    }

    public static void spawnTreasure(String position){
        String[] strings = position.split(":");
        Position pos = new Position(Double.parseDouble(strings[0]), Double.parseDouble(strings[1]), Double.parseDouble(strings[2]), Server.getInstance().getLevelByName(strings[3]));
        CompoundTag tag = Entity.getDefaultNBT(new Vector3(pos.x, pos.y, pos.z));
        tag.putCompound("Skin", new CompoundTag()
                        .putByteArray("Data", treasureSkin.getSkinData().data)
                        .putString("ModelId", treasureSkin.getSkinId()))
                .putBoolean("isTreasureEntity", true)
                .putString("treasurePositionText", position);
        TreasureEntity entity = new TreasureEntity(pos.getChunk(), tag, pos);
        entity.setSkin(treasureSkin);
        entity.setScale((float) scale);
        entity.setNameTagVisible(false);
        entity.setImmobile();
        entity.spawnToAll();
        treasureEntities.add(entity);
    }

    public static void createTreasure(Player player, Position position){
        String pos = position.getFloorX()+":"+position.getFloorY()+":"+position.getFloorZ()+":"+position.getLevel().getName();
        if(!treasurePositions.contains(pos)) {
            treasurePositions.add(pos);
            spawnTreasure(pos);
            Config config = new Config(path + "/config.yml", Config.YAML);
            config.set("treasurePositions", treasurePositions);
            config.save();
            player.sendMessage(translateString("addTreasure_success", pos));
        }else{
            player.sendMessage(translateString("addTreasure_alreadyExist", pos));
        }
    }

    public static void removeTreasure(Position position){
        String pos = position.getFloorX()+":"+position.getFloorY()+":"+position.getFloorZ()+":"+position.getLevel().getName();
        List<TreasureEntity> cache = new ArrayList<>(treasureEntities);
        for(Entity entity: treasureEntities){
            if(entity.namedTag.getString("treasurePositionText").equals(pos)){
                entity.kill();
                entity.close();
                cache.remove(entity);
            }
        }
        treasureEntities = cache;
        treasurePositions.remove(pos);
    }

    public static void deleteTreasure(Player player, Position position){
        String pos = position.getFloorX()+":"+position.getFloorY()+":"+position.getFloorZ()+":"+position.getLevel().getName();
        if(treasurePositions.contains(pos)) {
            Config config = new Config(path + "/config.yml", Config.YAML);
            List<String> posCache = treasurePositions;
            posCache.remove(pos);
            config.set("treasurePositions", posCache);
            config.save();
            removeTreasure(position);
            player.sendMessage(translateString("removeTreasure_success", pos));
        }else{
            player.sendMessage(translateString("removeTreasure_notFound", pos));
        }
    }

    public List<String> getPlayerCollect(String player){
        Config config = new Config(this.getDataFolder().getPath()+"/players/"+player+".yml", Config.YAML);
        return new ArrayList<>(config.getStringList("list"));
    }

    public void setPlayerCollect(String player, List<String> strings){
        Config config = new Config(this.getDataFolder().getPath()+"/players/"+player+".yml", Config.YAML);
        config.set("list", strings);
        config.save();
    }

    public Boolean addPlayerCollect(Player player, String string){
        List<String> strings = getPlayerCollect(player.getName());
        if(!strings.contains(string)) {
            strings.add(string);
            setPlayerCollect(player.getName(), strings);
            if(strings.size() >= this.maxCollectCount){
                if(strings.size() == this.maxCollectCount) {
                    player.sendMessage(translateString("found_all_treasures", this.maxCollectCount - strings.size()));
                    for(String cmd : this.rewardCommands){
                        this.getServer().dispatchCommand(this.getServer().getConsoleSender(), cmd.replace("%player%", player.getName()));
                    }
                }else{
                    player.sendMessage(translateString("already_found_all_treasures"));
                }
            }else {
                player.sendMessage(translateString("found_treasure", maxCollectCount - strings.size()));
            }
            return true;
        }else{
            player.sendMessage(translateString("already_found_treasure"));
        }
        return false;
    }

    @EventHandler
    public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event){
        CompoundTag compoundTag = event.getEntity().namedTag;
        if(event.getDamager() instanceof Player && compoundTag.getBoolean("isTreasureEntity")){
            if(!addPlayerCollect((Player) event.getDamager(), compoundTag.getString("treasurePositionText"))){
                if(isKnockback) {
                    double deltaX = event.getDamager().x - event.getEntity().x;
                    double deltaZ = event.getDamager().z - event.getEntity().z;
                    knockBack(event.getDamager(), 0, deltaX, deltaZ, 1.0);
                }
            }
            event.setCancelled(true);
        }
    }

    public void knockBack(Entity entity, double damage, double x, double z, double base) {
        double f = Math.sqrt(x * x + z * z);
        if (!(f <= 0.0)) {
            f = 1.0 / f;
            Vector3 motion = new Vector3();
            motion.x /= 2.0;
            motion.y /= 2.0;
            motion.z /= 2.0;
            motion.x += x * f * base;
            motion.y += base;
            motion.z += z * f * base;
            if (motion.y > base) {
                motion.y = base;
            }
            entity.setMotion(motion);
        }
    }

    @EventHandler
    public void EntityDamageEvent(EntityDamageEvent event){
        if(event.getEntity() instanceof TreasureEntity){
            event.setCancelled(true);
        }
    }

    //by:lt-name CrystalWars
    public static String translateString(String key, Object... params) {
        String string = langConfig.getString(key, "§c Unknown key:" + key);
        if (params != null && params.length > 0) {
            for(int i = 1; i < params.length + 1; ++i) {
                string = string.replace("%" + i + "%", Objects.toString(params[i - 1]));
            }
        }
        return string;
    }

    public static String translateString(String key) {
        return langConfig.getString(key, "§c Unknown key:" + key);
    }
}

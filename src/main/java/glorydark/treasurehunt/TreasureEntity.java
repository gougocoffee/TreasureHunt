package glorydark.treasurehunt;

import cn.nukkit.Server;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * OriginalAuthor: SmallasWater
 */
public class TreasureEntity extends EntityHuman {
    private final Position position;


    public TreasureEntity(FullChunk chunk, CompoundTag nbt, Position position) {
        super(chunk, nbt);
        this.x += 0.5;
        this.z += 0.5;
        this.position = position;
    }

    @Deprecated //只是为了兼容PN核心
    public TreasureEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.position = new Position(0, 0, 0, Server.getInstance().getDefaultLevel()); //防止NPE
        this.close();
    }


    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
        this.setHealth(20.0F);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public Position getPosition() {
        return position;
    }
}
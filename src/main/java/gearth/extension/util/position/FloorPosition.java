package gearth.extension.util.position;

import gearth.protocol.HPacket;

public class FloorPosition implements Position {
    public final int x, y, z, dir;

    public FloorPosition(int x, int y, int z, int dir) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dir = dir;
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendObjects(x, y, dir);
    }
}

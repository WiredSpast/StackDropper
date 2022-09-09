package gearth.extension.util.position;

import gearth.protocol.HPacket;

public interface Position {
    FloorPosition FLOOR_ORIGIN = new FloorPosition(0, 0, 0, 0);
    WallPosition WALL_ORIGIN = new WallPosition(0, 0, 0, 0, 'r');

    void appendToPacket(HPacket packet);
}

package util;

import gearth.extensions.ExtensionBase;
import gearth.protocol.HPacket;

public class StackTile {
    private final ExtensionBase ext;
    public final int id;
    private double height;
    public final StackTileType type;

    public StackTile(ExtensionBase ext, int id, StackTileType type) {
        this.ext = ext;
        this.id = id;
        this.type = type;
        this.height = 0;
    }

    public StackTile(ExtensionBase ext, int id, StackTileType type, double initialHeight) {
        this.ext = ext;
        this.id = id;
        this.type = type;
        this.height = initialHeight;
    }

    public void incrementHeight(double increment) {
        height += increment;
        ext.sendToServer(new HPacket(String.format("{out:SetCustomStackingHeight}{i:%d}{i:%d}", id, Math.round(height * 100))));
        Util.sleep(16);
    }

    // {out:SetCustomStackingHeight}{i:225237071}{i:100}
    public void setHeight(double height) {
        ext.sendToServer(new HPacket(String.format("{out:SetCustomStackingHeight}{i:%d}{i:%d}", id, Math.round(height * 100))));
        this.height = height;
        Util.sleep(16);
    }

    public enum StackTileType {
        TILE1x1 ("tile_stackmagic", 1, 1),
        TILE1x2 ("tile_stackmagic1", 1, 2, 0),
        TILE2x1 ("tile_stackmagic1", 2, 1, 2),
        TILE2x2 ("tile_stackmagic2", 2, 2),
        TILE4x4 ("tile_stackmagic4x4", 4, 4),
        TILE6x6 ("tile_stackmagic6x6", 6, 6),
        TILE8x8 ("tile_stackmagic8x8", 8, 8);

        public final String className;
        public final int width;
        public final int length;
        public final int dropDirection;

        StackTileType(String className, int width, int length) {
            this.className = className;
            this.width = width;
            this.length = length;
            this.dropDirection = 0;
        }

        StackTileType(String className, int width, int length, int dropDirection) {
            this.className = className;
            this.width = width;
            this.length = length;
            this.dropDirection = dropDirection;
        }

        @Override
        public String toString() {
            return String.format("%dx%d", width, length);
        }
    }
}

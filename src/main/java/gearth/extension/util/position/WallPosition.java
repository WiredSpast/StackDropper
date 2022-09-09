package gearth.extension.util.position;

import gearth.protocol.HPacket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WallPosition implements Position {
    // {s:":w=4,9 l=9,91 l"}
    public final int wallX, wallY, localX, localY;
    public final char dir;

    public WallPosition(String position) {
        Pattern pattern = Pattern.compile(":w=(\\d*),(\\d*) l=(\\d*),(\\d*) ([lr])");
        Matcher matcher = pattern.matcher(position);
        if (matcher.matches()) {
            this.wallX = Integer.parseInt(matcher.group(1));
            this.wallY = Integer.parseInt(matcher.group(2));
            this.localX = Integer.parseInt(matcher.group(3));
            this.localY = Integer.parseInt(matcher.group(4));
            this.dir = matcher.group(5).charAt(0);
        } else {
            throw new RuntimeException(String.format("\"%s\" is not a valid wall position string", position));
        }
    }

    public WallPosition(int wallX, int wallY, int localX, int localY, char dir) {
        this.wallX = wallX;
        this.wallY = wallY;
        this.localX = localX;
        this.localY = localY;
        this.dir = dir;
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendString(toString());
    }

    @Override
    public String toString() {
        return String.format(":w=%d,%d l=%d,%d %c", wallX, wallY, localX, localY, dir);
    }

    public static void main(String[] args) {
        WallPosition pos = new WallPosition(":w=4,9 l=9,91 l");
        System.out.println(pos);
    }
}

package gearth.extension.dropper;

import gearth.extension.util.position.Position;

public interface Dropper<T extends Position> {
    void startPlacing(int id, T pos);
}

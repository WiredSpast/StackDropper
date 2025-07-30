package util;

import gearth.extensions.ExtensionBase;
import gearth.extensions.extra.tools.AwaitingPacket;
import gearth.extensions.parsers.HInventoryItem;
import gearth.extensions.parsers.HProductType;
import gearth.extensions.extra.tools.GAsync;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.HashMap;

public class Inventory {
    private final HashMap<Integer, HInventoryItem> items = new HashMap<>();
    private boolean loaded = false;
    private final InventoryLoadedListener loadedListener;
    private final ExtensionBase ext;

    public Inventory(ExtensionBase ext, InventoryLoadedListener loadedListener) {
        this.loadedListener = loadedListener;
        this.ext = ext;

        ext.intercept(HMessage.Direction.TOCLIENT, "FurniList", this::onFurniList);
        ext.intercept(HMessage.Direction.TOCLIENT, "FurniListAddOrUpdate", this::onFurniListAddOrUpdate);
        ext.intercept(HMessage.Direction.TOCLIENT, "FurniListRemove", this::onFurniListRemove);
    }

    private void onFurniList(HMessage hMessage) {
        new Thread(() -> {
            synchronized (items) {
                for (HInventoryItem item : HInventoryItem.parse(hMessage.getPacket()))
                    if (item.getType() == HProductType.FloorItem)
                        items.put(item.getId(), item);
            }
            loaded = true;
            loadedListener.loaded();
        }).start();
    }

    private void onFurniListAddOrUpdate(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        synchronized (items) {
            int count = packet.readInteger();
            for (int i = 0; i < count; i++) {
                HInventoryItem item = new HInventoryItem(packet);
                items.put(item.getId(), item);
            }
        }
    }

    private void onFurniListRemove(HMessage hMessage) {
        synchronized (items) {
            items.remove(hMessage.getPacket().readInteger());
        }
    }

    private void removeItemWithId(int id) {
        synchronized (items) {
            items.remove(id);
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void reload() {
        ext.sendToServer(new HPacket("{out:RequestFurniInventory}"));
    }

    public HInventoryItem findItemByPlacementId(int placementId) {
        synchronized (items) {
            return items.values().stream().filter(i -> i.getPlacementId() == placementId).findAny().orElse(null);
        }
    }

    public HInventoryItem findItemByItemTypeId(int itemTypeId, HProductType productType) {
        synchronized (items) {
            return items.values().stream()
                    .filter(item -> item.getType().equals(productType))
                    .filter(item -> item.getTypeId() == itemTypeId)
                    .findAny().orElse(null);
        }
    }

    public interface InventoryLoadedListener {
        void loaded();
    }

    public HPacket placeFloorItem(GAsync async, int id, int x, int y, int dir) {
        removeItemWithId(id);
        int tries = 0;
        String floorString = String.format("-%d %d %d %d", id, x, y, dir);
        while(tries < 10) {
            ext.sendToServer(new HPacket("PlaceObject", HMessage.Direction.TOSERVER, floorString));
            HPacket response = async.awaitPacket(new AwaitingPacket("ObjectAdd", HMessage.Direction.TOCLIENT, 100)
                    .addConditions(p -> p.readInteger() == id));
            if(response != null) {
                return response;
            }
            tries++;
        }
        return null;
    }

    public void pickUpItem(GAsync async, int id) {
        int tries = 0;
        while(tries < 10) {
            ext.sendToServer(new HPacket("PickupObject", HMessage.Direction.TOSERVER, 2, id));
            HPacket response = async.awaitPacket(new AwaitingPacket("ObjectRemove", HMessage.Direction.TOCLIENT, 50)
                    .addConditions(p -> Integer.parseInt(p.readString()) == id));
            if(response != null) return;
            tries++;
        }
    }
}

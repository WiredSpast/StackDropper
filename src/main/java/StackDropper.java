import gamedata.furnidata.FurniData;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HInventoryItem;
import gearth.extensions.parsers.HProductType;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import hotel.Hotel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.paint.Paint;
import util.GAsync;
import util.Inventory;
import util.StackTile;

import java.io.IOException;
import java.util.HashMap;

@ExtensionInfo(
        Title =         "Stack Dropper",
        Description =   "Drop any item from your inventory into a stack",
        Version =       "0.1",
        Author =        "WiredSpast"
)
public class StackDropper extends ExtensionForm {
    // FX-Components
    public Button enableOrDisableButton;
    public Spinner<Double> itemSpacingSpinner;
    public Spinner<Integer> maxItemCountSpinner;
    public Label inventoryStatusLabel;
    public ChoiceBox<StackTile.StackTileType> stackTileTypeBox;

    public int[] stackTileIds = new int[] {4811, 4885, 4886, 11811, 11812, 11813};

    private Inventory inventory;
    private GAsync async;
    private FurniData furniData;
    private boolean enabled = false;
    private final HashMap<Integer, Integer> placedStackTiles = new HashMap<>();
    private Thread placingThread;

    @Override
    protected void initExtension() {
        inventory = new Inventory(this, this::updateUI);
        async = new GAsync(this);

        intercept(HMessage.Direction.TOSERVER, "PlaceObject", this::onPlaceObject);

        itemSpacingSpinner.getEditor().setOnKeyTyped(e -> {
            if(!".0123456789".contains(e.getCharacter()) || (e.getCharacter().equals(".") && itemSpacingSpinner.getEditor().getText().contains("."))) {
                e.consume();
            }
        });

        maxItemCountSpinner.getEditor().setOnKeyTyped(e -> {
            if(!"-0123456789".contains(e.getCharacter()) || (e.getCharacter().equals("-") && maxItemCountSpinner.getEditor().getText().contains("-"))) {
                e.consume();
            }
        });

        stackTileTypeBox.getItems().addAll(StackTile.StackTileType.values());
        stackTileTypeBox.setValue(StackTile.StackTileType.TILE2x2);

        onConnect(this::doOnConnect);
    }

    public void doOnConnect(String host, int port, String hotelversion, String clientIdentifier, HClient clientType) {
        new Thread(() -> {
            try {
                switch (host) {
                    case "game-nl.habbo.com":
                        furniData = new FurniData(Hotel.NL);
                        break;
                    case "game-br.habbo.com":
                        furniData = new FurniData(Hotel.COMBR);
                        break;
                    case "game-tr.habbo.com":
                        furniData = new FurniData(Hotel.COMTR);
                        break;
                    case "game-de.habbo.com":
                        furniData = new FurniData(Hotel.DE);
                        break;
                    case "game-fr.habbo.com":
                        furniData = new FurniData(Hotel.FR);
                        break;
                    case "game-fi.habbo.com":
                        furniData = new FurniData(Hotel.FI);
                        break;
                    case "game-es.habbo.com":
                        furniData = new FurniData(Hotel.ES);
                        break;
                    case "game-it.habbo.com":
                        furniData = new FurniData(Hotel.IT);
                        break;
                    case "game-s2.habbo.com":
                        furniData = new FurniData(Hotel.SANDBOX);
                        break;
                    default:
                        furniData = new FurniData(Hotel.COM);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void reloadInventory(ActionEvent actionEvent) {
        inventory.reload();
    }

    public void enableOrDisable(ActionEvent actionEvent) {
        enabled = !enabled;
        if(!enabled && placingThread != null) {
            placingThread.interrupt();
            placingThread.stop();
            placingThread = null;
        }
        updateUI();
    }

    private void updateUI() {
        Platform.runLater(() -> {
            if(enabled) {
                enableOrDisableButton.setText("Disable");
            } else {
                enableOrDisableButton.setText("Enable");
            }

            if(inventory.isLoaded()) {
                inventoryStatusLabel.setText("Inventory loaded");
                inventoryStatusLabel.setTextFill(Paint.valueOf("LIME"));
            } else {
                inventoryStatusLabel.setText("Inventory not loaded");
                inventoryStatusLabel.setTextFill(Paint.valueOf("RED"));
            }
        });
    }

    @Override
    protected void onEndConnection() {
        enabled = false;
        updateUI();
    }

    private void sendMessage(String msg) {
        sendToClient(new HPacket("Shout", HMessage.Direction.TOCLIENT, -1, msg, 0, 30, 0, -1));
        GAsync.sleep(50);
    }

    // {out:PlaceObject}{s:"-124873506 12 21 2"}
    // {in:ObjectAdd}{i:258751840}{i:4655}{i:9}{i:34}{i:2}{s:"0.0"}{s:"0.65"}{i:0}{i:0}{s:"1"}{i:-1}{i:1}{i:11927526}{s:"WiredSpast"}
    private void onPlaceObject(HMessage hMessage) {
        if(enabled && placingThread == null) {
            placingThread = new Thread(() -> {
                String[] placementArray = hMessage.getPacket().readString().split(" ");

                int placementId = Integer.parseInt(placementArray[0]);

                HInventoryItem placedItem = inventory.findItemByPlacementId(placementId);
                if(placedItem != null) {
                    HPacket placedItemPacket = async.awaitPacket(new GAsync.AwaitingPacket("ObjectAdd", HMessage.Direction.TOCLIENT, 500)
                            .addConditions(p -> p.readInteger() == Math.abs(placementId)));
                    if(placedItemPacket != null) {
                        HFloorItem item = new HFloorItem(placedItemPacket);
                        startPlacing(placedItem.getTypeId(), item.getTile().getX(), item.getTile().getY(), item.getTile().getZ(), item.getFacing().ordinal());
                    } else {
                        sendMessage("Couldn't confirm placed item, please try again!");
                    }
                } else {
                    sendMessage("Couldn't identify placed object, try reloading your inventory!");
                }

            });
            placingThread.start();
        }
    }

    private void startPlacing(int typeId, int x, int y, double z, int dir) {
        sendMessage("Starting");

        int stackTileTypeId = furniData.getFurniDetailsByClassName(stackTileTypeBox.getValue().className).id;
        HInventoryItem stackTileInv = inventory.findItemByItemTypeId(stackTileTypeId, HProductType.FloorItem);
        if(stackTileInv == null) {
            sendMessage("Couldn't find a stacktile of the requested size in your inventory!");
            return;
        }
        HPacket placedStackTilePacket = inventory.placeFloorItem(async, stackTileInv.getId(), x, y, stackTileTypeBox.getValue().dropDirection);
        if(placedStackTilePacket == null) {
            sendMessage("Couldn't place the requested stacktile!");
            return;
        }
        HFloorItem stackTileItem = new HFloorItem(placedStackTilePacket);
        StackTile stackTile = new StackTile(this, stackTileItem.getId(), stackTileTypeBox.getValue(), z);

        int count = 1;
        boolean previousWasPlaced = true;
        while((maxItemCountSpinner.getValue() == -1 || count < maxItemCountSpinner.getValue())){
            HInventoryItem invItem = inventory.findItemByItemTypeId(typeId, HProductType.FloorItem);
            if(invItem == null) break;
            if(previousWasPlaced) stackTile.incrementHeight(itemSpacingSpinner.getValue());
            HPacket res = inventory.placeFloorItem(async, invItem.getId(), x, y, dir);
            previousWasPlaced = res != null;
            if(res != null) count++;
        }
        sendMessage(String.format("Placed %d items", count));
        inventory.pickUpItem(async, stackTile.id);

        placingThread = null;
    }
}

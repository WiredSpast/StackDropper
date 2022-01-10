package util;

import gearth.extensions.ExtensionBase;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;

public class GAsync {
    private final PacketInfoManager packetInfoManager;
    private final ExtensionBase ext;
    private final ArrayList<AwaitingPacket> awaitingPackets = new ArrayList<>();

    public GAsync(ExtensionBase ext) {
        this.packetInfoManager = ext.getPacketInfoManager();
        this.ext = ext;

        ext.intercept(HMessage.Direction.TOSERVER, this::onMessageToServer);
        ext.intercept(HMessage.Direction.TOCLIENT, this::onMessageToClient);
    }

    public void sendToServer(String hashOrName, Object... objects) {
        ext.sendToServer(new HPacket(hashOrName, HMessage.Direction.TOSERVER, objects));
    }

    public void sendToClient(String hashOrName, Object... objects) {
        ext.sendToClient(new HPacket(hashOrName, HMessage.Direction.TOCLIENT, objects));
    }

    private void onMessageToServer(HMessage hMessage) {
        PacketInfo info = packetInfoManager.getPacketInfoFromHeaderId(HMessage.Direction.TOSERVER, hMessage.getPacket().headerId());
        if(info == null) {
            return;
        }

        synchronized (awaitingPackets) {
            awaitingPackets.stream()
                    .filter(packet -> packet.direction.equals(HMessage.Direction.TOSERVER))
                    .filter(packet -> packet.headerName.equals(info.getName()))
                    .filter(packet -> packet.test(hMessage))
                    .forEach(packet -> packet.setPacket(hMessage.getPacket()));
        }
    }

    private void onMessageToClient(HMessage hMessage) {
        PacketInfo info = packetInfoManager.getPacketInfoFromHeaderId(HMessage.Direction.TOCLIENT, hMessage.getPacket().headerId());
        if(info == null) {
            return;
        }

        synchronized (awaitingPackets) {
            awaitingPackets.stream()
                    .filter(packet -> packet.direction.equals(HMessage.Direction.TOCLIENT))
                    .filter(packet -> packet.headerName.equals(info.getName()))
                    .filter(packet -> packet.test(hMessage))
                    .forEach(packet -> packet.setPacket(hMessage.getPacket()));
        }
    }

    public HPacket awaitPacket(AwaitingPacket packet) {
        synchronized (awaitingPackets) {
            awaitingPackets.add(packet);
        }

        while (true) {
            if (packet.isReady()) {
                synchronized (awaitingPackets) {
                    awaitingPackets.remove(packet);
                }
                return packet.getPacket();
            }
            sleep(1);
        }
    }

    public void clear() {
        synchronized (awaitingPackets) {
            awaitingPackets.clear();
        }
    }

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class AwaitingPacket {
        public final String headerName;
        public final HMessage.Direction direction;
        private HPacket packet = null;
        private boolean received = false;
        private final ArrayList<Predicate<? super HPacket>> conditions = new ArrayList<>();
        private final long start;
        private long minWait = 0;

        public AwaitingPacket(String headerName, HMessage.Direction direction, int maxWaitingTimeMillis) {
            this.headerName = headerName;
            this.direction = direction;

            if(maxWaitingTimeMillis < 30) {
                maxWaitingTimeMillis = 30;
            }

            AwaitingPacket packet = this;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    packet.received = true;
                }
            }, maxWaitingTimeMillis);

            this.start = System.currentTimeMillis();
        }

        public AwaitingPacket setMinWaitingTime(int millis) {
            this.minWait = millis;
            return this;
        }

        @SafeVarargs
        public final AwaitingPacket addConditions(Predicate<? super HPacket>... conditions) {
            this.conditions.addAll(Arrays.asList(conditions));
            return this;
        }

        private void setPacket(HPacket packet) {
            this.packet = packet;
            received = true;
        }

        public HPacket getPacket() {
            if(packet != null) {
                this.packet.resetReadIndex();
            }
            return this.packet;
        }

        private boolean test(HMessage hMessage) {
            for(Predicate<? super HPacket> condition : conditions) {
                HPacket packet = hMessage.getPacket();
                packet.resetReadIndex();
                if(!condition.test(packet)) {
                    return false;
                }
            }

            return true;
        }

        private boolean isReady() {
            return received && start + minWait < System.currentTimeMillis();
        }
    }
}

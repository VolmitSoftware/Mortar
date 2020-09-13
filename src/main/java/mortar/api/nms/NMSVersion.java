package mortar.api.nms;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public enum NMSVersion {
    v1_16_R2(Catalyst16.class),
    v1_15_R1(Catalyst15.class),
    v1_14_R1(Catalyst14.class),
    v1_13_R2(Catalyst13.class),
    v1_12_R1(Catalyst12.class),
    v1_11_R1(Catalyst11.class),
    v1_10_R1(Catalyst10.class),
    v1_9_R2(Catalyst9.class),
    v1_8_R3(Catalyst8.class);

    private static NMSVersion current;
    private final Class<? extends CatalystHost> hostClazz;

    NMSVersion(Class<? extends CatalystHost> hostClazz) {
        this.hostClazz = hostClazz;
    }

    static {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        String version = name.substring(name.lastIndexOf('.') + 1);
        try {
            current = valueOf(version);
        } catch (Throwable ignored) {
        }
    }

    public CatalystHost createHost() {
        try {
            return hostClazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<NMSVersion> getAboveInclusive() {
        List<NMSVersion> n = new ArrayList<>();

        for (NMSVersion i : values()) {
            if (i.ordinal() >= ordinal()) {
                n.add(i);
            }
        }

        return n;
    }

    public List<NMSVersion> betweenInclusive(NMSVersion other) {
        List<NMSVersion> n = new ArrayList<>();

        for (NMSVersion i : values()) {
            if (i.ordinal() <= Math.max(other.ordinal(), ordinal()) && i.ordinal() >= Math.min(ordinal(), other.ordinal())) {
                n.add(i);
            }
        }

        return n;
    }

    public List<NMSVersion> getBelowInclusive() {
        List<NMSVersion> n = new ArrayList<>();

        for (NMSVersion i : values()) {
            if (i.ordinal() <= ordinal()) {
                n.add(i);
            }
        }

        return n;
    }

    public static NMSVersion getMinimum() {
        return values()[values().length - 1];
    }

    public static NMSVersion getMaximum() {
        return values()[0];
    }

    public static NMSVersion current() {
        return current;
    }
}

package com.gmail.bobason01;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.Predicate;

import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

public class NoDamageIndicator extends JavaPlugin {
    private static final int LEGACY_DAMAGE_ID = 44;
    private volatile Predicate<PacketContainer> checker;

    @Override
    public void onEnable() {
        this.checker = buildChecker();
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST, PacketType.Play.Server.WORLD_PARTICLES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (checker.test(event.getPacket())) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @Override
    public void onDisable() {
    }

    private Predicate<PacketContainer> buildChecker() {
        String v = getServer().getBukkitVersion();
        boolean legacy = isLegacy(v);
        Predicate<PacketContainer> p1 = legacyIntReader();
        if (p1 != null) return p1;
        Predicate<PacketContainer> p2 = legacyEnumReader();
        if (p2 != null) return p2;
        Predicate<PacketContainer> p3 = modernProtocolLibReader();
        if (p3 != null) return p3;
        Predicate<PacketContainer> p4 = modernNmsKeyReader();
        if (p4 != null) return p4;
        return x -> false;
    }

    private boolean isLegacy(String bv) {
        try {
            String s = bv.split("-")[0];
            String[] arr = s.split("\\.");
            int major = Integer.parseInt(arr[1]);
            return major < 13;
        } catch (Throwable t) {
            return false;
        }
    }

    private Predicate<PacketContainer> legacyIntReader() {
        try {
            PacketContainer probe = new PacketContainer(PacketType.Play.Server.WORLD_PARTICLES);
            probe.getIntegers();
        } catch (Throwable t) {
            return null;
        }
        return new Predicate<PacketContainer>() {
            @Override
            public boolean test(PacketContainer packet) {
                try {
                    Integer id = packet.getIntegers().read(0);
                    return id != null && id.intValue() == LEGACY_DAMAGE_ID;
                } catch (Throwable t) {
                    return false;
                }
            }
        };
    }

    private Predicate<PacketContainer> legacyEnumReader() {
        try {
            Class<?> nms = findNms("PacketPlayOutWorldParticles");
            if (nms == null) return null;
            Field f = null;
            for (Field x : nms.getDeclaredFields()) {
                if (x.getType().isEnum()) {
                    f = x;
                    break;
                }
            }
            if (f == null) return null;
            f.setAccessible(true);
            Class<?> enumCls = f.getType();
            Object damage = null;
            for (Object e : enumCls.getEnumConstants()) {
                if (String.valueOf(e).equalsIgnoreCase("DAMAGE_INDICATOR")) {
                    damage = e;
                    break;
                }
            }
            if (damage == null) return null;
            MethodHandle hPacket = MethodHandles.publicLookup().findVirtual(PacketContainer.class, "getHandle", MethodType.methodType(Object.class));
            final Object damageConst = damage;
            final MethodHandle hField = MethodHandles.lookup().unreflectGetter(f);
            return new Predicate<PacketContainer>() {
                @Override
                public boolean test(PacketContainer packet) {
                    try {
                        Object handle = hPacket.invoke(packet);
                        Object val = hField.invoke(handle);
                        return val == damageConst;
                    } catch (Throwable t) {
                        return false;
                    }
                }
            };
        } catch (Throwable t) {
            return null;
        }
    }

    private Predicate<PacketContainer> modernProtocolLibReader() {
        try {
            Method m = PacketContainer.class.getMethod("getParticles");
            Object modifier = m.invoke(new PacketContainer(PacketType.Play.Server.WORLD_PARTICLES));
            Method read = modifier.getClass().getMethod("read", int.class);
            return new Predicate<PacketContainer>() {
                @Override
                public boolean test(PacketContainer packet) {
                    try {
                        Object particle = read.invoke(m.invoke(packet), 0);
                        if (particle == null) return false;
                        String s = particle.toString().toLowerCase(Locale.ROOT);
                        if (s.contains("damage_indicator")) return true;
                        if (s.contains("minecraft:damage_indicator")) return true;
                        return false;
                    } catch (Throwable t) {
                        return false;
                    }
                }
            };
        } catch (Throwable t) {
            try {
                Method m = PacketContainer.class.getMethod("getNewParticles");
                Object modifier = m.invoke(new PacketContainer(PacketType.Play.Server.WORLD_PARTICLES));
                Method read = modifier.getClass().getMethod("readSafely", int.class);
                return new Predicate<PacketContainer>() {
                    @Override
                    public boolean test(PacketContainer packet) {
                        try {
                            Object particle = read.invoke(m.invoke(packet), 0);
                            if (particle == null) return false;
                            String s = particle.toString().toLowerCase(Locale.ROOT);
                            if (s.contains("damage_indicator")) return true;
                            if (s.contains("minecraft:damage_indicator")) return true;
                            return false;
                        } catch (Throwable t) {
                            return false;
                        }
                    }
                };
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    private Predicate<PacketContainer> modernNmsKeyReader() {
        try {
            Class<?> nms = findNms("PacketPlayOutWorldParticles");
            if (nms == null) nms = findCraft("PacketPlayOutWorldParticles");
            if (nms == null) return null;
            Field f = null;
            for (Field x : nms.getDeclaredFields()) {
                Class<?> c = x.getType();
                if (c.getName().contains("ParticleParam") || c.getName().contains("Particle") || c.getName().contains("Particles")) {
                    f = x;
                    break;
                }
            }
            if (f == null) return null;
            f.setAccessible(true);
            MethodHandle hPacket = MethodHandles.publicLookup().findVirtual(PacketContainer.class, "getHandle", MethodType.methodType(Object.class));
            MethodHandle hField = MethodHandles.lookup().unreflectGetter(f);
            MethodHandle hToString = MethodHandles.publicLookup().findVirtual(Object.class, "toString", MethodType.methodType(String.class));
            return new Predicate<PacketContainer>() {
                @Override
                public boolean test(PacketContainer packet) {
                    try {
                        Object handle = hPacket.invoke(packet);
                        Object p = hField.invoke(handle);
                        if (p == null) return false;
                        String s = (String) hToString.invoke(p);
                        if (s == null) return false;
                        s = s.toLowerCase(Locale.ROOT);
                        if (s.contains("damage_indicator")) return true;
                        if (s.contains("minecraft:damage_indicator")) return true;
                        return false;
                    } catch (Throwable t) {
                        return false;
                    }
                }
            };
        } catch (Throwable t) {
            return null;
        }
    }

    private Class<?> findNms(String name) {
        try {
            String p = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
            String v = p.substring(p.lastIndexOf('.') + 1);
            try {
                return Class.forName("net.minecraft.server." + v + "." + name);
            } catch (ClassNotFoundException e) {
                try {
                    return Class.forName("net.minecraft.server." + name);
                } catch (ClassNotFoundException e2) {
                    return null;
                }
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private Class<?> findCraft(String name) {
        try {
            String p = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
            String v = p.substring(p.lastIndexOf('.') + 1);
            try {
                return Class.forName("org.bukkit.craftbukkit." + v + "." + name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }
}

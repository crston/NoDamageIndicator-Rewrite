package com.gmail.bobason01;

import java.util.logging.Logger;

import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedParticle;

public class NoDamageIndicator extends JavaPlugin {
    private Logger logger;

    @Override
    public void onEnable() {
        logger = this.getLogger();
        logger.info("Starting on " + this.getServer().getVersion());

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST, PacketType.Play.Server.WORLD_PARTICLES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                WrappedParticle<?> particle = packet.getNewParticles().readSafely(0);
                if (particle != null && particle.getParticle() == Particle.DAMAGE_INDICATOR) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @Override
    public void onDisable() {
        logger.info("Disabled!");
    }
}

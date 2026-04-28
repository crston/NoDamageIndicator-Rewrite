package com.gmail.bobason01;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedParticle;
import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;

public class NoDamageIndicator extends JavaPlugin {

    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();

        // 월드 파티클 패킷 (World Particles) 감지 리스너 등록
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST, PacketType.Play.Server.WORLD_PARTICLES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                // 패킷에서 파티클 정보를 가져옵니다
                WrappedParticle particle = packet.getNewParticles().read(0);

                if (particle != null) {
                    // 파티클 타입이 DAMAGE_INDICATOR 인지 확인합니다
                    if (particle.getParticle() == Particle.DAMAGE_INDICATOR) {
                        // 해당 패킷 전송을 취소하여 클라이언트 화면에 안 보이게 합니다
                        event.setCancelled(true);
                    }
                }
            }
        });

        getLogger().info("NoDamageIndicator Plugin Enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("NoDamageIndicator Plugin Disabled");
    }
}
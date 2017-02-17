package org.inventivetalent.rpapi.packet;

import net.minecraft.server.v1_7_R4.*;
import net.minecraft.util.com.google.common.collect.BiMap;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelPromise;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.inventivetalent.rpapi.IPacketPlayResourcePackStatus;
import org.inventivetalent.rpapi.RPApiPlugin;
import org.inventivetalent.rpapi.Status;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class PacketPlayResourcePackStatus_v1_7_R4 extends Packet implements IPacketPlayResourcePackStatus {

	private Status status;
	private String hash;
	private Player p;

	@Override
	public void a(PacketDataSerializer serializer) throws IOException {
		this.hash = serializer.c(255);
		this.status = Status.byID(serializer.a());
		if (this.getStatus() != null && this.p != null) {
			RPApiPlugin.onResourcePackResult(this.getStatus(), this.p, this.hash);
		}
	}

	@Override
	public void b(PacketDataSerializer serializer) throws IOException {
	}

	@Override
	public void handle(PacketListener paramPacketListener) {
	}

	@Override
	public Status getStatus() {
		return this.status;
	}

	@Override
	public String getHash() {
		return this.hash;
	}

	@Override
	public void onPacketReceive(Object packet, final Player p) {
		if (!(packet instanceof Packet)) { return; }
		if (!packet.getClass().equals(this.getClass())) { return; }
		this.p = p;
		if (this.getStatus() != null && p != null) {
			RPApiPlugin.onResourcePackResult(this.getStatus(), p, this.getHash());
		}
	}

	@Override
	public void inject() throws NoSuchFieldException, IllegalAccessException {
		addPacket(EnumProtocol.PLAY, false, 25, this.getClass());
	}

	@SuppressWarnings({
			"rawtypes",
			"unchecked" })
	private static void addPacket(EnumProtocol protocol, boolean clientbound, int id, Class<? extends Packet> packet) throws NoSuchFieldException, IllegalAccessException {
		Field packets;
		if (!clientbound) {
			packets = EnumProtocol.class.getDeclaredField("h");
		} else {
			packets = EnumProtocol.class.getDeclaredField("i");
		}
		packets.setAccessible(true);
		BiMap<Integer, Class<? extends Packet>> pMap = (BiMap) packets.get(protocol);
		pMap.put(Integer.valueOf(id), packet);
		Field map = EnumProtocol.class.getDeclaredField("f");
		map.setAccessible(true);
		Map<Class<? extends Packet>, EnumProtocol> protocolMap = (Map) map.get(null);
		protocolMap.put(packet, protocol);
	}

	private static Field channelField;

	@Override
	public void addChannelForPlayer(final Player p) {
		if (channelField == null) {
			try {
				channelField = NetworkManager.class.getDeclaredField("m");
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
			channelField.setAccessible(true);
		}
		try {
			EntityPlayer ep = ((CraftPlayer) p).getHandle();
			final Channel channel = (Channel) channelField.get(ep.playerConnection.networkManager);
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						channel.pipeline().addBefore("packet_handler", "RPApi", new ChannelHandler(p));
					} catch (Exception e) {
					}
				}
			}, "RPApi channel adder").start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void removeChannelForPlayer(Player p) {
		if (channelField == null) {
			try {
				channelField = NetworkManager.class.getDeclaredField("m");
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
			channelField.setAccessible(true);
		}
		try {
			EntityPlayer ep = ((CraftPlayer) p).getHandle();
			final Channel channel = (Channel) channelField.get(ep.playerConnection.networkManager);
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						channel.pipeline().remove("RPApi");
					} catch (Exception e) {
					}
				}
			}, "RPApi channel remover").start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class ChannelHandler extends ChannelDuplexHandler {

		private Player p;

		public ChannelHandler(Player p) {
			this.p = p;
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			super.write(ctx, msg, promise);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (IPacketPlayResourcePackStatus.class.isAssignableFrom(msg.getClass())) {
				((IPacketPlayResourcePackStatus) msg).onPacketReceive(msg, this.p);
			}
			super.channelRead(ctx, msg);
		}

	}
}

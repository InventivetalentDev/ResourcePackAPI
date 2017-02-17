package org.inventivetalent.rpapi.packet;

import com.google.common.collect.BiMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.minecraft.server.v1_8_R1.*;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.inventivetalent.rpapi.IPacketPlayResourcePackStatus;
import org.inventivetalent.rpapi.RPApiPlugin;
import org.inventivetalent.rpapi.Status;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class PacketPlayResourcePackStatus_v1_8_R1 implements Packet, IPacketPlayResourcePackStatus {

	private Status status;
	private String hash;
	private Player p;

	@Override
	public void a(PacketDataSerializer serializer) throws IOException {
		this.hash = serializer.c(40);
		int status = ((EnumResourcePackStatus) serializer.a(EnumResourcePackStatus.class)).ordinal();
		this.status = Status.byID(status);
		if (this.getStatus() != null && this.p != null) {
			RPApiPlugin.onResourcePackResult(this.getStatus(), this.p, this.getHash());
		}
	}

	@Override
	public void b(PacketDataSerializer serializer) throws IOException {
	}

	@Override
	public void a(PacketListener paramPacketListener) {
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
			"unchecked",
			"rawtypes" })
	private static void addPacket(EnumProtocol protocol, boolean clientbound, int id, Class<? extends Packet> packet) throws NoSuchFieldException, IllegalAccessException {
		EnumProtocolDirection dir = clientbound ? EnumProtocolDirection.CLIENTBOUND : EnumProtocolDirection.SERVERBOUND;
		Field mapField = EnumProtocol.class.getDeclaredField("h");
		mapField.setAccessible(true);
		Map map = (Map) mapField.get(protocol);

		BiMap<Integer, Class> biMap = (BiMap<Integer, Class>) map.get(dir);
		biMap.put(Integer.valueOf(id), packet);
		map.put(dir, biMap);
	}

	private static Field channelField;

	@Override
	public void addChannelForPlayer(final Player p) {
		if (channelField == null) {
			try {
				channelField = NetworkManager.class.getDeclaredField("i");
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
				channelField = NetworkManager.class.getDeclaredField("i");
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

	public class ChannelHandler extends ByteToMessageDecoder {

		private Player p;

		public ChannelHandler(Player p) {
			this.p = p;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (IPacketPlayResourcePackStatus.class.isAssignableFrom(msg.getClass())) {
				((IPacketPlayResourcePackStatus) msg).onPacketReceive(msg, this.p);
			}
			super.channelRead(ctx, msg);
		}

		@Override
		protected void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, List<Object> paramList) throws Exception {
		}

	}

}

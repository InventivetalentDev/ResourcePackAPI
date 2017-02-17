package org.inventivetalent.rpapi.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.minecraft.server.v1_9_R1.EntityPlayer;
import net.minecraft.server.v1_9_R1.NetworkManager;
import net.minecraft.server.v1_9_R1.Packet;
import net.minecraft.server.v1_9_R1.PacketPlayInResourcePackStatus;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.inventivetalent.rpapi.IPacketPlayResourcePackStatus;
import org.inventivetalent.rpapi.RPApiPlugin;
import org.inventivetalent.rpapi.Status;

import java.lang.reflect.Field;
import java.util.List;

public class PacketPlayResourcePackStatus_v1_9_R1 implements IPacketPlayResourcePackStatus {

	private Status status;
	private String hash;
	private Player p;

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
		this.p = p;

		try {
			Field field = PacketPlayInResourcePackStatus.class.getDeclaredField("status");
			field.setAccessible(true);

			this.status = Status.byID(((PacketPlayInResourcePackStatus.EnumResourcePackStatus) field.get(packet)).ordinal());

			field = PacketPlayInResourcePackStatus.class.getDeclaredField("a");
			field.setAccessible(true);

			this.hash = (String) field.get(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (this.getStatus() != null && p != null) {
			RPApiPlugin.onResourcePackResult(this.getStatus(), p, this.getHash());
		}
	}

	@Override
	public void inject() throws NoSuchFieldException, IllegalAccessException {
	}

	private static Field channelField;

	@Override
	public void addChannelForPlayer(final Player p) {
		if (channelField == null) {
			try {
				channelField = NetworkManager.class.getDeclaredField("channel");
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
				channelField = NetworkManager.class.getDeclaredField("channel");
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
			if (PacketPlayInResourcePackStatus.class.isAssignableFrom(msg.getClass())) {
				PacketPlayResourcePackStatus_v1_9_R1.this.onPacketReceive(msg, this.p);
			}
			super.channelRead(ctx, msg);
		}

		@Override
		protected void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, List<Object> paramList) throws Exception {
		}

	}

}

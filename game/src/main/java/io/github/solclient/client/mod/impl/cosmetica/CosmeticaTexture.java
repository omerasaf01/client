package io.github.solclient.client.mod.impl.cosmetica;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import io.github.solclient.client.mixin.client.MixinTextureUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITickableTextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

final class CosmeticaTexture extends AbstractTexture implements ITickableTextureObject {

	private static Set<ResourceLocation> all = new HashSet<>();

	private int[] textures;
	private int frame;
	private int ticks;
	private final int aspectRatio;
	private final int frameDelay;
	private String base64;

	CosmeticaTexture(int aspectRatio, int frameDelay, String base64) {
		this.aspectRatio = aspectRatio;
		this.frameDelay = ticks = frameDelay;
		this.base64 = base64;
	}

	private static String strictParse(String input) {
		if (input.startsWith("data:image/png;base64,")) {
			return input.substring(22);
		}

		return null;
	}

	private static ResourceLocation target(String base64) {
		return new ResourceLocation("sol_client_base64", base64);
	}

	static void disposeAll() {
		all.forEach((location) -> Minecraft.getMinecraft().getTextureManager().deleteTexture(location));

		if(!all.isEmpty()) {
			all = new HashSet<>();
		}
	}

	static ResourceLocation load(int aspectRatio, int frameDelay, String url) {
		String base64 = strictParse(url);
		if(base64 == null) {
			throw new IllegalArgumentException(url);
		}

		ResourceLocation target = target(base64);

		if(all.contains(target)) {
			return target;
		}

		all.add(target);

		CosmeticaTexture texture = new CosmeticaTexture(aspectRatio, frameDelay, base64);
		Minecraft.getMinecraft().getTextureManager().loadTickableTexture(target, texture);
		return target;
	}

	@Override
	public void loadTexture(IResourceManager resourceManager) throws IOException {
		deleteGlTexture();

		try(ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(base64))) {
			BufferedImage image = ImageIO.read(in);
			int frames = (aspectRatio * image.getHeight()) / image.getWidth();
			int frameHeight = image.getHeight() / frames;
			textures = new int[frames];
			for(int i = 0; i < frames; i++) {
				textures[i] = GL11.glGenTextures();
				TextureUtil.allocateTexture(textures[i], image.getWidth(), frameHeight);

				// modified from code in TextureUtil

				int width = image.getWidth();
		        int height = frameHeight;
		        int k = 4194304 / width;
		        int[] sample = new int[k * width];

				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

				for (int j = 0; j < width * height; j += width * k) {
					int y = j / width;
					int sampleHeight = Math.min(k, height - y);
					int length = width * sampleHeight;
					image.getRGB(0, i * frameHeight + y, width, sampleHeight, sample, 0, width);
					MixinTextureUtil.copyToBuffer(sample, length);
					GL11.glTexSubImage2D(
							GL11.GL_TEXTURE_2D, 0, 0, y, width, sampleHeight, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, MixinTextureUtil.getDataBuffer());
		        }
			}
		}
	}

	@Override
	public int getGlTextureId() {
		if(textures == null) {
			return -1;
		}

		return textures[frame];
	}

	@Override
	public void deleteGlTexture() {
		if(textures == null) {
			return;
		}

		for(int texture : textures) {
			TextureUtil.deleteTexture(texture);
		}

		textures = null;
	}

	@Override
	public void tick() {
		if(textures == null) {
			return;
		}

		if(--ticks < 0) {
			ticks = frameDelay;
			if(++frame > textures.length - 1) {
				frame = 0;
			}
		}
	}

}

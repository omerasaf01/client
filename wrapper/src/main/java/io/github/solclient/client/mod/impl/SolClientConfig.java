package io.github.solclient.client.mod.impl;

import java.io.*;

import org.lwjgl.input.Keyboard;

import com.google.gson.*;
import com.google.gson.annotations.Expose;

import io.github.solclient.client.GlobalConstants;
import io.github.solclient.client.mod.*;
import io.github.solclient.client.mod.annotation.Option;
import io.github.solclient.client.util.SemVer;
import io.github.solclient.client.util.data.Colour;
import net.minecraft.client.settings.KeyBinding;

public class SolClientConfig extends ConfigOnlyMod {

	public static SolClientConfig instance;

	@Expose
	@Option
	public boolean remindMeToUpdate = true;

	@Expose
	@Option
	public boolean fancyMainMenu = true;

	@Expose
	@Option
	public boolean logoInInventory;

	@Option
	public KeyBinding modsKey = new KeyBinding(getTranslationKey() + ".mods", Keyboard.KEY_RSHIFT,
			GlobalConstants.KEY_CATEGORY);

	@Option
	public KeyBinding editHudKey = new KeyBinding(getTranslationKey() + ".edit_hud", 0, GlobalConstants.KEY_CATEGORY);

	@Expose
	@Option
	public Colour uiColour = new Colour(255, 180, 0);
	public Colour uiHover;

	@Expose
	@Option
	public boolean smoothUIColours = true;

	@Expose
	@Option
	public boolean roundedUI = true;

	@Expose
	@Option
	public boolean buttonClicks = true;

	@Expose
	@Option
	public boolean smoothScrolling = true;

	public SemVer latestRelease;

	@Override
	public String getId() {
		return "sol_client";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.GENERAL;
	}

	@Override
	public void onRegister() {
		super.onRegister();

		instance = this;
		uiHover = getUiHover();

		// yuck...
		if (GlobalConstants.AUTOUPDATE)
			getOptions().remove(0);
		else if (remindMeToUpdate) {
			Thread thread = new Thread(() -> {
				try (InputStream in = GlobalConstants.RELEASE_API.openStream()) {
					JsonObject object = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
					latestRelease = SemVer.parseOrNull(object.get("name").getAsString());
				} catch (Throwable error) {
					logger.warn("Could not check for updates", error);
				}
			});
			thread.setDaemon(true);
			thread.start();
		}
	}

	@Override
	public void postOptionChange(String key, Object value) {
		super.postOptionChange(key, value);

		if (key.equals("uiColour")) {
			uiHover = getUiHover();
		}
	}

	@Override
	public String getDescription() {
		return super.getDescription();
	}

	private Colour getUiHover() {
		return uiColour.add(40);
	}

}

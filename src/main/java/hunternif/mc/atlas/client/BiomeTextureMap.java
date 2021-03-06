package hunternif.mc.atlas.client;

import static hunternif.mc.atlas.client.TextureSet.*;
import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.core.Tile;
import hunternif.mc.atlas.util.SaveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.primitives.UnsignedBytes;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Maps biome IDs (or pseudo IDs) to textures. <i>Not thread-safe!</i>
 * <p>If several textures are set for one ID, one will be chosen at random when
 * putting tile into Atlas.</p> 
 * @author Hunternif
 */
@SideOnly(Side.CLIENT)
public class BiomeTextureMap extends SaveData {
	private static final BiomeTextureMap INSTANCE = new BiomeTextureMap();
	public static BiomeTextureMap instance() {
		return INSTANCE;
	}
	
	/** This map allows keys other than the 256 biome IDs to use for special tiles. */
	final Map<Integer, TextureSet> textureMap = new HashMap<Integer, TextureSet>();
	
	public static final TextureSet defaultTexture = PLAINS;
	
	/** Assign texture set to biome. */
	public void setTexture(int biomeID, TextureSet textureSet) {
		TextureSet previous = textureMap.put(biomeID, textureSet);
		if (previous == null) {
			markDirty();
		} else if (previous != textureSet) {
			AntiqueAtlasMod.logger.warn("Overwriting texture set for biome " + biomeID);
			markDirty();
		}
		if (previous == null) {
			textureMap.put(biomeID, textureSet);
			markDirty();
		}
	}
	
	/** Find the most appropriate standard texture set depending on
	 * BiomeDictionary types. */
	private void autoRegister(int biomeID) {
		if (biomeID < 0 || biomeID >= 256) {
			AntiqueAtlasMod.logger.warn("Biome ID " + biomeID + " is out of range. "
					+ "Auto-registering default texture set");
			setTexture(biomeID, defaultTexture);
			return;
		}
		BiomeGenBase biome = BiomeGenBase.getBiome(biomeID);
		if (biome == null) {
			AntiqueAtlasMod.logger.warn("Biome ID " + biomeID + " is null. "
					+ "Auto-registering default texture set");
			setTexture(biomeID, defaultTexture);
			return;
		}
		List<Type> types = Arrays.asList(BiomeDictionary.getTypesForBiome(biome));
		if (types.contains(Type.SWAMP)) {
			setTexture(biomeID, SWAMP);
		} else if (types.contains(Type.WATER)) {
			// Water + trees = swamp
			if (types.contains(Type.FOREST) || types.contains(Type.JUNGLE) || types.contains(Type.SWAMP)) {
				setTexture(biomeID, SWAMP);
			} else if (types.contains(Type.SNOWY)){
				setTexture(biomeID, ICE);
			} else {
				setTexture(biomeID, WATER);
			}
		} else if (types.contains(Type.MOUNTAIN)) {
			setTexture(biomeID, MOUNTAINS);
		} else if (types.contains(Type.HILLS)) {
			if (types.contains(Type.FOREST)) {
				// Frozen forest automatically counts as pines:
				if (types.contains(Type.SNOWY)) {
					setTexture(biomeID, PINES_HILLS);
				} else {
					setTexture(biomeID, FOREST_HILLS);
				}
			} else if (types.contains(Type.JUNGLE)) {
				setTexture(biomeID, JUNGLE_HILLS);
			} else {
				setTexture(biomeID, HILLS);
			}
		} else if (types.contains(Type.JUNGLE)) {
			setTexture(biomeID, JUNGLE);
		} else if (types.contains(Type.FOREST)) {
			// Frozen forest automatically counts as pines:
			if (types.contains(Type.SNOWY)) {
				setTexture(biomeID, PINES);
			} else {
				setTexture(biomeID, FOREST);
			}
		} else if (types.contains(Type.SANDY) || types.contains(Type.WASTELAND)) {
			if (types.contains(Type.SNOWY)) {
				setTexture(biomeID, SNOW);
			} else {
				setTexture(biomeID, SAND);
			}
		} else if (types.contains(Type.BEACH)){
			setTexture(biomeID, BEACH);
		} else {
			setTexture(biomeID, defaultTexture);
		}
		AntiqueAtlasMod.logger.info("Auto-registered standard texture set for biome " + biomeID);
	}
	
	/** Auto-registers the biome ID if it is not registered. */
	public void checkRegistration(int biomeID) {
		if (!isRegistered(biomeID)) {
			autoRegister(biomeID);
			markDirty();
		}
	}
	
	public boolean isRegistered(int biomeID) {
		return textureMap.containsKey(biomeID);
	}

	public int getVariations(int biomeID) {
		checkRegistration(biomeID);
		TextureSet set = textureMap.get(biomeID);
		return set.textures.length;
	}

	public ResourceLocation getTexture(Tile tile) {
		checkRegistration(tile.biomeID);
		TextureSet set = textureMap.get(tile.biomeID);
		int i = Math.round((float)(UnsignedBytes.toInt(tile.getVariationNumber()))
				/ 256f * (float)(set.textures.length - 1));
		return set.textures[i];
	}
	
	/** Whether the first tile should be stitched to the 2nd
	 * (but the opposite is not always true!) */
	public boolean shouldStitchTo(int biomeID, int toBiomeID) {
		checkRegistration(biomeID);
		checkRegistration(toBiomeID);
		TextureSet entry = textureMap.get(biomeID);
		TextureSet toEntry = textureMap.get(toBiomeID);
		return entry.shouldStichTo(toEntry);
	}
	
	public List<ResourceLocation> getAllTextures() {
		List<ResourceLocation> list = new ArrayList<ResourceLocation>(textureMap.size());
		for (Entry<Integer, TextureSet> entry : textureMap.entrySet()) {
			list.addAll(Arrays.asList(entry.getValue().textures));
		}
		return list;
	}
}
/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.villages;

import com.mojang.datafixers.util.Pair;
import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.common.config.ConfigHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.jigsaw.JigsawPattern;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.SingleJigsawPiece;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.List;

public class VillageStructures {
    /**
     * Adds the building to the targeted pool.
     * We will call this in addNewVillageBuilding method further down to add to every village.
     *
     * Note: This is an additive operation which means multiple mods can do this and they stack with each other safely.
     *
     * With thanks to TelepathicGrunt: https://gist.github.com/TelepathicGrunt/4fdbc445ebcbcbeb43ac748f4b18f342
     */
    private static void addPieceToPool(MutableRegistry<JigsawPattern> templatePoolRegistry, ResourceLocation poolRL, String nbtPieceRL, JigsawPattern.PlacementBehaviour projection, int weight) {
        // Grab the pool we want to add to
        JigsawPattern pool = templatePoolRegistry.get(poolRL);
        if (pool == null) return;

        // Grabs the nbt piece and creates a SingleJigsawPiece of it that we can add to a structure's pool.
        // Note: street pieces are a legacy_single_pool_piece type, houses are single_pool_piece
        SingleJigsawPiece piece = poolRL.getPath().endsWith("streets") ?
                SingleJigsawPiece.legacy(nbtPieceRL).apply(projection) :
                SingleJigsawPiece.single(nbtPieceRL).apply(projection);


        // AccessTransformer to make JigsawPattern's templates field public for us to see.
        // public net.minecraft.world.gen.feature.jigsaw.JigsawPattern templates #templates
        // Weight is handled by how many times the entry appears in this list.
        // We do not need to worry about immutability as this field is created using Lists.newArrayList(); which makes a mutable list.
        for (int i = 0; i < weight; i++) {
            pool.templates.add(piece);
        }

        // AccessTransformer to make JigsawPattern's rawTemplates field public for us to see.
        // net.minecraft.world.gen.feature.jigsaw.JigsawPattern rawTemplates #rawTemplates
        // This list of pairs of pieces and weights is not used by vanilla by default but another mod may need it for efficiency.
        // So lets add to this list for completeness. We need to make a copy of the array as it can be an immutable list.
        List<Pair<JigsawPiece, Integer>> listOfPieceEntries = new ArrayList<>(pool.rawTemplates);
        listOfPieceEntries.add(new Pair<>(piece, weight));
        pool.rawTemplates = listOfPieceEntries;
    }

    public static void addMechanicHouse(final FMLServerAboutToStartEvent event) {
        if (ConfigHelper.common().villagers.addMechanicHouse.get()) {
            MutableRegistry<JigsawPattern> templatePoolRegistry = event.getServer().registryAccess().registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);

            for (VillageBiome v : VillageBiome.values()) {
                // desert & snowy villages don't have street pieces large enough to support a PNC house
                // in this case, we add a custom street with extra reserved space big enough for the house
                // - the jigsaw pieces in that street use a custom pool in PNC's namespace which has only our house in it
                if (v.needsCustomStreet()) {
                    // note: in this case, our mechanic house is in the custom pneumaticcraft:village/<biome>/houses
                    //   template pool JSON, so doesn't need to be added in code
                    addPieceToPool(templatePoolRegistry,
                            new ResourceLocation("village/" + v.getBiomeName() + "/streets"),
                            Names.MOD_ID + ":villages/custom_street_" + v.getBiomeName(),
                            JigsawPattern.PlacementBehaviour.TERRAIN_MATCHING, 2);
                } else {
                    // add the house to the vanilla minecraft:village/<biome>/houses pool
                    addPieceToPool(templatePoolRegistry,
                            new ResourceLocation("village/" + v.getBiomeName() + "/houses"),
                            Names.MOD_ID + ":villages/mechanic_house_" + v.getBiomeName(),
                            JigsawPattern.PlacementBehaviour.RIGID, 8
                    );
                }
            }
        }
    }

    enum VillageBiome {
        PLAINS("plains", false),
        DESERT("desert", true),
        SAVANNA("savanna", false),
        TAIGA("taiga", false),
        SNOWY("snowy", true);

        private final String biomeName;
        private final boolean needsCustomStreet;

        VillageBiome(String biomeName, boolean needsCustomStreet) {
            this.biomeName = biomeName;
            this.needsCustomStreet = needsCustomStreet;
        }

        public String getBiomeName() {
            return biomeName;
        }

        public boolean needsCustomStreet() {
            return needsCustomStreet;
        }
    }
}

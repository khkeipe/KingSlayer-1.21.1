package com.koreykeipe.kingslayer;

import com.koreykeipe.kingslayer.airdrop.AirdropConfig;
import com.koreykeipe.kingslayer.block.ModBlocks;
import com.koreykeipe.kingslayer.client.AirdropEntityRenderer;
import com.koreykeipe.kingslayer.entity.ModEntityTypes;
import com.koreykeipe.kingslayer.item.ModCreativeModeTabs;
import com.koreykeipe.kingslayer.item.ModItems;
import com.koreykeipe.kingslayer.loot.ModLootModifiers;
import com.koreykeipe.kingslayer.worldgen.ModFeatures;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;

import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(KingSlayer.MOD_ID)
public class KingSlayer
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "kcs_kingslayer";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public KingSlayer(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        com.koreykeipe.kingslayer.item.ModArmorMaterials.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModFeatures.register(modEventBus);

        ModLootModifiers.register(modEventBus);
        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // Airdrop system config (server-side — loot tables and thresholds)
        context.registerConfig(ModConfig.Type.SERVER, AirdropConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

    }
    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

        Scoreboard scoreboard = event.getServer().getScoreboard();

        String team_aqua = "aqua_team";
        PlayerTeam aquaTeam = scoreboard.getPlayerTeam(team_aqua);
        String team_green = "green_team";
        PlayerTeam greenTeam = scoreboard.getPlayerTeam(team_green);
        String team_lime = "lime_team";
        PlayerTeam limeTeam = scoreboard.getPlayerTeam(team_lime);
        String team_yello = "yello_team";
        PlayerTeam yellowTeam = scoreboard.getPlayerTeam(team_yello);
        String team_red = "red_team";
        PlayerTeam redTeam = scoreboard.getPlayerTeam(team_red);
         String team_gray = "gray_team";
        PlayerTeam grayTeam = scoreboard.getPlayerTeam(team_gray);

        if(aquaTeam == null){
            aquaTeam = scoreboard.addPlayerTeam(team_aqua);
            aquaTeam.setColor(ChatFormatting.DARK_AQUA);
        }
        if(greenTeam == null){
            greenTeam = scoreboard.addPlayerTeam(team_green);
            greenTeam.setColor(ChatFormatting.DARK_GREEN);
        }
        if(limeTeam == null){
            limeTeam = scoreboard.addPlayerTeam(team_lime);
            limeTeam.setColor(ChatFormatting.GREEN);
        }
        if(yellowTeam == null){
            yellowTeam = scoreboard.addPlayerTeam(team_yello);
            yellowTeam.setColor(ChatFormatting.YELLOW);
        }
        if(redTeam == null){
            redTeam = scoreboard.addPlayerTeam(team_red);
            redTeam.setColor(ChatFormatting.RED);
        }
        if(grayTeam == null){
            grayTeam = scoreboard.addPlayerTeam(team_gray);
            grayTeam.setColor(ChatFormatting.GRAY);
        }

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            // Register renderer for the falling airdrop entity
            EntityRenderers.register(ModEntityTypes.AIRDROP.get(), AirdropEntityRenderer::new);
            // The King reuses the Warden renderer with a royal texture
            EntityRenderers.register(ModEntityTypes.KING.get(), com.koreykeipe.kingslayer.client.KingRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(
                net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(
                    com.koreykeipe.kingslayer.client.ModModelLayers.KING_CROWN,
                    com.koreykeipe.kingslayer.client.KingCrownModel::createLayer);
        }
    }
}
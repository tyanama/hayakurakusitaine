package com.fastplace;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(FastPlaceMod.MOD_ID)
public class FastPlaceMod {

    public static final String MOD_ID = "fastplace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FastPlaceMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(new FastPlaceEventHandler());
        LOGGER.info("FastPlace MOD が読み込まれました！");
    }
}

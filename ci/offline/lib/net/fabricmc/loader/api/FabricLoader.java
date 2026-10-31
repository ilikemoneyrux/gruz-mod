package net.fabricmc.loader.api;
public interface FabricLoader { static FabricLoader getInstance() { return null; } java.nio.file.Path getConfigDir(); java.nio.file.Path getGameDir(); java.util.Optional<ModContainer> getModContainer(String id); boolean isModLoaded(String id); boolean isDevelopmentEnvironment(); java.util.Collection<ModContainer> getAllMods(); }

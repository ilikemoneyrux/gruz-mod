package net.fabricmc.loader.api;
public interface ModContainer { net.fabricmc.loader.api.metadata.ModMetadata getMetadata(); java.util.Optional<java.nio.file.Path> findPath(String p); java.util.List<java.nio.file.Path> getRootPaths(); }

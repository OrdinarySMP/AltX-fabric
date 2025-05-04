package com.xadale.playerlogger.repositories.implementations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import com.xadale.playerlogger.data.IpAss;
import com.xadale.playerlogger.repositories.IpAssRepository;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class JsonIpAssRepositoryImpl implements IpAssRepository {

  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final Logger LOGGER = LogUtils.getLogger();
  private final File file;
  private List<IpAss> data;

  private JsonIpAssRepositoryImpl(File file) {
    this.file = file;
    if (!file.exists()) {
      this.data = new ArrayList<>();
      return;
    }
    try (FileReader reader = new FileReader(file)) {
      this.data = gson.fromJson(reader, new TypeToken<ArrayList<IpAss>>() {});
    } catch (JsonSyntaxException ex) {
      throw new RuntimeException("Cannot parse data in %s".formatted(file.getAbsolutePath()), ex);
    } catch (Exception e) {
      throw new RuntimeException("Cannot load data (%s)".formatted(file.getAbsolutePath()), e);
    }
    if (this.data == null) {
      // gson failed to parse a valid list
      this.data = new ArrayList<>();
    }
    this.data.forEach(data -> data.setRepository(this));
    System.out.println("Loaded " + this.data.size() + " data entries");
  }

  public static IpAssRepository from(File file) {
    return new JsonIpAssRepositoryImpl(file);
  }

  @Override
  public Optional<IpAss> get(String ip) {
    return this.data.stream().filter(link -> ip.equals(link.getIp())).findFirst();
  }

  @Override
  public void add(IpAss ipAss) {
    this.data.add(ipAss);
    ipAss.setRepository(this);
    this.onUpdated();
  }

  @Override
  public void remove(IpAss ipAss) {
    this.data.remove(ipAss);
    this.onUpdated();
  }

  @Override
  public void update(IpAss ipAss) {
    this.onUpdated();
  }

  private void onUpdated() {
    try (FileWriter writer = new FileWriter(this.file)) {
      gson.toJson(this.data, writer);
    } catch (IOException e) {
      LOGGER.error("Failed to save data to file {}", this.file.getAbsolutePath(), e);
    }
  }

  @Override
  public Stream<IpAss> getAll() {
    return this.data.stream();
  }
}

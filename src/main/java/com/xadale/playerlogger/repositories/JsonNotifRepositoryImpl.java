package com.xadale.playerlogger.repositories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import com.xadale.playerlogger.core.LocalDateTimeAdapter;
import com.xadale.playerlogger.data.Notif;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class JsonNotifRepositoryImpl implements NotifRepository {

  private static final Gson gson =
      new GsonBuilder()
          .setPrettyPrinting()
          .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
          .create();
  private static final Logger LOGGER = LogUtils.getLogger();
  private final File file;
  private List<Notif> data;

  private JsonNotifRepositoryImpl(File file) {
    this.file = file;
    if (!file.exists()) {
      this.data = new ArrayList<>();
      return;
    }
    try (FileReader reader = new FileReader(file)) {
      this.data = gson.fromJson(reader, new TypeToken<ArrayList<Notif>>() {});
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
    System.out.println("Loaded " + this.data.size() + " notification entries");
  }

  public static NotifRepository from(File file) {
    return new JsonNotifRepositoryImpl(file);
  }

  @Override
  public Optional<Notif> get(Integer id) {
    return this.data.stream().filter(notif -> id.equals(notif.getId())).findFirst();
  }

  @Override
  public void add(Notif notif) {
    this.data.add(notif);
    notif.setRepository(this);
    this.onUpdated();
  }

  @Override
  public void remove(Notif notif) {
    this.data.remove(notif);
    this.onUpdated();
  }

  @Override
  public void update(Notif notif) {
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
  public Stream<Notif> getAll() {
    return this.data.stream();
  }
}
